package com.freshdigitable.udonroad2

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.timeline.TimelineFragment

class UserFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    private val user: TweetingUser
) : FragmentStatePagerAdapter(fragmentManager, RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return UserPage.values()[position].creator(user)
    }

    val titles: MutableList<String> = UserPage.values().map { it.name }.toMutableList()

    override fun getPageTitle(position: Int): CharSequence? = titles[position]

    override fun getCount(): Int = UserPage.values().size
}

@Keep
enum class UserPage(
    val creator: (TweetingUser) -> Fragment,
    val titleRes: Int,
    val count: ((User?) -> Int?)? = null
) {
    TWEET(
        creator = { user -> TimelineFragment.newInstance(ListQuery.Timeline(user.id)) },
        titleRes = R.string.user_tab_tweet,
        count = { user -> user?.tweetCount }
    ),
    FAV(
        creator = { user -> TimelineFragment.newInstance(ListQuery.Fav(user.id)) },
        titleRes = R.string.user_tab_fav,
        count = { user -> user?.favoriteCount }
    ),
    MEDIA(
        creator = { user -> TimelineFragment.newInstance(ListQuery.Media(user.screenName)) },
        titleRes = R.string.user_tab_media
    )
}
