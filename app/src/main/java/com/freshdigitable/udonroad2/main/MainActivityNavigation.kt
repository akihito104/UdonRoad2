package com.freshdigitable.udonroad2.main

import com.freshdigitable.udonroad2.model.QueryType
import com.freshdigitable.udonroad2.model.app.navigation.FragmentContainerState

sealed class MainActivityState : FragmentContainerState {
    data class Init(val type: QueryType) : MainActivityState()
    data class Timeline(val type: QueryType) : MainActivityState()
    data class TweetDetail(val tweetId: Long) : MainActivityState()
}
