package dev.foldman.deck

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A tape you put together yourself from songs in your folders. */
data class Mixtape(
    val id: String,
    val name: String,
    /** Label colour, ARGB. */
    val color: Long,
    val tracks: List<LocalTrack>,
)

/** Mixtapes live in the app's preferences as JSON; each keeps its songs' details so it plays even before a rescan. */
class MixtapeStore(private val prefs: SharedPreferences) {

    fun load(): List<Mixtape> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val t = o.getJSONArray("tracks")
            Mixtape(
                id = o.getString("id"),
                name = o.optString("name", "Mixtape"),
                color = o.optLong("color", DEFAULT_COLOR),
                tracks = List(t.length()) { j ->
                    val x = t.getJSONObject(j)
                    LocalTrack(
                        uri = Uri.parse(x.getString("uri")),
                        title = x.optString("title", ""),
                        artist = x.optString("artist").takeIf { it.isNotBlank() },
                        folder = x.optString("folder", ""),
                        coverUri = x.optString("cover").takeIf { it.isNotBlank() }?.let(Uri::parse),
                    )
                },
            )
        }
    }.getOrDefault(emptyList())

    fun save(all: List<Mixtape>) {
        val arr = JSONArray()
        all.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("name", m.name)
                    .put("color", m.color)
                    .put("tracks", JSONArray().apply {
                        m.tracks.forEach { t ->
                            put(
                                JSONObject()
                                    .put("uri", t.uri.toString())
                                    .put("title", t.title)
                                    .put("artist", t.artist ?: "")
                                    .put("folder", t.folder)
                                    .put("cover", t.coverUri?.toString() ?: ""),
                            )
                        }
                    }),
            )
        }
        prefs.edit { putString(KEY, arr.toString()) }
    }

    companion object {
        private const val KEY = "mixtapes"
        const val DEFAULT_COLOR = 0xFFE8642CL
        val COLORS = listOf(0xFFE8642CL, 0xFFF2B33DL, 0xFF3E9C5AL, 0xFF2F6FB5L, 0xFF8A4FB8L, 0xFFD6406EL, 0xFF2A2522L, 0xFF9AA0A7L)
        fun newId(): String = UUID.randomUUID().toString()
    }
}
