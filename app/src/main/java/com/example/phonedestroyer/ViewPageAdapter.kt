package com.example.phonedestroyer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.phonedestroyer.fragments.Leaderboard
import com.example.phonedestroyer.fragments.UserData

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity)  {
    override fun getItemCount(): Int = 3 // We have 3 tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ThrowFragment()
            1 -> Leaderboard()
            2 -> UserData()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}