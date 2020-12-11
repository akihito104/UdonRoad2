package com.freshdigitable.udonroad2.user

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.QueryType.UserQueryType
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment

class UserFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    private val viewModel: UserViewModel
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return ListItemFragment.newInstance(viewModel.getOwner(UserPage.values()[position]))
    }

    override fun getCount(): Int = UserPage.values().size
}

@Keep
enum class UserPage(
    val createQuery: (TweetUserItem) -> QueryType,
    val titleRes: Int,
    val count: ((User?) -> Int?)? = null
) {
    TWEET(
        createQuery = { user -> TweetQueryType.Timeline(user.id) },
        titleRes = R.string.user_tab_tweet,
        count = { user -> user?.tweetCount }
    ),
    FOLLOWER(
        createQuery = { user -> UserQueryType.Follower(user.id) },
        titleRes = R.string.user_tab_follower,
        count = { user -> user?.followerCount }
    ),
    FOLLOWING(
        createQuery = { user -> UserQueryType.Following(user.id) },
        titleRes = R.string.user_tab_following,
        count = { user -> user?.followingCount }
    ),
    FAV(
        createQuery = { user -> TweetQueryType.Fav(user.id) },
        titleRes = R.string.user_tab_fav,
        count = { user -> user?.favoriteCount }
    ),
    LISTED(
        createQuery = { user -> QueryType.UserListMembership(user.id) },
        titleRes = R.string.user_tab_listed,
        count = { user -> user?.listedCount }
    ),
    MEDIA(
        createQuery = { user -> TweetQueryType.Media(user.screenName) },
        titleRes = R.string.user_tab_media
    )
}
