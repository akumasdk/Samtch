package com.akumasdk.samtchtv

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.akumasdk.samtch.util.NetworkUtil
import com.akumasdk.samtch.util.StreamingPlayerFactory

class TvSamtchApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        StreamingPlayerFactory.prewarm()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { NetworkUtil.relaxedClient }
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
