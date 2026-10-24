package cn.dytt.replica.data

import android.util.Base64
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 源站（pic.dyapis.cn 等）返回的是 base64 文本而不是图片二进制，
 * 所以图片要走自己的取数逻辑：拉下来先解 base64，再交给图片库解码。
 */
data class DyttImage(val url: String)

class DyttImageLoaderFactory : ModelLoaderFactory<DyttImage, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<DyttImage, InputStream> =
        DyttImageLoader(DyttImageLoader.client)

    override fun teardown() = Unit
}

class DyttImageLoader(private val client: OkHttpClient) : ModelLoader<DyttImage, InputStream> {

    override fun buildLoadData(
        model: DyttImage,
        width: Int,
        height: Int,
        options: Options,
    ): LoadData<InputStream> = LoadData(ObjectKey("dytt-b64:" + model.url), Fetcher(client, model.url))

    override fun handles(model: DyttImage): Boolean = model.url.isNotEmpty()

    private class Fetcher(
        private val client: OkHttpClient,
        private val url: String,
    ) : DataFetcher<InputStream> {

        override fun getDataClass(): Class<InputStream> = InputStream::class.java

        override fun getDataSource(): DataSource = DataSource.REMOTE

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            val request = Request.Builder().url(url)
                .header("User-Agent", "okhttp/4.12.0")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        callback.onLoadFailed(IOException("HTTP ${response.code}"))
                        return
                    }
                    val body = response.body?.bytes() ?: ByteArray(0)
                    callback.onDataReady(ByteArrayInputStream(decode(body)))
                }
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }

        override fun cleanup() = Unit

        override fun cancel() = Unit
    }

    companion object {
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        /** 文本内容是 base64 编码的图片时解码成二进制，否则按原图处理。 */
        fun decode(raw: ByteArray): ByteArray {
            if (raw.size < 8) return raw
            val text = String(raw, Charsets.US_ASCII).filterNot { it.isWhitespace() }
            if (text.isEmpty()) return raw
            return try {
                val decoded = Base64.decode(text, Base64.DEFAULT)
                if (isImage(decoded)) decoded else raw
            } catch (e: IllegalArgumentException) {
                raw
            }
        }

        private fun isImage(data: ByteArray): Boolean {
            if (data.size < 4) return false
            val b0 = data[0].toInt() and 0xFF
            val b1 = data[1].toInt() and 0xFF
            val b2 = data[2].toInt() and 0xFF
            val b3 = data[3].toInt() and 0xFF
            return when {
                b0 == 0xFF && b1 == 0xD8 -> true
                b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47 -> true
                b0 == 0x47 && b1 == 0x49 && b2 == 0x46 -> true
                b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 -> true
                else -> false
            }
        }
    }
}
