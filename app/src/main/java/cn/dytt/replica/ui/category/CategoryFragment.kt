package cn.dytt.replica.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.dytt.replica.R
import cn.dytt.replica.data.Filter
import cn.dytt.replica.data.Filters
import cn.dytt.replica.data.Repository
import cn.dytt.replica.databinding.FragmentCategoryBinding
import cn.dytt.replica.ui.DetailActivity
import cn.dytt.replica.ui.VodAdapter
import cn.dytt.replica.ui.createChip
import cn.dytt.replica.ui.createTab
import cn.dytt.replica.ui.gridColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryFragment : Fragment() {

    private var binding: FragmentCategoryBinding? = null

    private val vodAdapter = VodAdapter { item ->
        startActivity(DetailActivity.intent(requireContext(), item.id, item.name))
    }

    private data class Dimension(val label: String, val param: String)

    private val dimensions = listOf(
        Dimension("类型", "type"),
        Dimension("分类", "class"),
        Dimension("地区", "area"),
        Dimension("年份", "year"),
        Dimension("语言", "lang"),
        Dimension("排序", "sort"),
    )

    private var filters: Filters? = null
    private var currentDim = 0
    private val selected = mutableMapOf<String, String>()
    private var loading = false
    private var page = 0
    private var totalPages = 0
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = FragmentCategoryBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = binding ?: return
        b.vodGrid.layoutManager = GridLayoutManager(requireContext(), gridColumns(requireContext()))
        b.vodGrid.adapter = vodAdapter
        b.vodGrid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || loading || page >= totalPages) return
                val lm = rv.layoutManager as? GridLayoutManager ?: return
                if (lm.findLastVisibleItemPosition() >=
                    vodAdapter.itemCount - gridColumns(requireContext()) * 2
                ) {
                    loadPage(reset = false)
                }
            }
        })
        loadFilters()
    }

    private fun loadFilters() {
        val b = binding ?: return
        b.status.visibility = View.VISIBLE
        b.status.text = getString(R.string.loading)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { Repository.filters() } }
            val b2 = binding ?: return@launch
            result.onSuccess {
                filters = it
                buildDimTabs()
                buildOptions()
                loadVodList()
            }.onFailure {
                b2.status.visibility = View.VISIBLE
                b2.status.text = getString(R.string.load_failed)
                b2.status.setOnClickListener { loadFilters() }
            }
        }
    }

    private fun optionsOf(dim: Dimension): List<Filter> {
        val f = filters ?: return emptyList()
        return when (dim.param) {
            "type" -> f.type
            "class" -> f.cls
            "area" -> f.area
            "year" -> f.year
            "lang" -> f.lang
            else -> f.sort
        }
    }

    private fun buildDimTabs() {
        val b = binding ?: return
        b.dimTabs.removeAllViews()
        dimensions.forEachIndexed { index, dim ->
            b.dimTabs.addView(
                createTab(requireContext(), dim.label, index == currentDim) {
                    currentDim = index
                    buildDimTabs()
                    buildOptions()
                },
            )
        }
    }

    private fun buildOptions() {
        val b = binding ?: return
        val dim = dimensions[currentDim]
        val container: LinearLayout = b.optTabs
        container.removeAllViews()
        val current = selected[dim.param]
        container.addView(
            createChip(requireContext(), "全部", current == null) {
                selected.remove(dim.param)
                buildOptions()
                loadVodList()
            },
        )
        optionsOf(dim).forEach { option ->
            container.addView(
                createChip(requireContext(), option.name, option.id == current) {
                    selected[dim.param] = option.id
                    buildOptions()
                    loadVodList()
                },
            )
        }
    }

    private fun loadVodList() = loadPage(reset = true)

    private fun loadPage(reset: Boolean) {
        val b = binding ?: return
        if (!reset && (loading || page >= totalPages)) return
        if (reset) {
            loadJob?.cancel()
            loading = false
            page = 0
            totalPages = 0
            vodAdapter.submit(emptyList())
            b.resultCount.visibility = View.GONE
            b.status.visibility = View.VISIBLE
            b.status.text = getString(R.string.loading)
            b.status.setOnClickListener(null)
        }
        val next = if (reset) 1 else page + 1
        val params = selected.mapValues { it.value }
        loading = true
        loadJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    Repository.vodList(
                        page = next,
                        type = params["type"].orEmpty(),
                        area = params["area"].orEmpty(),
                        year = params["year"].orEmpty(),
                        cls = params["class"].orEmpty(),
                        lang = params["lang"].orEmpty(),
                        sort = params["sort"].orEmpty(),
                    )
                }
            }
            loading = false
            val b2 = binding ?: return@launch
            result.onSuccess { result ->
                page = next
                totalPages = result.totalPages
                if (reset) vodAdapter.submit(result.items) else vodAdapter.append(result.items)
                b2.status.visibility =
                    if (vodAdapter.itemCount == 0) View.VISIBLE else View.GONE
                if (vodAdapter.itemCount == 0) b2.status.text = getString(R.string.empty)
                b2.resultCount.visibility =
                    if (result.total > 0) View.VISIBLE else View.GONE
                b2.resultCount.text = getString(R.string.result_count, result.total)
                fillScreen()
            }.onFailure {
                if (reset || vodAdapter.itemCount == 0) {
                    b2.status.visibility = View.VISIBLE
                    b2.status.text = getString(R.string.load_failed)
                    b2.status.setOnClickListener { loadVodList() }
                }
            }
        }
    }

    /** 电视上一页 10 条可能填不满屏幕，滚不动就触发不了加载，这里补一次填屏检查。 */
    private fun fillScreen() {
        if (loading || page >= totalPages) return
        val b = binding ?: return
        b.vodGrid.post {
            val lm = b.vodGrid.layoutManager as? GridLayoutManager ?: return@post
            if (vodAdapter.itemCount > 0 &&
                lm.findLastVisibleItemPosition() >= vodAdapter.itemCount - 1
            ) {
                loadPage(reset = false)
            }
        }
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        binding = null
        super.onDestroyView()
    }
}
