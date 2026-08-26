package com.example.smsapp.util

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException

object PduUtils {
    private const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L


    fun buildPdu(
        context: Context,
        recipient: String,
        subject: String,
        text: String,
        attachmentUri: Uri,
        attachmentType: String
    ): ByteArray {
        val pdu = PduComposer(context, recipient, subject)
        
        // Add text part
        val textPart = PduPart().apply {
            contentId = "text".toByteArray()
            contentType = "text/plain".toByteArray()
            data = text.toByteArray()
        }
        pdu.addPart(textPart)

        // Add attachment part. Read in chunks and fail instead of silently
        // producing an MMS with a missing attachment.
        val data = ByteArrayOutputStream().use { output ->
            context.contentResolver.openInputStream(attachmentUri)?.use { inputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = inputStream.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_ATTACHMENT_BYTES) {
                        throw IOException("Attachment exceeds ${MAX_ATTACHMENT_BYTES / (1024 * 1024)} MB MMS limit")
                    }
                    output.write(buffer, 0, count)
                }
            } ?: throw IOException("Unable to read attachment: $attachmentUri")
            output.toByteArray()
        }
        val attachmentPart = PduPart().apply {
                contentId = "attachment".toByteArray()
                contentType = attachmentType.toByteArray()
            this.data = data
            fileName = getFileName(context, attachmentUri).ifBlank { "attachment" }.toByteArray()
        }
        pdu.addPart(attachmentPart)

        return pdu.make()
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = "attachment"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}

private class PduComposer(private val context: Context, private val recipient: String, private val subject: String) {
    private val parts = mutableListOf<PduPart>()

    fun addPart(part: PduPart): PduComposer {
        parts.add(part)
        return this
    }

    fun make(): ByteArray {
        val bos = ByteArrayOutputStream()
        
        // Headers
        bos.write(PduHeaders.MESSAGE_TYPE_M_SEND_REQ)
        bos.write(PduHeaders.TRANSACTION_ID) // needs to be unique
        bos.write(PduHeaders.MMS_VERSION)
        
        // To
        bos.write(PduHeaders.TO)
        bos.write(recipient.toByteArray())
        bos.write(0) // Null terminator
        
        // Subject
        bos.write(PduHeaders.SUBJECT)
        bos.write(subject.toByteArray())
        bos.write(0) // Null terminator

        // Date
        bos.write(PduHeaders.DATE)
        bos.write(System.currentTimeMillis() / 1000L)

        // Content-Type
        bos.write(PduHeaders.CONTENT_TYPE)
        val contentType = "application/vnd.wap.multipart.related; type=\"application/smil\"; start=\"<smil.xml>\"; boundary=\"--boundary--\"".toByteArray()
        bos.write(contentType.size)
        bos.write(contentType)

        // Message Body
        val body = ByteArrayOutputStream()
        
        // SMIL part
        val smil = """<smil><head><layout><root-layout/></layout></head><body><par dur="5000ms"><text src="text_part.txt"/><img src="attachment_part"/></par></body></smil>""".toByteArray()
        body.write("--boundary--".toByteArray())
        body.write("\r\n".toByteArray())
        body.write("Content-Type: application/smil\r\n".toByteArray())
        body.write("Content-ID: <smil.xml>\r\n\r\n".toByteArray())
        body.write(smil)
        body.write("\r\n".toByteArray())
        
        // Text part
        val textPart = parts.first { it.contentType.contentEquals("text/plain".toByteArray()) }
        body.write("--boundary--".toByteArray())
        body.write("\r\n".toByteArray())
        body.write("Content-Type: text/plain\r\n".toByteArray())
        body.write("Content-ID: <text_part.txt>\r\n\r\n".toByteArray())
        body.write(textPart.data)
        body.write("\r\n".toByteArray())
        
        // Attachment part
        val attachmentPart = parts.first { !it.contentType.contentEquals("text/plain".toByteArray()) }
        body.write("--boundary--".toByteArray())
        body.write("\r\n".toByteArray())
        body.write("Content-Type: ${String(attachmentPart.contentType)}\r\n".toByteArray())
        body.write("Content-Transfer-Encoding: binary\r\n".toByteArray())
        body.write("Content-ID: <attachment_part>\r\n".toByteArray())
        body.write("Content-Location: ${String(attachmentPart.fileName!!)}\r\n\r\n".toByteArray())
        body.write(attachmentPart.data)
        body.write("\r\n--boundary--\r\n".toByteArray())

        bos.write(body.toByteArray())

        return bos.toByteArray()
    }
}


private class PduPart {
    var headers = mutableMapOf<Int, Any>()
    var data: ByteArray = byteArrayOf()
    var fileName: ByteArray? = null
    var contentType: ByteArray = byteArrayOf()
    var contentId: ByteArray = byteArrayOf()
}

private object PduHeaders {
    const val MESSAGE_TYPE_M_SEND_REQ = 0x8C
    val TRANSACTION_ID: ByteArray get() = "TID_${System.currentTimeMillis()}".toByteArray()
    const val MMS_VERSION = 0x92 // 1.2
    const val TO = 0x97
    const val SUBJECT = 0x96
    const val DATE = 0x85
    const val CONTENT_TYPE = 0x84
}

private fun ByteArrayOutputStream.write(long: Long) {
    val buffer = ByteArray(8)
    for (i in 7 downTo 0) {
        buffer[i] = (long shr (i * 8)).toByte()
    }
    this.write(buffer)
} 