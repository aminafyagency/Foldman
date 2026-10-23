package dev.foldman.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.DeckUi
import dev.foldman.deck.LocalTrack
import dev.foldman.deck.Mixtape
import dev.foldman.deck.MixtapeStore

class MixtapeActions(
    val play: (String) -> Unit,
    val backToFolder: () -> Unit,
    val playFromLibrary: (Int) -> Unit,
    /** Opens the editor for a mixtape, or a new one when null. */
    val edit: (String?) -> Unit,
)

private val MixHand = TextStyle(fontFamily = FontFamily.Cursive, color = DeckColors.Ink, fontWeight = FontWeight.Bold)

/** Mixtapes on the tape shelf: a row of cassettes with handwritten labels, and a blank to record a new one. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MixtapeRow(ui: DeckUi, actions: MixtapeActions, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MIXTAPES", style = EngravedCaps.copy(color = Color(0xFFB59B80), fontSize = 10.sp, letterSpacing = 3.sp), modifier = Modifier.weight(1f))
            if (ui.activeMixtape != null) {
                Text(
                    "FOLDER",
                    style = EngravedCaps.copy(color = DeckColors.Accent, fontSize = 9.sp),
                    modifier = Modifier.clickable(onClick = actions.backToFolder).padding(6.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.mixtapes.forEach { m ->
                MiniCassette(
                    m, active = m.id == ui.activeMixtape?.id,
                    modifier = Modifier.combinedClickable(onClick = { actions.play(m.id) }, onLongClick = { actions.edit(m.id) }),
                )
            }
            BlankCassette(enabled = ui.library.isNotEmpty(), modifier = Modifier.clickable(enabled = ui.library.isNotEmpty()) { actions.edit(null) })
        }
        if (ui.mixtapes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Tap to play · hold to edit", style = EngravedCaps.copy(color = Color(0xFF8A7666), fontSize = 8.sp, letterSpacing = 1.sp))
        }
    }
}

/** A little cassette seen face-on: shell, a paper label in the mix's colour, and two hub holes. */
@Composable
private fun MiniCassette(m: Mixtape, active: Boolean, modifier: Modifier = Modifier) {
    val c = Color(m.color.toInt())
    Column(
        modifier
            .width(112.dp)
            .shadow(if (active) 8.dp else 3.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF45484F), Color(0xFF2B2D32))))
            .border(if (active) 2.dp else 0.dp, if (active) DeckColors.Accent else Color.Transparent, RoundedCornerShape(6.dp))
            .padding(6.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(DeckColors.Paper)
                .drawBehind {
                    drawRect(c, Offset(0f, size.height * 0.62f), Size(size.width, size.height * 0.12f))
                    drawRect(lerp(c, Color.White, 0.45f), Offset(0f, size.height * 0.77f), Size(size.width, size.height * 0.07f))
                }
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(m.name, style = MixHand.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth().height(18.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                repeat(2) { Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF1A1714))) }
            }
            Text("${m.tracks.size} songs", style = EngravedCaps.copy(color = DeckColors.Ink, fontSize = 7.sp, letterSpacing = 1.sp))
        }
    }
}

@Composable
private fun BlankCassette(enabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(112.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.5.dp, Color.White.copy(alpha = if (enabled) 0.35f else 0.12f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (enabled) "+  NEW MIX" else "PICK A FOLDER\nTO MAKE MIXES",
            style = EngravedCaps.copy(color = Color.White.copy(alpha = if (enabled) 0.8f else 0.35f), fontSize = 9.sp, letterSpacing = 1.5.sp),
        )
    }
}

/**
 * Recording a mixtape: name it, pick a label colour, then add songs from the folder. The tracklist
 * plays in the order shown; arrows move songs up and down.
 */
