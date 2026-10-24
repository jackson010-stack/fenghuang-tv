package cn.dytt.replica.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import cn.dytt.replica.R
import cn.dytt.replica.data.DyttImage
import cn.dytt.replica.data.PlayItem
import cn.dytt.replica.data.PlayLine
import cn.dytt.replica.data.Repository
import cn.dytt.replica.data.VodDetail
import cn.dytt.replica.databinding.ActivityDetailBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private var detail: VodDetail? = null
    private var currentLine = 0
    private var episodes: List<PlayItem> = emptyList()

    private val episodeAdapter = EpisodeAdapter(::onEpisodeClick)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val vodId = intent.getStringExtra(EXTRA_ID).orEmpty()
        binding.episodeGrid.layoutManager = GridLayoutManager(this, episodeColumns())
        binding.episodeGrid.adapter = episodeAdapter
        binding.status.visibility = android.view.View.VISIBLE
        binding.status.text = getString(R.string.loading)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { Repository.detail(vodId) } }
            result.onSuccess { data ->
                detail = data
                currentLine = 0
                binding.name.text = data.item.name
                binding.meta.text = buildMeta(data)
                binding.blurb.text = data.blurb.ifEmpty { data.content }
                Glide.with(binding.pic).load(DyttImage(data.item.pic)).centerCrop().into(binding.pic)
                buildLineTabs()
                bindEpisodes()
            }.onFailure {
                binding.status.visibility = android.view.View.VISIBLE
                binding.status.text = getString(R.string.load_failed)
                binding.status.setOnClickListener { finish() }
            }
        }
    }

    private fun onEpisodeClick(episode: PlayItem) {
        val data = detail ?: return
        val line = data.lines.getOrNull(currentLine) ?: return
        val index = episodes.indexOf(episode)
        if (index < 0) return
        startActivity(
            PlayerActivity.vodIntent(
                this,
                data.item.id,
                line.key,
                index,
                "${data.item.name} ${episode.name}",
            ),
        )
    }

    private fun episodeColumns(): Int {        val widthDp = resources.configuration.screenWidthDp
        return when {
            widthDp >= 1000 -> 10
            widthDp >= 600 -> 8
            else -> 5
        }
    }

    private fun buildMeta(data: VodDetail): String = buildString {
        if (data.item.remarks.isNotEmpty()) append("【${data.item.remarks}】")
        if (data.year.isNotEmpty()) append("${data.year}　")
        if (data.area.isNotEmpty()) append("${data.area}　")
        if (data.director.isNotEmpty()) append("导演：${data.director}　")
        if (data.actor.isNotEmpty()) append("主演：${data.actor}")
        if (data.item.score.isNotEmpty() && data.item.score != "0") {
            append("　评分：${data.item.score}")
        }
    }

    private fun buildLineTabs() {
        val container: LinearLayout = binding.lineTabs
        container.removeAllViews()
        detail?.lines?.forEachIndexed { index, line ->
            container.addView(
                createTab(this, line.name, index == currentLine) {
                    currentLine = index
                    buildLineTabs()
                    bindEpisodes()
                },
            )
        }
    }

    /** 详情接口只带得第一条线路的剧集，其余线路要按 player 单独取，顺带刷新签名。 */
    private fun bindEpisodes() {
        val data = detail ?: return
        val line = data.lines.getOrNull(currentLine) ?: return
        binding.status.visibility = android.view.View.VISIBLE
        binding.status.text = getString(R.string.loading)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { Repository.play(data.item.id, line.key) }
            }
            result.onSuccess { played ->
                episodes = played?.items.orEmpty()
                episodeAdapter.submit(episodes)
                binding.episodeTitle.text = getString(R.string.episode_count, episodes.size)
                binding.status.visibility =
                    if (episodes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                if (episodes.isEmpty()) binding.status.text = getString(R.string.empty)
            }.onFailure {
                binding.status.visibility = android.view.View.VISIBLE
                binding.status.text = getString(R.string.load_failed)
            }
        }
    }

    companion object {
        private const val EXTRA_ID = "vod_id"
        private const val EXTRA_NAME = "vod_name"

        fun intent(context: Context, vodId: String, name: String) =
            Intent(context, DetailActivity::class.java)
                .putExtra(EXTRA_ID, vodId)
                .putExtra(EXTRA_NAME, name)
    }
}
