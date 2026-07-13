package com.ohmusic.app.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ohmusic.app.R

/**
 * MediaSession 封面加载器：真实封面读取失败时统一返回应用默认封面。
 * 这样系统通知、锁屏和厂商播放胶囊不会出现空白封面。
 */
@OptIn(UnstableApi::class)
class FallbackBitmapLoader(context: Context) : BitmapLoader {
    private val delegate = DataSourceBitmapLoader.Builder(context).build()
    private val fallbackBitmap: Bitmap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        checkNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.default_album_art))
    }

    override fun supportsMimeType(mimeType: String): Boolean =
        delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        withFallback(delegate.decodeBitmap(data))

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        withFallback(delegate.loadBitmap(uri))

    private fun withFallback(source: ListenableFuture<Bitmap>): ListenableFuture<Bitmap> =
        Futures.catching(
            source,
            Exception::class.java,
            { fallbackBitmap },
            MoreExecutors.directExecutor()
        )
}
