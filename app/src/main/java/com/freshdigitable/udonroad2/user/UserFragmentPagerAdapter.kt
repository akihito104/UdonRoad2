package com.freshdigitable.udonroad2.user

import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.freshdigitable.udonroad2.R
import com.freshdigitable.udonroad2.model.ListOwner
import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.QueryType.Tweet
import com.freshdigitable.udonroad2.model.QueryType.User
import com.freshdigitable.udonroad2.model.user.TweetUserItem
import com.freshdigitable.udonroad2.model.user.UserEntity
import com.freshdigitable.udonroad2.timeline.fragment.ListItemFragment

class UserFragmentPagerAdapter(
    activity: UserActivity,
) : FragmentStateAdapter(activity) {
    private val items = mutableMapOf<UserPage, ListOwner<*>>()

    override fun createFragment(position: Int): Fragment {
        return ListItemFragment.newInstance(getOwnerByPosition(position))
    }

    private fun getOwnerByPosition(position: Int) =
        requireNotNull(items[UserPage.values()[position]])

    override fun getItemCount(): Int = items.size

    internal fun setItems(items: Map<UserPage, ListOwner<*>>) {
        this.items.clear()
        this.items.putAll(items)
    }

    override fun getItemId(position: Int): Long = getOwnerByPosition(position).id.value.toLong()

    override fun containsItem(itemId: Long): Boolean =
        items.values.find { it.id.value.toLong() == itemId } != null
}

@Keep
enum class UserPage(
    val createQuery: (TweetUserItem) -> QueryType,
    val titleRes: Int,
    val count: ((UserEntity?) -> Int?)? = null,
) {
    TWEET(
        createQuery = { user -> Tweet.Timeline(user.id) },
        titleRes = R.string.user_tab_tweet,
        count = { user -> user?.tweetCount }
    ),
    FOLLOWER(
        createQuery = { user -> User.Follower(user.id) },
        titleRes = R.string.user_tab_follower,
        count = { user -> user?.followerCount }
    ),
    FOLLOWING(
        createQuery = { user -> User.Following(user.id) },
        titleRes = R.string.user_tab_following,
        count = { user -> user?.followingCount }
    ),
    FAV(
        createQuery = { user -> Tweet.Fav(user.id) },
        titleRes = R.string.user_tab_fav,
        count = { user -> user?.favoriteCount }
    ),
    LISTED(
        createQuery = { user -> QueryType.CustomTimelineList.Membership(user.id) },
        titleRes = R.string.user_tab_listed,
        count = { user -> user?.listedCount }
    ),
    MEDIA(
        createQuery = { user -> Tweet.Media(user.screenName) },
        titleRes = R.string.user_tab_media
    )
}
