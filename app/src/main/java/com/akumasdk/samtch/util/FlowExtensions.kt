package com.akumasdk.samtch.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Custom flow operator to chunk elements by time with adaptive interval for high traffic
 */
fun <T> Flow<T>.adaptiveChunked(
    minInterval: Long = 100L,
    maxInterval: Long = 500L,
    floodThreshold: Int = 20,
    maxBufferSize: Int = 80
): Flow<List<T>> = flow {
    val buffer = mutableListOf<T>()
    var lastEmitTime = System.currentTimeMillis()
    
    collect { value ->
        buffer.add(value)
        val currentTime = System.currentTimeMillis()
        val currentInterval = if (buffer.size > floodThreshold) maxInterval else minInterval
        
        if (currentTime - lastEmitTime >= currentInterval || buffer.size >= maxBufferSize) {
            emit(buffer.toList())
            buffer.clear()
            lastEmitTime = currentTime
        }
    }
}
