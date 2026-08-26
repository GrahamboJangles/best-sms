package com.example.smsapp.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsapp.model.AttachmentType
import com.example.smsapp.model.ContactGroup
import com.example.smsapp.model.Conversation
import com.example.smsapp.model.SmsMessage
import com.example.smsapp.model.SendStatus
import com.example.smsapp.receiver.ScheduledSmsReceiver
import com.example.smsapp.util.SmsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    
    private companion object {
        const val TAG = "SmsViewModel"
        const val PREFS_NAME = "conversation_preferences"
        const val PINNED = "pinned"
        const val ARCHIVED = "archived"
        const val MUTED = "muted"
        const val BLOCKED = "blocked"
        const val DRAFT_RECIPIENT = "draft_recipient"
        const val DRAFT_MESSAGE = "draft_message"
    }

    private val conversationPrefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
    private val draftPrefs = application.getSharedPreferences("message_draft", Application.MODE_PRIVATE)
    
    // Messages for all conversations
    private val _allMessages = mutableStateListOf<SmsMessage>()
    
    // Messages for the current conversation
    private val _messages = mutableStateListOf<SmsMessage>()
    val messages: List<SmsMessage> get() = _messages
    
    // List of all conversations
    private val _allConversations = mutableStateListOf<Conversation>()
    
    // List of visible conversations (filtered by selected group)
    private val _conversations = mutableStateListOf<Conversation>()
    val conversations: List<Conversation> get() = _conversations
    
    // Contact groups
    private val _groups = mutableStateListOf<ContactGroup>()
    val groups: List<ContactGroup> get() = _groups
    
    // Current group selection ("all" is a special value for all conversations)
    val selectedGroupId = mutableStateOf("all")

    // Message search state
    val globalSearchQuery = mutableStateOf("")
    val conversationSearchQuery = mutableStateOf("")
    val showArchived = mutableStateOf(false)
    
    // Dialog state for group creation
    val showGroupDialog = mutableStateOf(false)
    val newGroupName = mutableStateOf("")
    
    // Current conversation contact
    val currentContact = mutableStateOf("")
    val currentContactName = mutableStateOf("")
    
    // New message composition
    val currentRecipient = mutableStateOf("")
    val currentMessage = mutableStateOf("")
    
    val permissionsGranted = mutableStateOf(false)
    
    // App navigation state
    val isInConversationList = mutableStateOf(true)
    
    // Use for diagnostic info
    val diagnosticInfo = mutableStateOf("")
    
    // New state for loading messages
    private val _isLoadingMessages = mutableStateOf(false)
    
    // Attachment handling
    val showAttachmentOptions = mutableStateOf(false)
    val currentAttachmentUri = mutableStateOf<Uri?>(null)
    val currentAttachmentType = mutableStateOf(AttachmentType.NONE)
    val currentAttachmentContentType = mutableStateOf("")
    val isProcessingAttachment = mutableStateOf(false)
    
    // Dialog for viewing attachments
    val showAttachmentDialog = mutableStateOf(false)
    val selectedAttachment = mutableStateOf<SmsMessage?>(null)
    
    init {
        checkPermissions()
    }

    fun checkPermissions() {
        permissionsGranted.value = SmsUtils.hasPermissions(getApplication())
        if (permissionsGranted.value) {
            loadContactGroups()
            loadMessages()
        }
    }

    /** Reload groups from Samsung Contacts/Android Contacts Provider. */
    fun loadContactGroups() {
        viewModelScope.launch {
            val importedGroups = withContext(Dispatchers.IO) {
                SmsUtils.retrieveContactGroups(getApplication())
            }
            _groups.clear()
            _groups.addAll(importedGroups)
            if (selectedGroupId.value != "all" && importedGroups.none { it.id == selectedGroupId.value }) {
                selectedGroupId.value = "all"
            }
            filterConversationsByGroup()
        }
    }
    
    fun loadMessages() {
        viewModelScope.launch {
            _isLoadingMessages.value = true

            withContext(Dispatchers.IO) {
                try {
                    val allMessages = SmsUtils.retrieveAllMessages(getApplication())
                    
                    // Update diagnostic info
                    diagnosticInfo.value = SmsUtils.appDiagnosticInfo
                    
                    // Create conversation map from messages
                    val conversationMap = mutableMapOf<Long, MutableList<SmsMessage>>()
                    
                    for (message in allMessages) {
                        val threadId = message.threadId
                        if (!conversationMap.containsKey(threadId)) {
                            conversationMap[threadId] = mutableListOf()
                        }
                        conversationMap[threadId]?.add(message)
                    }
                    
                    // Process the conversations
                    val conversations = mutableListOf<Conversation>()
                    
                    for ((threadId, messages) in conversationMap) {
                        if (messages.isNotEmpty()) {
                            // Sort messages by date
                            messages.sortByDescending { it.date }
                            
                            // Use the newest message details for the conversation
                            val latestMessage = messages.first()
                            val contactName = if (latestMessage.contactName.isNotEmpty()) {
                                latestMessage.contactName
                            } else {
                                // Try to get contact name from the address
                                SmsUtils.getContactName(getApplication(), latestMessage.address)
                            }
                            
                            // Create conversation object
                            val conversation = Conversation(
                                id = threadId,
                                address = latestMessage.address,
                                contactName = contactName,
                                lastMessagePreview = latestMessage.body,
                                lastMessageTimestamp = latestMessage.date,
                                unreadCount = messages.count { it.read == 0 },
                                hasRcsMessages = messages.any { it.isRcs }
                            ).withPreferences(conversationPrefs)
                            
                            conversations.add(conversation)
                        }
                    }
                    
                    // Sort conversations by timestamp (newest first)
                    conversations.sortByDescending { it.lastMessageTimestamp }
                    
                    withContext(Dispatchers.Main) {
                        _allMessages.clear()
                        _allMessages.addAll(allMessages)
                        _allConversations.clear()
                        _allConversations.addAll(conversations)
                        _conversations.clear()
                        _allConversations.clear()
                        _allConversations.addAll(conversations.map { it.withPreferences(conversationPrefs) })
                        _conversations.clear()
                        _conversations.addAll(_allConversations)
                        updateFilteredConversations()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading messages", e)
                    diagnosticInfo.value = "Error loading messages: ${e.message}"
                }
                
                _isLoadingMessages.value = false
            }
        }
    }
    
    private fun updateFilteredConversations() {
        filterConversationsByGroup()
    }

    fun setGlobalSearchQuery(query: String) {
        globalSearchQuery.value = query
        filterConversationsByGroup()
    }

    fun setConversationSearchQuery(query: String) {
        conversationSearchQuery.value = query
        refreshCurrentConversationMessages()
    }

    fun clearConversationSearch() {
        conversationSearchQuery.value = ""
        refreshCurrentConversationMessages()
    }

    private fun refreshCurrentConversationMessages() {
        val address = currentContact.value
        if (address.isEmpty()) return
        val query = conversationSearchQuery.value.trim()
        val filteredMessages = _allMessages
            .asSequence()
            .filter { it.address == address }
            .filter { query.isEmpty() || it.body.contains(query, ignoreCase = true) }
            .sortedBy { it.date }
            .toList()
        _messages.clear()
        _messages.addAll(filteredMessages)
    }
    
    fun filterConversationsByGroup() {
        _conversations.clear()
        
        if (selectedGroupId.value == "all") {
            // Show all conversations
                                _conversations.addAll(_allConversations.filterNot { it.isBlocked || (!showArchived.value && it.isArchived) })

        } else {
            // Filter by group
            val group = _groups.find { it.id == selectedGroupId.value }
            if (group != null) {
                val filteredConversations = _allConversations.filter { conversation ->
                    group.contacts.any { groupNumber ->
                        SmsUtils.phoneNumbersMatch(getApplication(), groupNumber, conversation.address)
                    }
                }
                _conversations.addAll(filteredConversations.filterNot { it.isBlocked || (!showArchived.value && it.isArchived) })
            }
        }

        val query = globalSearchQuery.value.trim()
        if (query.isNotEmpty()) {
            val matchingAddresses = _allMessages
                .filter { it.body.contains(query, ignoreCase = true) }
                .map { it.address }
                .toSet()
            _conversations.retainAll { it.address in matchingAddresses }
        }
        _conversations.sortWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageTimestamp })
    }

    fun exportBackupText(): String {
        return buildString {
            appendLine("BestSMS backup")
            appendLine("Exported: ${java.util.Date()}")
            appendLine()
            _allMessages.sortedBy { it.date }.forEach { message ->
                appendLine("${java.util.Date(message.date)} | ${message.address} | ${message.body}")
                if (message.hasAttachment) appendLine("Attachment: ${message.attachmentType} ${message.attachmentContentType}")
            }
        }
    }

    fun toggleArchivedView() {
        showArchived.value = !showArchived.value
        filterConversationsByGroup()
    }
    
    private fun Conversation.withPreferences(prefs: android.content.SharedPreferences): Conversation {
        return copy(
            isPinned = prefs.getStringSet(PINNED, emptySet())?.contains(address) == true,
            isArchived = prefs.getStringSet(ARCHIVED, emptySet())?.contains(address) == true,
            isMuted = prefs.getStringSet(MUTED, emptySet())?.contains(address) == true,
            isBlocked = prefs.getStringSet(BLOCKED, emptySet())?.contains(address) == true
        )
    }

    private fun updateConversationPreference(key: String, address: String, enabled: Boolean) {
        val values = conversationPrefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        if (enabled) values.add(address) else values.remove(address)
        conversationPrefs.edit().putStringSet(key, values).apply()
        val index = _allConversations.indexOfFirst { it.address == address }
        if (index >= 0) _allConversations[index] = _allConversations[index].withPreferences(conversationPrefs)
        filterConversationsByGroup()
    }

    fun togglePinned(address: String) {
        val conversation = _allConversations.find { it.address == address } ?: return
        updateConversationPreference(PINNED, address, !conversation.isPinned)
    }

    fun toggleArchived(address: String) {
        val conversation = _allConversations.find { it.address == address } ?: return
        updateConversationPreference(ARCHIVED, address, !conversation.isArchived)
    }

    fun toggleMuted(address: String) {
        val conversation = _allConversations.find { it.address == address } ?: return
        updateConversationPreference(MUTED, address, !conversation.isMuted)
    }

    fun toggleBlocked(address: String) {
        val conversation = _allConversations.find { it.address == address } ?: return
        updateConversationPreference(BLOCKED, address, !conversation.isBlocked)
    }

    fun selectGroup(groupId: String) {
        selectedGroupId.value = groupId
        filterConversationsByGroup()
    }
    
    fun addGroup(name: String) {
        // Contact groups are owned by Samsung/Android Contacts and are read-only here.
        // Keep this callback as a no-op for compatibility with the existing dialog.
        newGroupName.value = ""
        showGroupDialog.value = false
    }
    
    fun deleteGroup(groupId: String) {
        _groups.removeIf { it.id == groupId }
        if (selectedGroupId.value == groupId) {
            selectedGroupId.value = "all"
            filterConversationsByGroup()
        }
    }
    
    fun addContactToGroup(groupId: String, contactAddress: String) {
        // Membership is managed in Samsung Contacts; the SMS app only mirrors it.
    }

    fun removeContactFromGroup(groupId: String, contactAddress: String) {
        // Membership is managed in Samsung Contacts; the SMS app only mirrors it.
    }
    
    fun isContactInGroup(groupId: String, contactAddress: String): Boolean {
        return _groups.find { group ->
            group.id == groupId && group.contacts.any { number ->
                SmsUtils.phoneNumbersMatch(getApplication(), number, contactAddress)
            }
        } != null
    }
    
    fun selectConversation(contactAddress: String) {
        currentContact.value = contactAddress
        conversationSearchQuery.value = ""
        isInConversationList.value = false
        
        // Filter messages for this conversation
        _messages.clear()
        // Sort messages by date (oldest first) so newest messages appear at the bottom
        val filteredMessages = _allMessages.filter { it.address == contactAddress }
                                         .sortedBy { it.date }
        _messages.addAll(filteredMessages)
        
        // Set contact name (use saved contact name or look it up)
        val contactName = _allConversations.find { it.address == contactAddress }?.contactName 
            ?: SmsUtils.getContactName(getApplication(), contactAddress)
        currentContactName.value = contactName
        
        // Pre-fill the recipient field
        currentRecipient.value = contactAddress
    }
    
    fun goToConversationList() {
        isInConversationList.value = true
        currentContact.value = ""
        currentContactName.value = ""
        conversationSearchQuery.value = ""
    }
    
    fun updateRecipient(value: String) {
        currentRecipient.value = value
        saveDraft()
    }

    fun updateMessage(value: String) {
        currentMessage.value = value
        saveDraft()
    }

    private fun saveDraft() {
        draftPrefs.edit()
            .putString(DRAFT_RECIPIENT, currentRecipient.value)
            .putString(DRAFT_MESSAGE, currentMessage.value)
            .apply()
    }

    private fun clearDraft() {
        draftPrefs.edit().remove(DRAFT_RECIPIENT).remove(DRAFT_MESSAGE).apply()
    }

    fun composeNewMessage() {
        isInConversationList.value = false
        currentContact.value = ""
        currentContactName.value = ""
        currentRecipient.value = draftPrefs.getString(DRAFT_RECIPIENT, "").orEmpty()
        currentMessage.value = draftPrefs.getString(DRAFT_MESSAGE, "").orEmpty()
        _messages.clear()
    }
    
    fun retryMessage(message: SmsMessage) {
        currentRecipient.value = message.address
        currentMessage.value = message.body
        if (message.hasAttachment && message.attachmentUri.isNotBlank()) {
            currentAttachmentUri.value = Uri.parse(message.attachmentUri)
            currentAttachmentType.value = message.attachmentType
            currentAttachmentContentType.value = message.attachmentContentType
        }
        sendMessage()
    }

    fun scheduleMessageAfterMinutes(minutes: Int): Boolean {
        val recipient = currentRecipient.value.ifBlank { currentContact.value }
        val body = currentMessage.value
        if (recipient.isBlank() || body.isBlank() || minutes <= 0) return false
        val requestCode = (System.currentTimeMillis() and 0x7fffffff).toInt()
        val intent = Intent(getApplication(), ScheduledSmsReceiver::class.java).apply {
            putExtra(ScheduledSmsReceiver.EXTRA_RECIPIENT, recipient)
            putExtra(ScheduledSmsReceiver.EXTRA_BODY, body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(), requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = getApplication<Application>().getSystemService(AlarmManager::class.java)
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + minutes * 60_000L,
            pendingIntent
        )
        currentMessage.value = ""
        clearDraft()
        return true
    }

    fun sendMessage() {
        if (currentRecipient.value.isNotEmpty() && (currentMessage.value.isNotEmpty() || currentAttachmentUri.value != null)) {
            val recipient = currentRecipient.value
            val messageText = currentMessage.value
            val hasAttachment = currentAttachmentUri.value != null
            
            // Attachments currently use the platform MMS path. RCS detection is
            // informational only; it does not change the transport yet.
            val isRcs = false
            var sendSucceeded = true

            // Send the message
            if (hasAttachment) {
                val attachmentUri = currentAttachmentUri.value.toString()
                sendSucceeded = SmsUtils.sendMediaMessage(
                    getApplication(),
                    recipient,
                    messageText,
                    attachmentUri,
                    currentAttachmentType.value,
                    isRcs,
                    currentAttachmentContentType.value
                )
            } else {
                SmsUtils.sendSms(recipient, messageText)
            }
            
            // Look up contact name
            val contactName = SmsUtils.getContactName(getApplication(), recipient)
            
            // Create sent message object
            val sentMessage = SmsMessage(
                id = "",
                address = recipient,
                body = messageText,
                date = System.currentTimeMillis(),
                type = android.provider.Telephony.Sms.MESSAGE_TYPE_SENT,
                read = 1,
                threadId = 0,
                isRcs = isRcs,
                contactName = contactName,
                hasAttachment = hasAttachment,
                attachmentType = if (hasAttachment) currentAttachmentType.value else AttachmentType.NONE,
                attachmentUri = currentAttachmentUri.value?.toString() ?: "",
                attachmentContentType = currentAttachmentContentType.value,
                sendStatus = if (sendSucceeded) SendStatus.SENDING else SendStatus.FAILED
            )
            
            // Add to current conversation (maintaining chronological order)
            _messages.add(sentMessage)
            
            // Add to all messages
            _allMessages.add(sentMessage)
            
            // Update conversations list
            updateFilteredConversations()
            
            // Select this conversation
            if (currentContact.value.isEmpty()) {
                currentContact.value = recipient
            }
            
            // Clear the message input and attachment
            currentMessage.value = ""
            clearDraft()
            clearAttachment()
        }
    }
    
    fun addAttachment(uri: Uri, type: AttachmentType, contentType: String) {
        currentAttachmentUri.value = uri
        currentAttachmentType.value = type
        currentAttachmentContentType.value = contentType
        showAttachmentOptions.value = false
    }
    
    fun clearAttachment() {
        currentAttachmentUri.value = null
        currentAttachmentType.value = AttachmentType.NONE
        currentAttachmentContentType.value = ""
    }
    
    fun viewAttachment(message: SmsMessage) {
        selectedAttachment.value = message
        showAttachmentDialog.value = true
    }
    
    fun closeAttachmentDialog() {
        showAttachmentDialog.value = false
        selectedAttachment.value = null
    }
} 