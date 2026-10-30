package cn.dytt.replica.data

import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class ApiException(message: String) : Exception(message)

/**
 * 电影天堂后端接口。请求参数既支持明文 query（服务端兼容），也支持原 APK 用的加密 data 参数。
 */
object ApiClient {

    const val BASE_URL = "https://api.dyapis.cn"

    private val trustManager: X509TrustManager = run {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private val socketFactory: SSLSocketFactory = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustManager), null)
    }.socketFactory

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .sslSocketFactory(socketFactory, trustManager)
        .build()

    /** 明文 query 参数请求，返回解密后的 JSON 文本。 */
    fun get(path: String, params: Map<String, String> = emptyMap()): String {
        val url = (BASE_URL + path).toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        val request = Request.Builder().url(url)
            .header("User-Agent", "okhttp/4.12.0")
            .build()
        return execute(request)
    }

    /** 原 APK 的写法：参数打包成 JSON 加密后放进 data 表单域。 */
    fun post(path: String, params: Map<String, String> = emptyMap()): String {
        val body = FormBody.Builder().apply {
            add("data", Crypto.encrypt(JSONObject(params as Map<*, *>).toString()))
        }.build()
        val request = Request.Builder().url(BASE_URL + path)
            .post(body)
            .header("User-Agent", "okhttp/4.12.0")
            .build()
        return execute(request)
    }

    private fun execute(request: Request): String {
        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw ApiException("HTTP ${response.code}")
                val json = JSONObject(text)
                val data = json.optString("data")
                if (data.isEmpty()) return text
                return try {
                    Crypto.decrypt(data)
                } catch (e: Exception) {
                    throw ApiException("解密失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求失败 ${request.url}", e)
            throw e
        }
    }

    private const val TAG = "DyttApi"
}
