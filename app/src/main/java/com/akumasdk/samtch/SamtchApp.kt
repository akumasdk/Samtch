package com.akumasdk.samtch

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.akumasdk.samtch.util.StreamingPlayerFactory

class SamtchApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Prewarm network and player resources early
        StreamingPlayerFactory.prewarm()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
