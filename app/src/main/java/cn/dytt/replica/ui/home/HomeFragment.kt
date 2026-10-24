package cn.dytt.replica.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.dytt.replica.R
import cn.dytt.replica.data.Repository
import cn.dytt.replica.databinding.FragmentHomeBinding
import cn.dytt.replica.ui.BannerAdapter
import cn.dytt.replica.ui.DetailActivity
import cn.dytt.replica.ui.SearchActivity
import cn.dytt.replica.ui.VodAdapter
import cn.dytt.replica.ui.createChip
import cn.dytt.replica.ui.gridColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var binding: FragmentHomeBinding? = null

    private val vodAdapter = VodAdapter { item ->
        startActivity(DetailActivity.intent(requireContext(), item.id, item.name))
    }
    private val bannerAdapter = BannerAdapter { banner ->
        startActivity(DetailActivity.intent(requireContext(), banner.vodId, banner.title))
    }

    private var types: List<Pair<String, String>> = emptyList()
    private var selectedType: String = ""
    private var loading = false
    private var page = 0
    private var totalPages = 0
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = FragmentHomeBinding.inflate(inflater, container, false)
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
        b.bannerList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        b.bannerList.adapter = bannerAdapter

        b.searchEntry.setOnClickListener { openSearch() }
        b.searchBtn.setOnClickListener { openSearch() }

        load()
    }

    private fun openSearch() {
        startActivity(SearchActivity.intent(requireContext(), ""))
    }

    private fun load() {
        val b = binding ?: return
        loading = true
        b.status.visibility = View.VISIBLE
        b.status.text = getString(R.string.loading)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val typeList = Repository.homeTypes()
                    val bannerList = Repository.banners()
                    val firstType = typeList.firstOrNull()?.id.orEmpty()
                    val firstPage = Repository.vodList(page = 1, type = firstType)
                    Triple(typeList, bannerList, firstPage)
                }
            }
            loading = false
            val b2 = binding ?: return@launch
            result.onSuccess { (typeList, bannerList, firstPage) ->
                types = typeList.map { it.id to it.name }
                selectedType = types.firstOrNull()?.first.orEmpty()
                buildTypeTabs()
                bannerAdapter.submit(bannerList)
                vodAdapter.submit(firstPage.items)
                page = 1
                totalPages = firstPage.totalPages
                b2.status.visibility = if (firstPage.items.isEmpty()) View.VISIBLE else View.GONE
                if (firstPage.items.isEmpty()) b2.status.text = getString(R.string.empty)
                fillScreen()
            }.onFailure {
                b2.status.visibility = View.VISIBLE
                b2.status.text = getString(R.string.load_failed)
                b2.status.setOnClickListener { load() }
            }
        }
    }

    private fun buildTypeTabs() {
        val b = binding ?: return
        val container: LinearLayout = b.typeTabs
        container.removeAllViews()
        types.forEach { (id, name) ->
            container.addView(
                createChip(requireContext(), name, id == selectedType) {
                    if (selectedType == id) return@createChip
                    selectedType = id
                    buildTypeTabs()
                    loadPage(reset = true)
                },
            )
        }
    }

    private fun loadPage(reset: Boolean) {
        val b = binding ?: return
        if (!reset && (loading || page >= totalPages)) return
        if (reset) {
            loadJob?.cancel()
            loading = false
            page = 0
            totalPages = 0
            vodAdapter.submit(emptyList())
            b.status.visibility = View.VISIBLE
            b.status.text = getString(R.string.loading)
            b.status.setOnClickListener(null)
        }
        val next = if (reset) 1 else page + 1
        loading = true
        loadJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { Repository.vodList(page = next, type = selectedType) }
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
                fillScreen()
            }.onFailure {
                if (reset || vodAdapter.itemCount == 0) {
                    b2.status.visibility = View.VISIBLE
                    b2.status.text = getString(R.string.load_failed)
                    b2.status.setOnClickListener { loadPage(reset = true) }
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
