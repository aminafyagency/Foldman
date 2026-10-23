package dev.foldman.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import dev.foldman.playback.PlaybackService

/** The widget's keys, applied straight to the running player. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val player = PlaybackService.player ?: return
        when (intent.action) {
            PLAY_PAUSE -> if (player.isPlaying) player.pause() else {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0, 0L)
                player.play()
            }
            NEXT -> if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            PREVIOUS -> if (player.currentPosition > 3000 || !player.hasPreviousMediaItem()) player.seekTo(0L) else player.seekToPreviousMediaItem()
        }
    }

    companion object {
        const val PLAY_PAUSE = "dev.foldman.widget.PLAY_PAUSE"
        const val NEXT = "dev.foldman.widget.NEXT"
        const val PREVIOUS = "dev.foldman.widget.PREVIOUS"
    }
}
