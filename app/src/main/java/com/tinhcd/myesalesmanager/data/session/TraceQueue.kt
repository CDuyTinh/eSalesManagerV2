package com.tinhcd.myesalesmanager.data.session

import android.content.Context
import com.tinhcd.myesalesmanager.data.remote.service.TracePointDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fixes waiting to be sent, one JSON object per line in the app's own files.
 * A day's few hundred lines need nothing more, and a file outlives the process.
 */
@Singleton
class TraceQueue @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val file get() = File(context.filesDir, "trace-queue.jsonl")
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun append(point: TracePointDto) = lock.withLock {
        withContext(Dispatchers.IO) { file.appendText(json.encodeToString(TracePointDto.serializer(), point) + "\n") }
    }

    /** Up to [max] of the oldest points; a line that no longer parses is skipped. */
    suspend fun peek(max: Int): List<TracePointDto> = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext emptyList()
            file.readLines().asSequence().filter { it.isNotBlank() }.take(max)
                .mapNotNull { runCatching { json.decodeFromString(TracePointDto.serializer(), it) }.getOrNull() }
                .toList()
        }
    }

    /** Drops the oldest [count] lines, the ones a successful send carried. */
    suspend fun drop(count: Int) = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext
            val rest = file.readLines().filter { it.isNotBlank() }.drop(count)
            if (rest.isEmpty()) file.delete() else file.writeText(rest.joinToString("\n", postfix = "\n"))
        }
    }

    suspend fun size(): Int = lock.withLock {
        withContext(Dispatchers.IO) { if (file.exists()) file.readLines().count { it.isNotBlank() } else 0 }
    }
}
