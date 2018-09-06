/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshdigitable.udonroad2.databinding.ViewTweetListItemBinding
import dagger.android.AndroidInjection
import twitter4j.Status
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var homeTimelineRepository: HomeTimelineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listView = findViewById<RecyclerView>(R.id.main_list)
        listView.layoutManager = LinearLayoutManager(this)
        val adapter = Adapter()
        listView.adapter = adapter
        homeTimelineRepository.getTimeline().observe(this, Observer { adapter.addData(it) })
    }
}

class Adapter : RecyclerView.Adapter<ViewHolder>() {
    private val data: MutableList<Status> = mutableListOf()

    fun addData(data: List<Status>) {
        this.data += data
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(ViewTweetListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tweet = data[position]
    }

}

class ViewHolder(val binding: ViewTweetListItemBinding) : RecyclerView.ViewHolder(binding.root)