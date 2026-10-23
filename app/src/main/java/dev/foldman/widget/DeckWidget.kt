package dev.foldman.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import dev.foldman.MainActivity
import dev.foldman.R
import dev.foldman.ui.Finish
import dev.foldman.ui.Skins

/**
 * The home-screen deck. [PlaybackService][dev.foldman.playback.PlaybackService] pushes what's
 * playing here; the keys talk to the player while it's running and open the app when it isn't.
 */
class DeckWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, ids)
    }

    companion object {
        /** Last state pushed by the playback service; lives as long as the process does. */
        @Volatile private var title: String? = null
        @Volatile private var artist: String? = null
        @Volatile private var playing = false
        @Volatile private var running = false
        @Volatile private var art: Bitmap? = null

        fun update(context: Context, title: String?, artist: String?, playing: Boolean, running: Boolean, art: Bitmap?) {
            if (title == this.title && artist == this.artist && playing == this.playing && running == this.running && art === this.art) return
            this.title = title
            this.artist = artist
            this.playing = playing
            this.running = running
            this.art = art
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DeckWidget::class.java))
            if (ids.isNotEmpty()) render(context, ids)
        }

        /** Redraws with the last state, e.g. after the skin changes. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DeckWidget::class.java))
            if (ids.isNotEmpty()) render(context, ids)
        }

        private fun render(context: Context, ids: IntArray) {
            val manager = AppWidgetManager.getInstance(context)
            val skin = Skins.byId(context.getSharedPreferences("foldman", Context.MODE_PRIVATE).getString("skin", null))
            val body = when (skin.finish) {
                Finish.CLEAR -> lerp(skin.body, androidx.compose.ui.graphics.Color(0xFF1C2630), 0.55f)
                Finish.CHROME -> androidx.compose.ui.graphics.Color(0xFFB4BAC2)
                else -> skin.body
            }
            val views = RemoteViews(context.packageName, R.layout.deck_widget).apply {
                setInt(R.id.widget_body, "setColorFilter", body.toArgb())
                setTextViewText(R.id.widget_badge, skin.badge)
                setTextColor(R.id.widget_badge, (if (skin.chromeBadge) androidx.compose.ui.graphics.Color(0xFFECEFF2) else skin.badgeColor).toArgb())
                setTextViewText(R.id.widget_title, title?.takeIf { it.isNotBlank() } ?: "No tape")
                setTextColor(R.id.widget_title, skin.ink.toArgb())
                setTextViewText(R.id.widget_artist, artist?.takeIf { it.isNotBlank() } ?: if (running) "" else "Tap to open the deck")
                setTextColor(R.id.widget_artist, skin.ink.copy(alpha = 0.8f).toArgb())
                val cover = art
                if (cover != null) {
                    setImageViewBitmap(R.id.widget_art, cover)
                    setViewVisibility(R.id.widget_art, View.VISIBLE)
                    setViewVisibility(R.id.widget_cassette, View.GONE)
                } else {
                    setViewVisibility(R.id.widget_art, View.GONE)
                    setViewVisibility(R.id.widget_cassette, View.VISIBLE)
                }
                setImageViewResource(R.id.widget_play, if (playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)
                val open = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                setOnClickPendingIntent(R.id.widget_root, open)
                if (running) {
                    setOnClickPendingIntent(R.id.widget_play, action(context, WidgetActionReceiver.PLAY_PAUSE, 1))
                    setOnClickPendingIntent(R.id.widget_next, action(context, WidgetActionReceiver.NEXT, 2))
                    setOnClickPendingIntent(R.id.widget_prev, action(context, WidgetActionReceiver.PREVIOUS, 3))
                } else {
                    // Nothing loaded in the background: the keys open the deck.
                    setOnClickPendingIntent(R.id.widget_play, open)
                    setOnClickPendingIntent(R.id.widget_next, open)
                    setOnClickPendingIntent(R.id.widget_prev, open)
                }
            }
            manager.updateAppWidget(ids, views)
        }

        private fun action(context: Context, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context, code,
            Intent(context, WidgetActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
