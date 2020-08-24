package com.freshdigitable.udonroad2.user

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.ListOwnerGenerator
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.TweetQueryType
import com.freshdigitable.udonroad2.model.QueryType.UserQueryType
import com.freshdigitable.udonroad2.model.user.TweetingUser
import com.freshdigitable.udonroad2.model.user.User
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment

class UserFragmentPagerAdapter(
    fragmentManager: FragmentManager,
    private val user: TweetingUser,
    private val listOwnerGenerator: ListOwnerGenerator
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return ListItemFragment.newInstance(
            UserPage.values()[position].createOwner(listOwnerGenerator, user)
        )
    }

    val titles: MutableList<String> = UserPage.values().map { it.name }.toMutableList()

    override fun getPageTitle(position: Int): CharSequence? = titles[position]

    override fun getCount(): Int = UserPage.values().size
}

@Keep
enum class UserPage(
    val createOwner: ListOwnerGenerator.(TweetingUser) -> ListOwner<*>,
    val titleRes: Int,
    val count: ((User?) -> Int?)? = null
) {
    TWEET(
        createOwner = { user -> create(TweetQueryType.Timeline(user.id)) },
        titleRes = R.string.user_tab_tweet,
        count = { user -> user?.tweetCount }
    ),
    FOLLOWER(
        createOwner = { user -> create(UserQueryType.Follower(user.id)) },
        titleRes = R.string.user_tab_follower,
        count = { user -> user?.followerCount }
    ),
    FOLLOWING(
        createOwner = { user -> create(UserQueryType.Following(user.id)) },
        titleRes = R.string.user_tab_following,
        count = { user -> user?.followingCount }
    ),
    FAV(
        createOwner = { user -> create(TweetQueryType.Fav(user.id)) },
        titleRes = R.string.user_tab_fav,
        count = { user -> user?.favoriteCount }
    ),
    LISTED(
        createOwner = { user -> create(QueryType.UserListMembership(user.id)) },
        titleRes = R.string.user_tab_listed,
        count = { user -> user?.listedCount }
    ),
    MEDIA(
        createOwner = { user -> create(TweetQueryType.Media(user.screenName)) },
        titleRes = R.string.user_tab_media
    )
}
