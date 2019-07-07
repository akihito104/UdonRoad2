package com.freshdigitable.udonroad2.user

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.ListQuery
import com.freshdigitable.udonroad2.model.TweetingUser
import com.freshdigitable.udonroad2.model.User
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment
import com.freshdigitable.udonroad2.timeline.fragment.MemberListListFragment
import com.freshdigitable.udonroad2.timeline.fragment.TimelineFragment
import com.freshdigitable.udonroad2.timeline.fragment.UserListFragment

class UserFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    private val user: TweetingUser
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

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
        creator = { user -> ListItemFragment.newInstance<TimelineFragment>(ListQuery.Timeline(user.id)) },
        titleRes = R.string.user_tab_tweet,
        count = { user -> user?.tweetCount }
    ),
    FOLLOWER(
        creator = { user -> ListItemFragment.newInstance<UserListFragment>(ListQuery.Follower(user.id)) },
        titleRes = R.string.user_tab_follower,
        count = { user -> user?.followerCount }
    ),
    FOLLOWING(
        creator = { user -> ListItemFragment.newInstance<UserListFragment>(ListQuery.Following(user.id)) },
        titleRes = R.string.user_tab_following,
        count = { user -> user?.followingCount }
    ),
    FAV(
        creator = { user -> ListItemFragment.newInstance<TimelineFragment>(ListQuery.Fav(user.id)) },
        titleRes = R.string.user_tab_fav,
        count = { user -> user?.favoriteCount }
    ),
    LISTED(
        creator = { user ->
            ListItemFragment.newInstance<MemberListListFragment>(
                ListQuery.UserListMembership(user.id)
            )
        },
        titleRes = R.string.user_tab_listed,
        count = { user -> user?.listedCount }
    ),
    MEDIA(
        creator = { user -> ListItemFragment.newInstance<TimelineFragment>(ListQuery.Media(user.screenName)) },
        titleRes = R.string.user_tab_media
    )
}
