package com.khanbashi.darb.quran

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object QuranPlayer {
    private var player: ExoPlayer? = null

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title
    private val _subtitle = MutableStateFlow<String?>(null)
    val subtitle: StateFlow<String?> = _subtitle
    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing
    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun init(ctx: Context) {
        if (player != null) return
        player = ExoPlayer.Builder(ctx).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playing.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.mediaMetadata?.title?.let { _title.value = it.toString() }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _error.value = "تعذر التشغيل، تحقق من الإنترنت"
                }
            })
        }
    }

    private fun item(url: String, title: String): MediaItem =
        MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .build()

    /** Plays a list of surahs, starting at [start]; next/previous move inside the list. */
    fun playQueue(items: List<Pair<String, String>>, start: Int, subtitle: String) {
        val p = player ?: return
        if (items.isEmpty()) return
        _error.value = null
        p.setMediaItems(items.map { item(it.first, it.second) }, start.coerceIn(0, items.lastIndex), 0L)
        p.prepare()
        p.play()
        _title.value = items[start.coerceIn(0, items.lastIndex)].second
        _subtitle.value = subtitle
        _live.value = false
    }

    fun playLive(url: String, title: String) {
        val p = player ?: return
        _error.value = null
        p.setMediaItem(item(url, title))
        p.prepare()
        p.play()
        _title.value = title
        _subtitle.value = "بث مباشر"
        _live.value = true
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun next() {
        player?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun previous() {
        player?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() }
    }

    fun stop() {
        player?.stop()
        _title.value = null
        _subtitle.value = null
        _playing.value = false
    }
}
