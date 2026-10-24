package cn.dytt.replica.ui.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cn.dytt.replica.R
import cn.dytt.replica.data.Repository
import cn.dytt.replica.databinding.FragmentLiveBinding
import cn.dytt.replica.ui.LiveAdapter
import cn.dytt.replica.ui.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveFragment : Fragment() {

    private var binding: FragmentLiveBinding? = null

    private val liveAdapter = LiveAdapter { item ->
        startActivity(PlayerActivity.directIntent(requireContext(), item.url, item.name))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val b = FragmentLiveBinding.inflate(inflater, container, false)
        binding = b
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = binding ?: return
        b.liveList.layoutManager = LinearLayoutManager(requireContext())
        b.liveList.adapter = liveAdapter
        load()
    }

    private fun load() {
        val b = binding ?: return
        b.status.visibility = View.VISIBLE
        b.status.text = getString(R.string.loading)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { Repository.lives(1) } }
            val b2 = binding ?: return@launch
            result.onSuccess { lives ->
                liveAdapter.submit(lives)
                b2.status.visibility = if (lives.isEmpty()) View.VISIBLE else View.GONE
                if (lives.isEmpty()) b2.status.text = getString(R.string.empty)
            }.onFailure {
                b2.status.visibility = View.VISIBLE
                b2.status.text = getString(R.string.load_failed)
                b2.status.setOnClickListener { load() }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
