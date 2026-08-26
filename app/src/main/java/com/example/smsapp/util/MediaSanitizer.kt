package com.example.smsapp.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.smsapp.model.AttachmentType
import java.io.File
import java.io.FileOutputStream

object MediaSanitizer {
    fun stripMetadata(context: Context, source: Uri, type: AttachmentType, mimeType: String): Uri? {
        return runCatching {
            val dir = File(context.cacheDir, "sanitized_media").apply { mkdirs() }
            val extension = if (type == AttachmentType.IMAGE) "jpg" else "mp4"
            val output = File(dir, "sanitized_${System.currentTimeMillis()}.$extension")
            when (type) {
                AttachmentType.IMAGE -> {
                    val bitmap = context.contentResolver.openInputStream(source).use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    } ?: return null
                    FileOutputStream(output).use { stream ->
                        val format = if (mimeType.contains("png", ignoreCase = true)) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                        bitmap.compress(format, 95, stream)
                    }
                    bitmap.recycle()
                }
                AttachmentType.VIDEO -> rewriteVideoWithoutMetadata(context, source, output)
                else -> return source
            }
            FileProvider.getUriForFile(context, "com.example.smsapp.mms.provider", output)
        }.getOrNull()
    }

    private fun rewriteVideoWithoutMetadata(context: Context, source: Uri, output: File) {
        val extractor = MediaExtractor()
        context.contentResolver.openFileDescriptor(source, "r").use { descriptor ->
            requireNotNull(descriptor) { "Unable to read video" }
            extractor.setDataSource(descriptor.fileDescriptor)
            val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    trackMap[index] = muxer.addTrack(format)
                }
            }
            muxer.start()
            val buffer = java.nio.ByteBuffer.allocate(1024 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            for ((sourceTrack, outputTrack) in trackMap) {
                extractor.selectTrack(sourceTrack)
                while (true) {
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = extractor.sampleTime
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(outputTrack, buffer, info)
                    extractor.advance()
                }
                extractor.unselectTrack(sourceTrack)
            }
            muxer.stop()
            muxer.release()
        }
        extractor.release()
    }
}
