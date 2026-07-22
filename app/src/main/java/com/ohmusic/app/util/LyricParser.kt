package com.ohmusic.app.util

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LyricLine(
    val timeMs: Long,
    val text: String
)

class LyricParser {

    companion object {
        private val TIME_TAG_PATTERN_HMS = Regex("\\[(\\d{2}):(\\d{2}):(\\d{2})\\]")
        private val TIME_TAG_PATTERN_MS = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

        suspend fun parseFromUri(context: Context, uri: Uri): List<LyricLine> =
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        parse(reader.readText())
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }

        suspend fun parseFromFilePath(context: Context, filePath: String): List<LyricLine> =
            withContext(Dispatchers.IO) {
                Log.d("LyricParser", "Parsing lyrics for: $filePath")
                
                val uri = Uri.parse(filePath)
                val realPath = getRealFilePath(context, uri)
                Log.d("LyricParser", "Real file path: $realPath")
                
                val embeddedLyrics = parseEmbeddedLyrics(context, filePath)
                if (embeddedLyrics.isNotEmpty()) {
                    Log.d("LyricParser", "Found ${embeddedLyrics.size} embedded lyric lines")
                    return@withContext embeddedLyrics
                }
                
                if (realPath != null) {
                    try {
                        val lrcPath = realPath.substringBeforeLast('.') + ".lrc"
                        java.io.File(lrcPath).bufferedReader().use { reader ->
                            val content = reader.readText()
                            val lines = parse(content)
                            if (lines.isNotEmpty()) {
                                Log.d("LyricParser", "Parsed ${lines.size} lines from .lrc file: $lrcPath")
                                return@withContext lines
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("LyricParser", "Error reading .lrc file: ${e.message}")
                    }
                }
                
                Log.d("LyricParser", "No lyrics found")
                emptyList()
            }

        private suspend fun parseEmbeddedLyrics(context: Context, filePath: String): List<LyricLine> =
            withContext(Dispatchers.IO) {
                val uri = Uri.parse(filePath)
                
                try {
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(context, uri)
                        
                        for (i in 0..20) {
                            val meta = retriever.extractMetadata(i)
                            if (meta != null && (meta.contains("[") || meta.contains("]"))) {
                                val parsed = parse(meta)
                                if (parsed.isNotEmpty()) {
                                    Log.d("LyricParser", "Found ${parsed.size} lyric lines via MediaMetadataRetriever index $i")
                                    return@withContext parsed
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("LyricParser", "Error reading embedded lyrics via MediaMetadataRetriever: ${e.message}")
                }
                
                val lyricsFromMediaStore = getLyricsFromMediaStore(context, filePath)
                if (lyricsFromMediaStore.isNotEmpty()) {
                    return@withContext lyricsFromMediaStore
                }
                
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        Log.d("LyricParser", "File size: ${bytes.size} bytes")
                        if (bytes.size >= 10) {
                            Log.d("LyricParser", "First 10 bytes: ${bytes.take(10).joinToString(", ") { it.toInt().toString() }}")
                        }
                        
                        val lyrics = extractUsltFromId3v2(bytes)
                        if (!lyrics.isNullOrEmpty()) {
                            Log.d("LyricParser", "Found embedded lyrics via ID3v2 parsing")
                            Log.d("LyricParser", "Lyrics content (first 200 chars): ${lyrics.take(200)}")
                            return@withContext parse(lyrics)
                        }
                        
                        val lyricsFromGeob = extractLyricsFromGeob(bytes)
                        if (!lyricsFromGeob.isNullOrEmpty()) {
                            Log.d("LyricParser", "Found embedded lyrics via GEOB parsing")
                            return@withContext parse(lyricsFromGeob)
                        }
                    }
                } catch (e: Exception) {
                    Log.d("LyricParser", "Error reading file content: ${e.message}")
                }
                
                emptyList()
            }
            
            private fun extractLyricsFromGeob(data: ByteArray): String? {
                if (data.size < 10) return null
                
                if (data[0] != 'I'.code.toByte() || 
                    data[1] != 'D'.code.toByte() || 
                    data[2] != '3'.code.toByte()) {
                    return null
                }

                val versionMajor = data[3].toInt() and 0xFF
                val tagSize = readSynchsafeInteger(data, 6)
                var offset = 10

                while (offset < tagSize + 10 && offset + 10 <= data.size) {
                    val frameId = String(data, offset, 4)
                    val frameSize = if (versionMajor >= 4) {
                        readSynchsafeInteger(data, offset + 4)
                    } else {
                        ((data[offset + 4].toInt() and 0xFF) shl 24) or
                        ((data[offset + 5].toInt() and 0xFF) shl 16) or
                        ((data[offset + 6].toInt() and 0xFF) shl 8) or
                        (data[offset + 7].toInt() and 0xFF)
                    }
                    
                    offset += 10

                    if (frameId == "GEOB") {
                        val encoding = data[offset].toInt() and 0xFF
                        var pos = offset + 1
                        
                        while (pos < offset + frameSize && data[pos] != 0.toByte()) pos++
                        pos++
                        
                        while (pos < offset + frameSize && data[pos] != 0.toByte()) pos++
                        pos++
                        
                        while (pos < offset + frameSize && data[pos] != 0.toByte()) pos++
                        pos++
                        
                        val lyricsEnd = offset + frameSize
                        if (pos < lyricsEnd) {
                            val charset = when (encoding) {
                                0 -> Charsets.ISO_8859_1
                                1 -> Charsets.UTF_16BE
                                2 -> Charsets.UTF_16LE
                                3 -> Charsets.UTF_8
                                else -> Charsets.UTF_8
                            }
                            return String(data, pos, lyricsEnd - pos, charset)
                        }
                    }

                    offset += frameSize
                }
                return null
            }
            
            private fun getLyricsFromMediaStore(context: Context, filePath: String): List<LyricLine> {
                try {
                    val uri = Uri.parse(filePath)
                    val projection = arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        "lyrics"
                    )
                    
                    context.contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val lyricsIndex = cursor.getColumnIndex("lyrics")
                            if (lyricsIndex != -1) {
                                val lyrics = cursor.getString(lyricsIndex)
                                if (!lyrics.isNullOrEmpty()) {
                                    Log.d("LyricParser", "Found lyrics via MediaStore query")
                                    Log.d("LyricParser", "Lyrics content (first 200 chars): ${lyrics.take(200)}")
                                    return parse(lyrics)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("LyricParser", "Error querying MediaStore for lyrics: ${e.message}")
                }
                return emptyList()
            }
            
            private fun getRealFilePath(context: Context, uri: Uri): String? {
                try {
                    val projection = arrayOf(MediaStore.Audio.Media.DATA)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                            if (dataIndex != -1) {
                                return cursor.getString(dataIndex)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("LyricParser", "Error getting real file path: ${e.message}")
                }
                return null
            }

        private fun extractUsltFromId3v2(data: ByteArray): String? {
            if (data.size < 10) return null
            
            if (data[0] != 'I'.code.toByte() || 
                data[1] != 'D'.code.toByte() || 
                data[2] != '3'.code.toByte()) {
                return null
            }

            val versionMajor = data[3].toInt() and 0xFF
            val tagSize = readSynchsafeInteger(data, 6)
            var offset = 10

            while (offset < tagSize + 10 && offset + 10 <= data.size) {
                val frameId = String(data, offset, 4)
                val frameSize = if (versionMajor >= 4) {
                    readSynchsafeInteger(data, offset + 4)
                } else {
                    ((data[offset + 4].toInt() and 0xFF) shl 24) or
                    ((data[offset + 5].toInt() and 0xFF) shl 16) or
                    ((data[offset + 6].toInt() and 0xFF) shl 8) or
                    (data[offset + 7].toInt() and 0xFF)
                }
                
                offset += 10

                if (frameId == "USLT" || frameId == "SYLT" || frameId == "GEOB") {
                    val encoding = data[offset].toInt() and 0xFF
                    var lyricsStart = offset + 1
                    
                    if (frameId == "USLT" || frameId == "SYLT") {
                        val languageEnd = findZero(data, offset + 4)
                        val descriptionEnd = findZero(data, languageEnd + 1)
                        lyricsStart = descriptionEnd + 1
                    } else if (frameId == "GEOB") {
                        val mimeTypeEnd = findZero(data, offset + 1)
                        val filenameEnd = findZero(data, mimeTypeEnd + 1)
                        val descriptionEnd = findZero(data, filenameEnd + 1)
                        lyricsStart = descriptionEnd + 1
                    }
                    
                    val lyricsEnd = offset + frameSize
                    
                    if (lyricsStart < lyricsEnd) {
                        val charset = when (encoding) {
                            0 -> Charsets.ISO_8859_1
                            1 -> Charsets.UTF_16BE
                            2 -> Charsets.UTF_16LE
                            3 -> Charsets.UTF_8
                            else -> Charsets.UTF_8
                        }
                        val content = String(data, lyricsStart, lyricsEnd - lyricsStart, charset)
                        
                        if (containsLrcTimeTags(content)) {
                            Log.d("LyricParser", "Found LRC content in frame: $frameId")
                            return content
                        }
                    }
                }

                offset += frameSize
            }

            return null
        }
        
        private fun containsLrcTimeTags(content: String): Boolean {
            return TIME_TAG_PATTERN_HMS.containsMatchIn(content) || TIME_TAG_PATTERN_MS.containsMatchIn(content)
        }

        private fun readSynchsafeInteger(data: ByteArray, offset: Int): Int {
            var result = 0
            for (i in 0..3) {
                result = (result shl 7) or (data[offset + i].toInt() and 0x7F)
            }
            return result
        }

        private fun findZero(data: ByteArray, start: Int): Int {
            for (i in start until data.size) {
                if (data[i] == 0.toByte()) {
                    return i
                }
            }
            return data.size
        }

        fun parse(lrcContent: String): List<LyricLine> {
            val lines = mutableListOf<LyricLine>()
            val linesArray = lrcContent.split("\n")

            for (line in linesArray) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue

                var text = trimmedLine
                var timeMs: Long? = null

                val hmsMatch = TIME_TAG_PATTERN_HMS.find(trimmedLine)
                if (hmsMatch != null) {
                    try {
                        val hours = hmsMatch.groupValues[1].toLong()
                        val minutes = hmsMatch.groupValues[2].toLong()
                        val seconds = hmsMatch.groupValues[3].toLong()
                        timeMs = hours * 3600 * 1000 + minutes * 60 * 1000 + seconds * 1000
                        text = TIME_TAG_PATTERN_HMS.replace(trimmedLine, "").trim()
                    } catch (_: NumberFormatException) {
                    }
                } else {
                    val msMatch = TIME_TAG_PATTERN_MS.find(trimmedLine)
                    if (msMatch != null) {
                        try {
                            val minutes = msMatch.groupValues[1].toLong()
                            val seconds = msMatch.groupValues[2].toLong()
                            val milliseconds = msMatch.groupValues[3].padEnd(3, '0').toLong()
                            timeMs = minutes * 60 * 1000 + seconds * 1000 + milliseconds
                            text = TIME_TAG_PATTERN_MS.replace(trimmedLine, "").trim()
                        } catch (_: NumberFormatException) {
                        }
                    }
                }

                if (timeMs != null && text.isNotEmpty()) {
                    lines.add(LyricLine(timeMs, text))
                }
            }

            return lines.sortedBy { it.timeMs }
        }

        fun findCurrentLineIndex(lines: List<LyricLine>, currentTimeMs: Long): Int {
            if (lines.isEmpty()) return -1

            for (i in lines.indices) {
                val line = lines[i]
                val nextLine = lines.getOrNull(i + 1)

                if (currentTimeMs >= line.timeMs && (nextLine == null || currentTimeMs < nextLine.timeMs)) {
                    return i
                }
            }

            return lines.lastIndex
        }
    }
}