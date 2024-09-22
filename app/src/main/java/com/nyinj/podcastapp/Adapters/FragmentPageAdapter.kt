package com.nyinj.podcastapp.Adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nyinj.podcastapp.Fragments.BrowseFragment
import com.nyinj.podcastapp.Fragments.ExploreFragment
import com.nyinj.podcastapp.Fragments.YouFragment
import com.nyinj.podcastapp.HomeFragment
import com.nyinj.podcastapp.LibraryFragment

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
            1 -> ExploreFragment()
            2 -> BrowseFragment()
            3 -> LibraryFragment()
            4 -> YouFragment()
            else -> throw IllegalArgumentException("Invalid position $position") //safety check
        }
    }
}
