package com.freshdigitable.udonroad2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class UserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
    }

    companion object {
        fun start(context: Context, userId: Long) {
            val intent = Intent(context, UserActivity::class.java)
            intent.putExtra("user_id", userId)
            context.startActivity(intent)
        }
    }
}
