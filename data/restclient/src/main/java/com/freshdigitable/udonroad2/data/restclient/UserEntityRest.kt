package com.freshdigitable.udonroad2.data.restclient

import com.freshdigitable.udonroad2.model.User

class UserEntityRest(
    override val id: Long,
    override val name: String,
    override val screenName: String,
    override val iconUrl: String
) :User
