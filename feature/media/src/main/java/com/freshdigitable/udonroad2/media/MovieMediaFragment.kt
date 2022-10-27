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
import androidx.lifecycle.lifecycleScope
import com.freshdigitable.udonroad2.model.MediaEntity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player.Listener
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.video.VideoSize
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

class MovieMediaFragment : Fragment() {

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
        mediaPlayer = ExoPlayer.Builder(requireContext()).build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return layoutInflater.inflate(R.layout.view_media_movie, container, false)
    }

    private lateinit var mediaPlayer: ExoPlayer
    private var listener: Listener? = null
    private var analyticsListener: AnalyticsListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setOnClickListener { mediaViewModel.toggleSystemUiVisibility.dispatch() }

        val surfaceView: SurfaceView = view.findViewById(R.id.media_video)
        val progressText: TextView = view.findViewById(R.id.media_progressText)
        val progressBar: ProgressBar = view.findViewById(R.id.media_progressBar)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                mediaPlayer.setVideoSurfaceHolder(surfaceHolder)
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                surfaceHolder.removeCallback(this)
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
        })

        val listener = createListener(mediaPlayer, progressText, progressBar)
        val analyticsListener = createAnalyticsListener(surfaceView)
        mediaPlayer.setup(listener, analyticsListener)
        this.listener = listener
        this.analyticsListener = analyticsListener
    }

    private fun ExoPlayer.setup(
        listener: Listener,
        analyticsListener: AnalyticsListener,
    ) {
        addListener(listener)
        addAnalyticsListener(analyticsListener)
        addMediaItem(MediaItem.fromUri(url))
    }

    private fun createAnalyticsListener(surfaceView: SurfaceView) = object : AnalyticsListener {
        override fun onVideoSizeChanged(
            eventTime: AnalyticsListener.EventTime,
            videoSize: VideoSize
        ) {
            val videoWidth = videoSize.width
            val videoHeight = videoSize.height
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
    }

    private fun createListener(
        mediaPlayer: ExoPlayer,
        progressText: TextView,
        progressBar: ProgressBar
    ) = object : Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == STATE_READY) {
                setupProgress(mediaPlayer, progressText, progressBar)
                mediaPlayer.play()
            }
        }
    }

    // the function should be called after MediaPlayer is prepared.
    @ExperimentalCoroutinesApi
    private fun setupProgress(
        mediaPlayer: ExoPlayer,
        progressText: TextView,
        progressBar: ProgressBar,
    ) {
        val currentPosStream: ReceiveChannel<Long> = viewLifecycleOwner.lifecycleScope.produce {
            var oldPosition = -1L
            while (isActive) {
                if (oldPosition != mediaPlayer.currentPosition) {
                    send(mediaPlayer.currentPosition)
                    oldPosition = mediaPlayer.currentPosition
                }
                delay(200)
            }
        }
        progressBar.max = mediaPlayer.duration.toInt()
        val timeElapseFormat = getString(R.string.media_remain_time)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            for (pos in currentPosStream) {
                val remain: Long = (progressBar.max - pos)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remain)
                val seconds =
                    TimeUnit.MILLISECONDS.toSeconds(remain - TimeUnit.MINUTES.toMillis(minutes))
                progressBar.progress = pos.toInt()
                progressText.text = String.format(timeElapseFormat, minutes, seconds)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.prepare()
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
            analyticsListener?.let { removeAnalyticsListener(it) }
            listener?.let { removeListener(it) }
            release()
        }
        analyticsListener = null
        listener = null
    }
}
