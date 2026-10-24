package cn.dytt.replica.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cn.dytt.replica.R
import cn.dytt.replica.ui.category.CategoryFragment
import cn.dytt.replica.ui.home.HomeFragment
import cn.dytt.replica.ui.live.LiveFragment
import cn.dytt.replica.ui.short.ShortFragment
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    private var currentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<NavigationBarView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            if (item.itemId == currentId) return@setOnItemSelectedListener true
            currentId = item.itemId
            show(
                when (item.itemId) {
                    R.id.nav_category -> CategoryFragment()
                    R.id.nav_live -> LiveFragment()
                    R.id.nav_short -> ShortFragment()
                    else -> HomeFragment()
                },
            )
            true
        }
        currentId = R.id.nav_home
        show(HomeFragment())
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content, fragment)
            .commit()
    }
}
