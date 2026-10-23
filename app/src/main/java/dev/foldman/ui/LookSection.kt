package dev.foldman.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.foldman.deck.CassetteType
import dev.foldman.deck.DeckUi
import dev.foldman.deck.DeckViewModel

/** Player skin and cassette type. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LookSection(ui: DeckUi, vm: DeckViewModel) {
    Section("PLAYER") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Skins.all.forEach { skin ->
                SkinTile(skin, selected = skin.id == ui.skinId, onClick = { vm.setSkin(skin.id) })
            }
        }
    }
    Section("CASSETTE") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CassetteType.entries.forEach { t ->
                ModeChip(t.label, selected = ui.cassette == t, onClick = { vm.setCassette(t) })
            }
        }
    }
}

@Composable
private fun SkinTile(skin: Skin, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(92.dp)
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) DeckColors.Accent else Color.White.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(76.dp, 50.dp).skinBody(skin, corner = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                skin.badge,
                style = EngravedCaps.copy(
                    color = if (skin.chromeBadge) Color(0xFFECEFF2) else skin.badgeColor,
                    fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                ),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            skin.name,
            style = ModeText.copy(fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            textAlign = TextAlign.Center, maxLines = 1,
        )
    }
}