@Composable
fun MixtapeEditor(
    existing: Mixtape?,
    library: List<LocalTrack>,
    onSave: (name: String, color: Long, uris: List<String>) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var color by rememberSaveable(existing?.id) { mutableStateOf(existing?.color ?: MixtapeStore.DEFAULT_COLOR) }
    val picked = remember(existing?.id) { mutableStateListOf<String>().apply { existing?.tracks?.forEach { add(it.uri.toString()) } } }
    var query by rememberSaveable { mutableStateOf("") }
    var adding by rememberSaveable(existing?.id) { mutableStateOf(existing == null) }
    val byUri = remember(library, existing) { (library + existing?.tracks.orEmpty()).associateBy { it.uri.toString() } }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFFF4EFE9), unfocusedTextColor = Color(0xFFF4EFE9),
        focusedBorderColor = DeckColors.Accent, unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
        cursorColor = DeckColors.Accent, focusedLabelColor = DeckColors.Accent, unfocusedLabelColor = Color(0xFFA79C92),
    )

    Column(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1F17), Color(0xFF1C1510))))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (existing == null) "NEW MIXTAPE" else "EDIT MIXTAPE", style = ModeHeader.copy(fontSize = 12.sp), modifier = Modifier.weight(1f))
            Text("CANCEL", style = EngravedCaps.copy(color = Color(0xFFA79C92), fontSize = 10.sp), modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
            Spacer(Modifier.width(4.dp))
            val canSave = picked.isNotEmpty()
            Text(
                "SAVE",
                style = EngravedCaps.copy(color = if (canSave) Color(0xFF1E1916) else Color(0xFF6B5E52), fontSize = 10.sp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (canSave) DeckColors.Accent else Color.White.copy(alpha = 0.08f))
                    .clickable(enabled = canSave) { onSave(name, color, picked.toList()) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40) },
            label = { Text("Title on the label") }, singleLine = true, colors = fieldColors,
            textStyle = MixHand.copy(color = Color(0xFFF4EFE9), fontSize = 20.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("LABEL", style = ModeHeader.copy(fontSize = 9.sp))
            Spacer(Modifier.width(2.dp))
            MixtapeStore.COLORS.forEach { c ->
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(c.toInt()))
                        .border(if (c == color) 3.dp else 1.dp, if (c == color) Color.White else Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { color = c },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ModeChip("Side A · ${picked.size}", selected = !adding, onClick = { adding = false })
            Spacer(Modifier.width(8.dp))
            ModeChip("+ Add songs", selected = adding, onClick = { adding = true })
        }
        Spacer(Modifier.height(10.dp))
        if (adding) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true, colors = fieldColors,
                label = { Text("Search songs, artists, albums") }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            val q = query.trim().lowercase()
            val shown = remember(library, q) {
                if (q.isEmpty()) library else library.filter { t ->
                    t.title.lowercase().contains(q) || t.artist?.lowercase()?.contains(q) == true || t.folder.lowercase().contains(q)
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(shown, key = { _, t -> t.uri.toString() }) { _, t ->
                    val uri = t.uri.toString()
                    val on = uri in picked
                    SongRow(t, trailing = if (on) "✓" else "+", highlight = on) {
                        if (on) picked.remove(uri) else picked.add(uri)
                    }
                }
            }
        } else {
            if (picked.isEmpty()) Text("No songs yet. Tap “Add songs” to start recording.", style = ModeHint)
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(picked.toList(), key = { _, u -> u }) { i, uri ->
                    val t = byUri[uri]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("%02d".format(i + 1), style = ModeHint.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.width(28.dp))
                        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            Text(t?.title ?: uri.substringAfterLast("%2F"), style = ModeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            t?.artist?.let { Text(it, style = ModeHint, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        SmallKey("▲", enabled = i > 0) { picked.add(i - 1, picked.removeAt(i)) }
                        SmallKey("▼", enabled = i < picked.size - 1) { picked.add(i + 1, picked.removeAt(i)) }
                        SmallKey("✕") { picked.removeAt(i) }
                    }
                }
            }
        }
        if (onDelete != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "DELETE THIS MIXTAPE",
                style = EngravedCaps.copy(color = Color(0xFFE0574B), fontSize = 10.sp),
                modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SongRow(t: LocalTrack, trailing: String, highlight: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .drawBehind { if (highlight) drawRect(DeckColors.Accent.copy(alpha = 0.12f)) }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(t.title, style = ModeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(t.artist, t.folder).joinToString(" · "), style = ModeHint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = ModeText.copy(color = if (highlight) DeckColors.Accent else Color(0xFFE9DFD3), fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
private fun SmallKey(symbol: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = ModeText.copy(color = if (enabled) Color(0xFFE9DFD3) else Color(0xFF5A4E44)))
    }
}

/** Mixtapes in the unfolded library panel. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MixtapeListPanel(ui: DeckUi, actions: MixtapeActions) {
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Mixtapes", style = TextStyle(color = Color(0xFFF4EFE9), fontSize = 14.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        if (ui.library.isNotEmpty()) Text(
            "+ New",
            style = TextStyle(color = DeckColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clickable { actions.edit(null) }.padding(4.dp),
        )
    }
    Spacer(Modifier.height(4.dp))
    if (ui.mixtapes.isEmpty()) {
        Text(if (ui.library.isEmpty()) "Pick a folder to make mixtapes" else "Record your own tapes from your songs", style = TextStyle(color = Color(0xFFA79C92), fontSize = 12.sp))
    }
    ui.mixtapes.forEach { m ->
        val active = m.id == ui.activeMixtape?.id
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) Color.White.copy(alpha = 0.09f) else Color.Transparent)
                .combinedClickable(onClick = { actions.play(m.id) }, onLongClick = { actions.edit(m.id) })
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(30.dp, 20.dp).clip(RoundedCornerShape(3.dp)).background(Color(m.color.toInt())), contentAlignment = Alignment.Center) {
                Box(Modifier.size(18.dp, 6.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1A1714)))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, style = TextStyle(color = if (active) DeckColors.Accent else Color(0xFFF4EFE9), fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${m.tracks.size} songs · hold to edit", style = TextStyle(color = Color(0xFFA79C92), fontSize = 11.sp), maxLines = 1)
            }
        }
    }
}
