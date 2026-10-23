package dev.foldman.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import android.graphics.Bitmap
import androidx.media3.common.Player
import dev.foldman.MainActivity
import dev.foldman.widget.DeckWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Plays the local folder in the background, with the system media notification and controls. */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var widgetArtKey: Any? = null
    private var widgetArt: Bitmap? = null
    private var widgetArtJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        TapeFx.load(this)
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                // Effects first, so the VU meters read what comes out of the "tape".
                .setAudioProcessors(arrayOf(TapeEffectsProcessor(), TeeAudioProcessor(AudioLevels)))
                .build()
        }
        val player = ExoPlayer.Builder(this, renderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()
        current = player
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = updateWidget(player)
        })
        updateWidget(player)
    }

    /** Keeps the home-screen deck in step with the player. */
    private fun updateWidget(player: Player) {
        val meta = player.mediaMetadata
        val data = meta.artworkData
        val uri = meta.artworkUri
        val key: Any? = data?.contentHashCode() ?: uri
        if (key != widgetArtKey) {
            widgetArtKey = key
            widgetArt = null
            widgetArtJob?.cancel()
            if (key != null) widgetArtJob = scope.launch {
                val bmp = withContext(Dispatchers.IO) { data?.let(Artwork::decode) } ?: uri?.let { Artwork.load(this@PlaybackService, it) }
                if (widgetArtKey == key && bmp != null) {
                    widgetArt = bmp.scaledForWidget()
                    updateWidget(player)
                }
            }
        }
        DeckWidget.update(
            this,
            title = meta.title?.toString(),
            artist = meta.artist?.toString() ?: meta.albumTitle?.toString(),
            playing = player.isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING),
            running = player.mediaItemCount > 0,
            art = widgetArt,
        )
    }

    private fun Bitmap.scaledForWidget(): Bitmap {
        val edge = 300
        if (width <= edge && height <= edge) return this
        val s = edge.toFloat() / maxOf(width, height)
        return Bitmap.createScaledBitmap(this, (width * s).toInt().coerceAtLeast(1), (height * s).toInt().coerceAtLeast(1), true)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        current = null
        scope.cancel()
        DeckWidget.update(this, title = null, artist = null, playing = false, running = false, art = null)
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var current: Player? = null
        /** The running player, for the widget's keys; null when the service isn't up. */
        val player: Player? get() = current
    }
}
