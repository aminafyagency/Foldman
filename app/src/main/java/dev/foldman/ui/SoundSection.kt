package dev.foldman.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.DeckUi
import dev.foldman.deck.DeckViewModel
import dev.foldman.playback.TapeFx
import kotlin.math.abs
import kotlin.math.roundToInt

/** Tape character, MEGA BASS and the graphic equaliser. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SoundSection(ui: DeckUi, vm: DeckViewModel) {
    Section("SOUND") {
        ModeToggle("MEGA BASS", ui.fx.megaBass, vm::setMegaBass, hint = "A deep low-end boost, the way portable players did it.")
        ModeToggle("Tape hiss", ui.fx.hiss, vm::setHiss, hint = "A soft bed of tape noise and a gentle treble roll-off.")
        ModeToggle("Wow & flutter", ui.fx.flutter, vm::setFlutter, hint = "The slight pitch waver of a tape motor and capstan.")
        Spacer(Modifier.height(6.dp))
        Text("GRAPHIC EQUALIZER", style = ModeHeader.copy(fontSize = 9.sp))
        Spacer(Modifier.height(8.dp))
        GraphicEq(ui.fx.eq, vm::setEqBand, Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TapeFx.presets.forEach { (name, gains) ->
                val selected = gains.indices.all { abs(gains[it] - ui.fx.eq.getOrElse(it) { 0f }) < 0.3f }
                ModeChip(name, selected = selected, onClick = { vm.setEqPreset(name) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Sound effects shape your local files and mixtapes. Streaming apps keep their own sound.", style = ModeHint)
    }
}

/** A row of slide faders on a dark faceplate, with a 0 dB line across them. */
@Composable
private fun GraphicEq(gains: List<Float>, onChange: (Int, Float) -> Unit, modifier: Modifier) {
    Row(
        modifier
            .recess(12.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TapeFx.BANDS_HZ.forEachIndexed { i, hz ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val g = gains.getOrElse(i) { 0f }
                Text(
                    (if (g > 0.05f) "+" else "") + g.roundToInt().toString(),
                    style = Label.copy(color = if (abs(g) > 0.05f) DeckColors.Accent else Label.color),
                )
                Fader(g, { onChange(i, it) }, Modifier.weight(1f).fillMaxWidth(), label = bandLabel(hz))
                Text(bandLabel(hz), style = Label)
            }
        }
    }
}

private val Label = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFFB8BBC1))

private fun bandLabel(hz: Float) = if (hz >= 1000f) "${(hz / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it) }}k" else hz.toInt().toString()

@Composable
private fun Fader(gainDb: Float, onChange: (Float) -> Unit, modifier: Modifier, label: String) {
    val range = TapeFx.EQ_RANGE_DB
    val current by rememberUpdatedState(gainDb)
    val set by rememberUpdatedState(onChange)
    fun snap(v: Float) = (v * 2f).roundToInt() / 2f
    Canvas(
        modifier
            .padding(vertical = 4.dp)
            .semantics {
                contentDescription = "$label Hz"
                progressBarRangeInfo = ProgressBarRangeInfo(current, -range..range)
                setProgress { set(it.coerceIn(-range, range)); true }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, drag ->
                    change.consume()
                    set(snap((current - drag / (size.height * 0.85f) * range * 2).coerceIn(-range, range)))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { set(0f) })
            },
    ) {
        val cx = size.width / 2f
        val top = 10f
        val bottom = size.height - 10f
        val slotW = 5.dp.toPx()
        // Scale ticks either side of the slot.
        for (i in -4..4) {
            val y = top + (bottom - top) * (4 - i) / 8f
            val w = if (i == 0) 9.dp.toPx() else 5.dp.toPx()
            drawLine(Color.White.copy(alpha = if (i == 0) 0.55f else 0.2f), Offset(cx - slotW - w, y), Offset(cx - slotW - 2f, y), 1.2f)
            drawLine(Color.White.copy(alpha = if (i == 0) 0.55f else 0.2f), Offset(cx + slotW + 2f, y), Offset(cx + slotW + w, y), 1.2f)
        }
        drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(cx - slotW / 2, top + 1.5f), Size(slotW, bottom - top), CornerRadius(3f))
        drawRoundRect(Color(0xFF050506), Offset(cx - slotW / 2, top), Size(slotW, bottom - top), CornerRadius(3f))
        val frac = (current + range) / (2 * range)
        val y = bottom - (bottom - top) * frac
        val capW = (size.width * 0.62f).coerceAtMost(30.dp.toPx())
        val capH = 16.dp.toPx()
        drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(cx - capW / 2, y - capH / 2 + 3f), Size(capW, capH), CornerRadius(3f))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFF2F3F5), Color(0xFFA6A9AE), Color(0xFFDCDEE1)), y - capH / 2, y + capH / 2),
            Offset(cx - capW / 2, y - capH / 2), Size(capW, capH), CornerRadius(3f),
        )
        drawLine(DeckColors.Accent, Offset(cx - capW / 2 + 3f, y), Offset(cx + capW / 2 - 3f, y), 2f)
        for (k in listOf(-1, 1)) {
            val ry = y + k * capH * 0.28f
            drawLine(Color.Black.copy(alpha = 0.3f), Offset(cx - capW / 2 + 3f, ry), Offset(cx + capW / 2 - 3f, ry), 1f)
        }
    }
}
