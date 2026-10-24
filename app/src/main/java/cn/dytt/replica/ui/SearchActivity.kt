package cn.dytt.replica.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.dytt.replica.R
import cn.dytt.replica.data.Repository
import cn.dytt.replica.databinding.ActivitySearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var loadJob: Job? = null

    private var query = ""
    private var page = 0
    private var totalPages = 0
    private var loading = false

    private val vodAdapter = VodAdapter { item ->
        startActivity(DetailActivity.intent(this, item.id, item.name))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.vodGrid.layoutManager = GridLayoutManager(this, gridColumns(this))
        binding.vodGrid.adapter = vodAdapter
        binding.vodGrid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || page >= totalPages) return
                val lm = rv.layoutManager as? GridLayoutManager ?: return
                if (lm.findLastVisibleItemPosition() >= vodAdapter.itemCount - gridColumns(this@SearchActivity) * 2) {
                    loadPage(reset = false)
                }
            }
        })

        binding.back.setOnClickListener { finish() }
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else {
                false
            }
        }

        val keyword = intent.getStringExtra(EXTRA_KEYWORD).orEmpty()
        binding.searchInput.setText(keyword)
        if (keyword.isNotEmpty()) doSearch()
    }

    private fun doSearch() {
        val keyword = binding.searchInput.text.toString().trim()
        if (keyword.isEmpty()) return
        loadJob?.cancel()
        loading = false
        query = keyword
        page = 0
        totalPages = 0
        vodAdapter.submit(emptyList())
        binding.resultCount.visibility = View.GONE
        binding.status.visibility = View.VISIBLE
        binding.status.text = getString(R.string.loading)
        binding.status.setOnClickListener(null)
        loadPage(reset = true)
    }

    private fun loadPage(reset: Boolean) {
        if (!reset && (loading || page >= totalPages)) return
        if (reset) {
            loadJob?.cancel()
            loading = false
        }
        val next = if (reset) 1 else page + 1
        loading = true
        loadJob = lifecycleScope.launch {
            delay(if (reset) 250 else 0)
            val result = withContext(Dispatchers.IO) {
                runCatching { Repository.search(query, next) }
            }
            loading = false
            result.onSuccess { result ->
                page = next
                totalPages = result.totalPages
                if (reset) vodAdapter.submit(result.items) else vodAdapter.append(result.items)
                binding.status.visibility =
                    if (vodAdapter.itemCount == 0) View.VISIBLE else View.GONE
                if (vodAdapter.itemCount == 0) binding.status.text = getString(R.string.empty)
                binding.resultCount.visibility =
                    if (result.total > 0) View.VISIBLE else View.GONE
                binding.resultCount.text = getString(R.string.result_count, result.total)
                fillScreen()
            }.onFailure {
                if (reset || vodAdapter.itemCount == 0) {
                    binding.status.visibility = View.VISIBLE
                    binding.status.text = getString(R.string.load_failed)
                    binding.status.setOnClickListener { loadPage(reset = true) }
                }
            }
        }
    }

    /** 电视上一页 10 条可能填不满屏幕，滚不动就触发不了加载，这里补一次填屏检查。 */
    private fun fillScreen() {
        if (loading || page >= totalPages) return
        binding.vodGrid.post {
            val lm = binding.vodGrid.layoutManager as? GridLayoutManager ?: return@post
            if (vodAdapter.itemCount > 0 &&
                lm.findLastVisibleItemPosition() >= vodAdapter.itemCount - 1
            ) {
                loadPage(reset = false)
            }
        }
    }

    companion object {
        private const val EXTRA_KEYWORD = "keyword"

        fun intent(context: Context, keyword: String) =
            Intent(context, SearchActivity::class.java).putExtra(EXTRA_KEYWORD, keyword)
    }
}
