package com.example.smsapp.model

data class Conversation(
    val id: Long,
    val address: String,
    val contactName: String = "",  // If we can resolve the name from contacts
    val lastMessagePreview: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val hasRcsMessages: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false
) 