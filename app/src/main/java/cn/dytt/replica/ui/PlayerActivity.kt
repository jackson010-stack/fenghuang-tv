package cn.dytt.replica.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cn.dytt.replica.data.Repository
import cn.dytt.replica.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.title.text = title
        binding.status.visibility = View.VISIBLE
        binding.status.text = getString(cn.dytt.replica.R.string.loading)

        val directUrl = intent.getStringExtra(EXTRA_URL)
        if (!directUrl.isNullOrEmpty()) {
            prepare(directUrl, title)
        } else {
            resolveVodUrl()
        }
    }

    /** type=1 线路的直链带签名与时间戳，播放前重新取一次拿到新的有效期。 */
    private fun resolveVodUrl() {
        val vodId = intent.getStringExtra(EXTRA_VOD_ID).orEmpty()
        val playerKey = intent.getStringExtra(EXTRA_PLAYER).orEmpty()
        val index = intent.getIntExtra(EXTRA_INDEX, 0)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { Repository.play(vodId, playerKey) }
            }
            result.onSuccess { line ->
                val url = line?.items?.getOrNull(index)?.url
                if (url.isNullOrEmpty()) {
                    showError("播放地址获取失败")
                } else {
                    prepare(url, binding.title.text.toString())
                }
            }.onFailure {
                showError("播放地址获取失败：${it.message}")
            }
        }
    }

    private fun prepare(url: String, title: String) {
        val http = OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "okhttp/4.12.0")
                            .build(),
                    )
                }.build(),
        )
        val dataSourceFactory = DefaultDataSource.Factory(this, http)
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        player = exo
        binding.playerView.player = exo
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                showError("播放失败：${error.errorCodeName}")
            }
        })
        exo.setMediaItem(buildMediaItem(url, title))
        exo.prepare()
        exo.playWhenReady = true
        binding.status.visibility = View.GONE
    }

    /** 直播地址是 .php?...m3u8 这种形式，Media3 从路径推断不出 HLS，只能显式指定。 */
    private fun buildMediaItem(url: String, title: String): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
        val path = Uri.parse(url).path.orEmpty().lowercase()
        val known = path.endsWith(".m3u8") || path.endsWith(".mpd") ||
            path.endsWith(".mp4") || path.endsWith(".ts")
        if (!known) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        return builder.build()
    }

    private fun showError(message: String) {
        binding.status.visibility = View.VISIBLE
        binding.status.text = message
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_VOD_ID = "vod_id"
        private const val EXTRA_PLAYER = "player"
        private const val EXTRA_INDEX = "index"

        fun directIntent(context: Context, url: String, title: String) =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)

        fun vodIntent(context: Context, vodId: String, player: String, index: Int, title: String) =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VOD_ID, vodId)
                .putExtra(EXTRA_PLAYER, player)
                .putExtra(EXTRA_INDEX, index)
                .putExtra(EXTRA_TITLE, title)
    }
}
