package com.nyinj.podcastapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
): FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 5
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> BrowseFragment()
            2 -> ExploreFragment()
            3 -> LibraryFragment()
            4 -> YouFragment()
            else -> throw IllegalArgumentException("Invalid position $position") //safety check
        }
    }
}
