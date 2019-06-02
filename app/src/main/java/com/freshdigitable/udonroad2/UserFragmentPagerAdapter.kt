package com.freshdigitable.udonroad2

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.timeline.TimelineFragment

class UserFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    private val userId: Long
) : FragmentStatePagerAdapter(fragmentManager, RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return UserPage.values()[position].creator(userId)
    }

    val titles: MutableList<String> = UserPage.values().map { it.name }.toMutableList()

    override fun getPageTitle(position: Int): CharSequence? = titles[position]

    override fun getCount(): Int = UserPage.values().size
}

@Keep
enum class UserPage(
    val creator: (Long) -> Fragment,
    val titleRes: Int,
    val count: ((User?) -> Int?)?
) {
    TWEET(
        creator = { userId -> TimelineFragment.newInstance(ListQuery.Timeline(userId)) },
        titleRes = R.string.user_tab_tweet,
        count = { user -> user?.tweetCount }
    ),
    FAV(
        creator = { userId -> TimelineFragment.newInstance(ListQuery.Fav(userId)) },
        titleRes = R.string.user_tab_fav,
        count = { user -> user?.favoriteCount }
    )
}
