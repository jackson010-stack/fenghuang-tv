package cn.dytt.replica.ui

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.dytt.replica.R
import cn.dytt.replica.data.Banner
import cn.dytt.replica.data.DyttImage
import cn.dytt.replica.data.LiveItem
import cn.dytt.replica.data.PlayItem
import cn.dytt.replica.data.ShortItem
import cn.dytt.replica.data.VodItem
import com.bumptech.glide.Glide

/** 手机竖屏 3 列，10 寸平板/电视 5~6 列。 */
fun gridColumns(context: Context): Int {
    val widthDp = context.resources.configuration.screenWidthDp
    return when {
        widthDp >= 1000 -> 6
        widthDp >= 600 -> 5
        else -> 3
    }
}

fun isTelevision(context: Context): Boolean =
    context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION

class VodAdapter(private val onClick: (VodItem) -> Unit) :
    RecyclerView.Adapter<VodAdapter.Holder>() {

    private val items = mutableListOf<VodItem>()

    fun submit(list: List<VodItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun append(list: List<VodItem>) {
        if (list.isEmpty()) return
        val start = items.size
        items.addAll(list)
        notifyItemRangeInserted(start, list.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vod, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val pic: ImageView = view.findViewById(R.id.pic)
        private val name: TextView = view.findViewById(R.id.name)
        private val remarks: TextView = view.findViewById(R.id.remarks)

        fun bind(item: VodItem) {
            name.text = item.name
            remarks.text = item.remarks
            Glide.with(pic).load(DyttImage(item.pic)).centerCrop().into(pic)
            itemView.setOnClickListener { onClick(item) }
            itemView.isFocusable = true
        }
    }
}

class BannerAdapter(private val onClick: (Banner) -> Unit) :
    RecyclerView.Adapter<BannerAdapter.Holder>() {

    private val items = mutableListOf<Banner>()

    fun submit(list: List<Banner>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val pic: ImageView = view.findViewById(R.id.pic)
        private val title: TextView = view.findViewById(R.id.title)

        fun bind(item: Banner) {
            title.text = item.title
            Glide.with(pic).load(DyttImage(item.pic)).centerCrop().into(pic)
            itemView.setOnClickListener { onClick(item) }
            itemView.isFocusable = true
        }
    }
}

class EpisodeAdapter(private val onClick: (PlayItem) -> Unit) :
    RecyclerView.Adapter<EpisodeAdapter.Holder>() {

    private val items = mutableListOf<PlayItem>()

    fun submit(list: List<PlayItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.title)

        fun bind(item: PlayItem) {
            title.text = item.name
            itemView.setOnClickListener { onClick(item) }
            itemView.isFocusable = true
        }
    }
}

class LiveAdapter(private val onClick: (LiveItem) -> Unit) :
    RecyclerView.Adapter<LiveAdapter.Holder>() {

    private val items = mutableListOf<LiveItem>()

    fun submit(list: List<LiveItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_live, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val pic: ImageView = view.findViewById(R.id.pic)
        private val name: TextView = view.findViewById(R.id.name)
        private val type: TextView = view.findViewById(R.id.type)

        fun bind(item: LiveItem) {
            name.text = item.name
            type.text = item.type
            Glide.with(pic).load(DyttImage(item.pic)).centerCrop().into(pic)
            itemView.setOnClickListener { onClick(item) }
            itemView.isFocusable = true
        }
    }
}

class ShortAdapter(private val onClick: (ShortItem) -> Unit) :
    RecyclerView.Adapter<ShortAdapter.Holder>() {

    private val items = mutableListOf<ShortItem>()

    fun submit(list: List<ShortItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_short, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val pic: ImageView = view.findViewById(R.id.pic)
        private val title: TextView = view.findViewById(R.id.title)
        private val count: TextView = view.findViewById(R.id.count)

        fun bind(item: ShortItem) {
            title.text = item.title
            count.text = "${item.count}集"
            Glide.with(pic).load(DyttImage(item.pic)).centerCrop().into(pic)
            itemView.setOnClickListener { onClick(item) }
            itemView.isFocusable = true
        }
    }
}
