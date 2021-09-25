/*
 * Copyright (c) 2019. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad2.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.freshdigitable.udonroad2.model.MediaEntity
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class MovieMediaFragment(
    private val coroutineScope: MovieMediaCoroutineScope = MovieMediaCoroutineScope(),
) : Fragment(), CoroutineScope by coroutineScope {

    companion object {
        private const val ARGS_URL = "url"
        fun create(mediaEntity: MediaEntity): MovieMediaFragment {
            val args = Bundle().apply {
                putString(
                    ARGS_URL,
                    mediaEntity.videoValiantItems.first { it.contentType == "video/mp4" }.url
                )
            }
            return MovieMediaFragment().apply {
                arguments = args
            }
        }
    }

    private val mediaViewModel: MediaViewModel by viewModels(
        ownerProducer = { activity as AppCompatActivity }
    )

    private val url: String
        get() = requireNotNull(arguments?.getString(ARGS_URL)) {
            "url is required. use `${this::class.java.simpleName}.create()`."
        }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        lifecycle.addObserver(coroutineScope)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return layoutInflater.inflate(R.layout.view_media_movie, container, false)
    }

    private val mediaPlayer = MediaPlayer()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnClickListener { mediaViewModel.toggleSystemUiVisibility.dispatch() }

        val surfaceView: SurfaceView = view.findViewById(R.id.media_video)
        val progressText: TextView = view.findViewById(R.id.media_progressText)
        val progressBar: ProgressBar = view.findViewById(R.id.media_progressBar)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                mediaPlayer.setDisplay(surfaceHolder)
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                surfaceHolder.removeCallback(this)
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
        })
        mediaPlayer.setup(surfaceView, progressText, progressBar)
    }

    private fun MediaPlayer.setup(
        surfaceView: SurfaceView,
        progressText: TextView,
        progressBar: ProgressBar,
    ) {
        setOnPreparedListener {
            it.setupProgress(progressText, progressBar)
            it.start()
        }
        setOnVideoSizeChangedListener { mp, _, _ ->
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            surfaceView.holder.setFixedSize(videoWidth, videoHeight)
            val parentWidth = (surfaceView.parent as View).width
            val parentHeight = (surfaceView.parent as View).height
            val factor = if (videoWidth * parentHeight > videoHeight * parentWidth) {
                parentWidth.toFloat() / videoWidth
            } else {
                parentHeight.toFloat() / videoHeight
            }
            surfaceView.layoutParams.apply {
                this.width = (factor * videoWidth).toInt()
                this.height = (factor * videoHeight).toInt()
            }
            surfaceView.requestLayout()
        }

        setDataSource(requireContext(), Uri.parse(url))
    }

    // the function should be called after MediaPlayer is prepared.
    @ExperimentalCoroutinesApi
    private fun MediaPlayer.setupProgress(progressText: TextView, progressBar: ProgressBar) {
        val currentPosStream: ReceiveChannel<Int> = produce {
            var oldPosition = -1
            while (isActive) {
                if (oldPosition != currentPosition) {
                    send(currentPosition)
                    oldPosition = currentPosition
                }
                delay(200)
            }
        }
        progressBar.max = duration
        val timeElapseFormat = getString(R.string.media_remain_time)
        launch {
            for (pos in currentPosStream) {
                val remain: Long = (progressBar.max - pos).toLong()
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remain)
                val seconds =
                    TimeUnit.MILLISECONDS.toSeconds(remain - TimeUnit.MINUTES.toMillis(minutes))
                progressBar.progress = pos
                progressText.text = String.format(timeElapseFormat, minutes, seconds)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.prepareAsync()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.setOnClickListener(null)
    }

    override fun onDetach() {
        super.onDetach()
        with(mediaPlayer) {
            if (isPlaying) {
                stop()
            }
            setOnVideoSizeChangedListener(null)
            setOnPreparedListener(null)
            release()
        }
    }
}

class MovieMediaCoroutineScope : CoroutineScope, LifecycleObserver {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreated() {
        if (!this::job.isInitialized || job.isCancelled) {
            job = Job()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        job.cancel()
    }
}
