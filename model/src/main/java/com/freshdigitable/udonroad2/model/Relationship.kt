package com.freshdigitable.udonroad2.model

interface Relationship {
    val userId: Long
    val following: Boolean
    val blocking: Boolean
    val muting: Boolean
    val wantRetweets: Boolean
    val notificationsEnabled: Boolean
}
