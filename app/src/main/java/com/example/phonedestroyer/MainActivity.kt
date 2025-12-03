package com.example.phonedestroyer

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.phonedestroyer.fragments.Leaderboard
import com.example.phonedestroyer.fragments.UserData
import androidx.core.graphics.toColorInt
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    private lateinit var btnRzut: Button
    private lateinit var btnLeaderboard: Button
    private lateinit var btnUserData: Button
    private lateinit var allButtons: List<Button>
    private lateinit var viewPager: ViewPager2

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isUserLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.main_layout)

        // make the app not appear over the status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // initialize Views
        btnRzut = findViewById(R.id.btnRzut)
        btnLeaderboard = findViewById(R.id.btnLeaderboard)
        btnUserData = findViewById(R.id.btnUserData)
        viewPager = findViewById(R.id.viewPager)

        allButtons = listOf(btnRzut, btnLeaderboard, btnUserData)

        // viewpager2 setup
        val pagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // sync swiping with tab button styles
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setActiveTab(allButtons[position])
            }
        })

        btnRzut.setOnClickListener {
            viewPager.currentItem = 0
        }
        btnLeaderboard.setOnClickListener {
            viewPager.currentItem = 1
        }
        btnUserData.setOnClickListener {
            viewPager.currentItem = 2
        }

        // set initial active tab
        setActiveTab(btnRzut)

        swipeRefreshLayout.setOnRefreshListener {
            refreshCurrentFragment()
        }
    }

    fun isUserLoggedIn(): Boolean {
        val sp = getSharedPreferences("user_data", MODE_PRIVATE)

        if (!sp.contains("username") || !sp.contains("uid")) return false

        val username = sp.getString("username", "")
        val uid = sp.getString("uid", "")

        return !username.isNullOrEmpty() && !uid.isNullOrEmpty()
    }

    fun stopRefreshing() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager.fragments.find {
            it.isVisible && it is Leaderboard || it is UserData
        }

        when (currentFragment) {
            is Leaderboard -> {
                currentFragment.loadLeaderboardData()
            }
            is UserData -> {
                currentFragment.loadThrowData()
            }
        }
    }

    private fun setActiveTab(activeButton: Button) {
        // Set all buttons to inactive first
        allButtons.forEach { button ->
            button.setTextColor("#8E8E93".toColorInt())
            button.setTypeface(null, android.graphics.Typeface.NORMAL)
            button.foreground = null
        }

        // Set the clicked button to active
        activeButton.setTextColor(Color.BLACK)
        activeButton.setTypeface(null, android.graphics.Typeface.BOLD)
        activeButton.foreground = createUnderlineDrawable()
    }

    private fun createUnderlineDrawable(): LayerDrawable {
        val underline = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.BLACK)
            setSize(0, 8)
        }

        return LayerDrawable(arrayOf(underline)).apply {
            setLayerInset(0, 0, 0, 0, 0)
            setLayerGravity(0, android.view.Gravity.BOTTOM)
            setLayerHeight(0, 8)
        }
    }
}
