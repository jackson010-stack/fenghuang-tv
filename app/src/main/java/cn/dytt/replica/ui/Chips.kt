package cn.dytt.replica.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.ContextCompat
import cn.dytt.replica.R

/** 横向筛选用的胶囊标签。 */
fun createChip(
    context: Context,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
): TextView {
    val tv = LayoutInflater.from(context).inflate(R.layout.item_chip, null) as TextView
    tv.text = text
    tv.isSelected = selected
    tv.setTextColor(
        ContextCompat.getColor(context, if (selected) R.color.bg else R.color.text_primary),
    )
    tv.isFocusable = true
    tv.isClickable = true
    tv.setOnClickListener { onClick() }
    return tv
}

/** 线路/分类这类下划线式标签。 */
fun createTab(context: Context, text: String, selected: Boolean, onClick: () -> Unit): TextView {
    val tv = LayoutInflater.from(context).inflate(R.layout.item_tab, null) as TextView
    tv.text = text
    tv.isSelected = selected
    tv.setTextColor(
        ContextCompat.getColor(context, if (selected) R.color.gold else R.color.text_secondary),
    )
    tv.isFocusable = true
    tv.isClickable = true
    tv.setOnClickListener { onClick() }
    return tv
}
