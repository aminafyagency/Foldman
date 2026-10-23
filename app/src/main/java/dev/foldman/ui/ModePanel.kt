package dev.foldman.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.DeckUi
import dev.foldman.deck.DeckViewModel

internal val ModeHeader = EngravedCaps.copy(color = Color(0xFFB59B80), fontSize = 10.sp, letterSpacing = 3.sp)
internal val ModeText = TextStyle(color = Color(0xFFE9DFD3), fontSize = 13.sp)
internal val ModeHint = TextStyle(color = Color(0xFF9C8B7B), fontSize = 11.sp, lineHeight = 15.sp)

/**
 * The MODE panel: the deck's less-used switches gathered on one card — sleep timer, auto-reverse,
 * and (in later sections) the sound and the look of the player.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModePanel(ui: DeckUi, vm: DeckViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1F17), Color(0xFF1C1510))))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("MODE", style = ModeHeader.copy(fontSize = 12.sp))
        Spacer(Modifier.height(16.dp))

        Section("SLEEP TIMER") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 15, 30, 45, 60, 90).forEach { m ->
                    ModeChip(if (m == 0) "Off" else "$m min", selected = ui.sleepMinutes == m, onClick = { vm.setSleepTimer(m) })
                }
            }
            if (ui.sleepRemainingMs > 0) {
                Spacer(Modifier.height(6.dp))
                Text("Stops in ${formatTime(ui.sleepRemainingMs)} — local files fade out at the end.", style = ModeHint)
            }
        }

        Section("AUTO REVERSE") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModeChip("Off", selected = !ui.autoReverse, onClick = { vm.setAutoReverse(false) })
                Spacer(Modifier.padding(4.dp))
                ModeChip("⇄  On", selected = ui.autoReverse, onClick = { vm.setAutoReverse(true) })
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "At the end of the tape the deck flips to side ${if (ui.side == 'A') "B" else "A"} and keeps playing, reels turning the other way. Local folders and mixtapes.",
                style = ModeHint,
            )
        }

        ModeExtras(ui, vm)
    }
}

/** Sections added by the sound and skin features. */
@Composable
internal fun ModeExtras(ui: DeckUi, vm: DeckViewModel) {}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    Text(title, style = ModeHeader)
    Spacer(Modifier.height(8.dp))
    content()
    Spacer(Modifier.height(20.dp))
}

@Composable
internal fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = TextStyle(
            color = if (selected) Color(0xFF1E1916) else Color(0xFFE9DFD3),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        ),
        modifier = modifier
            .clip(shape)
            .background(if (selected) DeckColors.Accent else Color.White.copy(alpha = 0.06f))
            .border(1.dp, if (selected) DeckColors.Accent else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** A full-width row with a label and an On/Off pair. */
@Composable
internal fun ModeToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = ModeText)
            if (hint != null) Text(hint, style = ModeHint)
        }
        Spacer(Modifier.padding(4.dp))
        ModeChip(if (on) "On" else "Off", selected = on, onClick = { onChange(!on) })
    }
    Spacer(Modifier.height(10.dp))
}
