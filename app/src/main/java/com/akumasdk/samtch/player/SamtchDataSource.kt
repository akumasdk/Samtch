package com.akumasdk.samtch.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.ByteArrayInputStream
import java.io.InputStream

import com.akumasdk.samtch.service.TwitchGqlService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request

@UnstableApi
class SamtchDataSource(
    private val httpDataSource: DataSource
) : BaseDataSource(true) {

    private var currentInputStream: InputStream? = null
    private var bytesRemaining: Long = 0
    private var currentUri: Uri? = null
    private val httpClient = OkHttpClient()

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        currentUri = dataSpec.uri
        
        val url = dataSpec.uri.toString()
        
        // 1. Check if it's a Twitch HLS manifest (Master or Media)
        if (url.contains("usher.ttvnw.net") || url.contains(".m3u8")) {
            val content = fetchAndProcessManifest(url)
            if (content != null) {
                return openFromContent(content, dataSpec)
            }
        }

        return httpDataSource.open(dataSpec).also {
            transferStarted(dataSpec)
        }
    }

    private fun fetchAndProcessManifest(url: String): String? {
        val rawContent = fetchDirectly(url) ?: return null
        
        // Only process master playlists for ad-blocking logic
        if (!url.contains("usher.ttvnw.net")) {
            // It's a variant playlist, we only strip if ads are present
            return if (AdBlockManifestProcessor.containsAds(rawContent)) {
                AdBlockManifestProcessor.stripAds(rawContent)
            } else rawContent
        }

        val channelName = url.substringAfter("hls/").substringBefore(".m3u8")
        
        // Try different player types to find an ad-free stream
        val playerTypes = listOf(
            TwitchGqlService.PlayerType.SITE,
            TwitchGqlService.PlayerType.EMBED,
            TwitchGqlService.PlayerType.POPOUT
        )

        for (type in playerTypes) {
            val content = fetchManifestForType(channelName, type)
            if (content != null) {
                if (!AdBlockManifestProcessor.containsAds(content)) {
                    android.util.Log.d("SamtchDataSource", "Found ad-free manifest for $channelName using $type")
                    return content
                }
                android.util.Log.d("SamtchDataSource", "Manifest for $type contains ads, trying next...")
            }
        }

        // If all contain ads, try to strip the last one we got
        val fallback = fetchManifestForType(channelName, TwitchGqlService.PlayerType.EMBED)
        return fallback?.let { AdBlockManifestProcessor.stripAds(it) }
    }

    private fun fetchManifestForType(channelName: String, type: TwitchGqlService.PlayerType): String? {
        return runBlocking {
            val tokenPair = TwitchGqlService.getPlaybackAccessToken(channelName, type)
            if (tokenPair != null) {
                val hlsUrl = TwitchGqlService.buildHlsUrl(channelName, tokenPair.first, tokenPair.second, type)
                fetchDirectly(hlsUrl)
            } else null
        }
    }

    private fun fetchDirectly(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().body.string()
        } catch (e: Exception) {
            null
        }
    }

    private fun openFromContent(content: String, dataSpec: DataSpec): Long {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val inputStream = ByteArrayInputStream(bytes)
        
        if (dataSpec.position > bytes.size) {
            throw Exception("Position out of bounds")
        }
        inputStream.skip(dataSpec.position)
        
        currentInputStream = inputStream
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            bytes.size - dataSpec.position
        }
        
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        
        val inputStream = currentInputStream
        if (inputStream != null) {
            if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
            
            val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
                length
            } else {
                length.toLong().coerceAtMost(bytesRemaining).toInt()
            }
            
            val bytesRead = inputStream.read(buffer, offset, bytesToRead)
            if (bytesRead == -1) return C.RESULT_END_OF_INPUT
            
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                bytesRemaining -= bytesRead
            }
            bytesTransferred(bytesRead)
            return bytesRead
        } else {
            return httpDataSource.read(buffer, offset, length).also {
                if (it != C.RESULT_END_OF_INPUT) {
                    bytesTransferred(it)
                }
            }
        }
    }

    override fun getUri(): Uri? {
        return currentUri ?: httpDataSource.uri
    }

    override fun close() {
        currentInputStream?.close()
        currentInputStream = null
        currentUri = null
        httpDataSource.close()
        transferEnded()
    }
}

@UnstableApi
class SamtchDataSourceFactory(
    private val httpDataSourceFactory: DataSource.Factory
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return SamtchDataSource(httpDataSourceFactory.createDataSource())
    }
}
