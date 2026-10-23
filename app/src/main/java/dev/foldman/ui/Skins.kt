package dev.foldman.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.foldman.deck.CassetteType
import kotlin.math.min
import kotlin.random.Random

/** What a player body is made of; each paints differently. */
enum class Finish { ENAMEL, BRUSHED, RUBBER, CHROME, CLEAR, GLOSS, MATTE }

/**
 * A player skin: an original design in the spirit of an era of portable cassette players, with its
 * own invented badge. (No real manufacturer's names, logos or trade dress.)
 */
data class Skin(
    val id: String,
    val name: String,
    val badge: String,
    val model: String,
    val tagline: String,
    val finish: Finish,
    /** Main body colour (tint for clear plastic). */
    val body: Color,
    /** Second colour: the lower band, gasket, trim or pinstripe depending on the finish. */
    val trim: Color,
    /** Printing on the body. */
    val ink: Color,
    /** Chrome lettering for the badge, or solid [badgeColor]. */
    val chromeBadge: Boolean,
    val badgeColor: Color = ink,
    val cassette: CassetteType,
)

object Skins {
    val Classic = Skin(
        "classic", "Classic Blue", "FOLDMAN", "FM-F8 · STEREO CASSETTE PLAYER", "STEREO",
        Finish.ENAMEL, Color(0xFF2F6FB5), Color(0xFF2F6FB5), Color(0xFFDCE6F2), chromeBadge = true, cassette = CassetteType.SOURCE,
    )
    val Original = Skin(
        "original", "Original '79", "KESTREL", "KS-1 · STEREO CASSETTE PLAYER", "STEREO",
        Finish.BRUSHED, Color(0xFFC9CCD1), Color(0xFF27508F), Color(0xFF263449), chromeBadge = false,
        badgeColor = Color(0xFF1E3E70), cassette = CassetteType.NORMAL,
    )
    val Sport = Skin(
        "sport", "Sport", "TERRAIN", "TR-5 · WATER RESISTANT", "SPORTS",
        Finish.RUBBER, Color(0xFFF3BE14), Color(0xFF2B2C2F), Color(0xFF221F1B), chromeBadge = false,
        badgeColor = Color(0xFF1B1A18), cassette = CassetteType.NORMAL,
    )
    val Chrome = Skin(
        "chrome", "Chrome '85", "CHROMAX", "CX-85 · AUTO REVERSE · DOLBY-FREE", "STEREO",
        Finish.CHROME, Color(0xFFC9CDD3), Color(0xFF111214), Color(0xFF16181B), chromeBadge = false,
        badgeColor = Color(0xFF0E0F11), cassette = CassetteType.CHROME,
    )
    val Crystal = Skin(
        "crystal", "Crystal", "PRISM", "PX-0 · SEE-THROUGH SERIES", "STEREO",
        Finish.CLEAR, Color(0xFF9CC9EA), Color(0xFFDDE8F2), Color(0xFF0F2B44), chromeBadge = false,
        badgeColor = Color(0xFF0D2740), cassette = CassetteType.CLEAR,
    )
    val Redline = Skin(
        "redline", "Boombox Red", "REDLINE", "RL-7 · SUPER BASS", "STEREO",
        Finish.GLOSS, Color(0xFFC21F1C), Color(0xFFE3E6EA), Color(0xFFFBE9E7), chromeBadge = true, cassette = CassetteType.METAL,
    )
    val Midnight = Skin(
        "midnight", "Midnight", "NOCTURNE", "NX-9 · METAL TAPE READY", "STEREO",
        Finish.MATTE, Color(0xFF1D1E21), Color(0xFFF0722B), Color(0xFFC6C9CE), chromeBadge = true, cassette = CassetteType.METAL,
    )

    val all = listOf(Classic, Original, Sport, Chrome, Crystal, Redline, Midnight)

    fun byId(id: String?): Skin = all.firstOrNull { it.id == id } ?: Classic
}

val LocalSkin = staticCompositionLocalOf { Skins.Classic }

/** Paints a player body in the skin's finish. Arguments as for [paintedBody]. */
fun Modifier.skinBody(skin: Skin, capHeight: Dp = 0.dp, corner: Dp = 34.dp, framed: Boolean = true): Modifier = when (skin.finish) {
    Finish.ENAMEL -> paintedBody(skin.body, capHeight, corner, framed)
    else -> drawWithCache {
        val r = CornerRadius(corner.toPx())
        val cap = capHeight.toPx()
        val outline = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), r)) }
        val painter = BodyPainter(skin, size, r, cap)
        onDrawBehind {
            if (framed) drawRoundRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, 8f), cornerRadius = r)
            clipPath(outline) {
                with(painter) { paint() }
            }
            if (framed) drawRoundRect(Color.White.copy(alpha = 0.3f), cornerRadius = r, style = Stroke(2f))
        }
    }
}

/** Everything the finishes need, worked out once per size. */
private class BodyPainter(val skin: Skin, val size: Size, val r: CornerRadius, val cap: Float) {
    private val rnd = Random(skin.id.hashCode())
    private val w = size.width
    private val h = size.height

    /** Fine speckle, for powder coat, rubber and soft-touch paint. */
    private val grain: Path = Path().apply {
        repeat((w * h / 200f).toInt().coerceAtMost(36000)) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            addRect(Rect(x, y, x + 1.1f, y + 1.1f))
        }
    }
    private val grainDark: Path = Path().apply {
        repeat((w * h / 320f).toInt().coerceAtMost(24000)) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            addRect(Rect(x, y, x + 1.1f, y + 1.1f))
        }
    }

    /** Horizontal brushing: thousands of hairlines in four tones. */
    private val hairlines: List<Path> = List(4) { Path() }.also { paths ->
        var y = 0f
        while (y < h) {
            val x0 = rnd.nextFloat() * w * 0.25f
            val x1 = w - rnd.nextFloat() * w * 0.25f
            val p = paths[rnd.nextInt(4)]
            p.moveTo(x0, y); p.lineTo(x1, y)
            y += 0.5f + rnd.nextFloat() * 1.3f
        }
    }

    fun DrawScope.paint() {
        when (skin.finish) {
            Finish.BRUSHED -> brushed()
            Finish.RUBBER -> rubber()
            Finish.CHROME -> chrome()
            Finish.CLEAR -> clear()
            Finish.GLOSS -> gloss()
            Finish.MATTE -> matte()
            Finish.ENAMEL -> Unit
        }
    }

    // region Finishes

    /** Satin aluminium with a painted colour band across the lower part, like the first portables. */
    private fun DrawScope.brushed() {
        val m = skin.body
        drawRect(Brush.verticalGradient(0f to lerp(m, Color.White, 0.45f), 0.22f to m, 0.7f to lerp(m, Color.Black, 0.06f), 1f to lerp(m, Color.Black, 0.2f)))
        val alphas = floatArrayOf(0.06f, 0.09f, 0.05f, 0.11f)
        hairlines.forEachIndexed { i, p ->
            drawPath(p, if (i % 2 == 0) Color.White.copy(alpha = alphas[i]) else Color.Black.copy(alpha = alphas[i]), style = Stroke(0.8f))
        }
        // Anisotropic sheen: a soft vertical highlight that brushed metal throws.
        drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.35f to Color.White.copy(alpha = 0.18f), 0.5f to Color.Transparent, 0.8f to Color.White.copy(alpha = 0.08f), 1f to Color.Transparent))
        // Painted band with a chamfered metal edge above it.
        val bandTop = h * 0.8f
        val band = skin.trim
        drawRect(Brush.verticalGradient(listOf(lerp(band, Color.White, 0.15f), band, lerp(band, Color.Black, 0.25f)), bandTop, h), Offset(0f, bandTop), Size(w, h - bandTop))
        drawPath(grain, Color.White.copy(alpha = 0.03f))
        drawLine(Color.White.copy(alpha = 0.75f), Offset(0f, bandTop - 2f), Offset(w, bandTop - 2f), 1.5f)
        drawLine(Color.Black.copy(alpha = 0.45f), Offset(0f, bandTop), Offset(w, bandTop), 1.5f)
        capPiece(lerp(m, Color.White, 0.3f))
        bevel()
    }

    /** Ribbed rubber over a yellow shell, a dark gasket and exposed screws: built to get wet. */
    private fun DrawScope.rubber() {
        val c = skin.body
        drawRect(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.1f), c, lerp(c, Color.Black, 0.14f))))
        drawPath(grainDark, Color.Black.copy(alpha = 0.06f))
        drawPath(grain, Color.White.copy(alpha = 0.05f))
        // Moulded grip ribs down both edges.
        val ribW = min(w, h) * 0.035f
        var y = h * 0.2f
        while (y < h * 0.92f) {
            for (x in listOf(ribW * 0.6f, w - ribW * 1.6f)) {
                drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(x, y + 1.5f), Size(ribW, ribW * 0.55f), CornerRadius(ribW * 0.3f))
                drawRoundRect(lerp(c, Color.White, 0.25f), Offset(x, y), Size(ribW, ribW * 0.5f), CornerRadius(ribW * 0.3f))
            }
            y += ribW * 1.3f
        }
        // The gasket: a dark rubber seal inset from the edge.
        val g = min(w, h) * 0.025f
        drawRoundRect(skin.trim, Offset(g, g), Size(w - 2 * g, h - 2 * g), CornerRadius(r.x * 0.8f), style = Stroke(g * 0.9f))
        drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(g - g * 0.35f, g - g * 0.35f), Size(w - 2 * g + g * 0.7f, h - 2 * g + g * 0.7f), CornerRadius(r.x * 0.8f), style = Stroke(1f))
        capPiece(skin.trim, seamLight = 0.15f)
        val s = g * 0.9f
        listOf(Offset(g * 2.6f, h - g * 2.6f), Offset(w - g * 2.6f, h - g * 2.6f), Offset(g * 2.6f, cap + g * 2.6f), Offset(w - g * 2.6f, cap + g * 2.6f))
            .forEachIndexed { i, p -> drawScrew(p, s, 25f + i * 31f) }
    }

    /** Mirror chrome: horizon reflections, sharp highlights and a black edge. */
    private fun DrawScope.chrome() {
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFFF7F8F9), 0.12f to Color(0xFFD8DCE1), 0.3f to Color(0xFF6F757D), 0.36f to Color(0xFF3E4248),
                0.42f to Color(0xFFEDEFF2), 0.6f to Color(0xFFB4BAC2), 0.78f to Color(0xFF51565D), 0.9f to Color(0xFF8D939B), 1f to Color(0xFFD7DBE0),
            ),
        )
        // Diagonal streaks of studio light.
        rotate(-28f, Offset(w / 2, h / 2)) {
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent), w * 0.18f, w * 0.30f), Offset(-w, -h), Size(w * 3, h * 3))
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent), w * 0.62f, w * 0.68f), Offset(-w, -h), Size(w * 3, h * 3))
        }
        capPiece(Color(0xFF1A1B1E), seamLight = 0.2f)
        drawRoundRect(skin.trim, cornerRadius = r, style = Stroke(min(w, h) * 0.02f))
        drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(min(w, h) * 0.02f, min(w, h) * 0.02f), Size(w - min(w, h) * 0.04f, h - min(w, h) * 0.04f), CornerRadius(r.x * 0.9f), style = Stroke(1.2f))
    }

    /** Transparent plastic: the mechanism and circuit board show through a tinted shell. */
    private fun DrawScope.clear() {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1C2630), Color(0xFF111820))))
        internals()
        // The tinted shell over it all.
        drawRect(Brush.verticalGradient(listOf(skin.body.copy(alpha = 0.42f), skin.body.copy(alpha = 0.28f), skin.body.copy(alpha = 0.38f))))
        drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.White.copy(alpha = 0.1f), Color.Transparent), Offset.Zero, Offset(w, h)))
        rotate(-24f, Offset(w / 2, h / 2)) {
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent), w * 0.2f, w * 0.34f), Offset(-w, -h), Size(w * 3, h * 3))
        }
        capPiece(skin.trim.copy(alpha = 0.55f), seamLight = 0.4f)
        // Polished edges of the moulding catch the light.
        val e = min(w, h) * 0.012f
        drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(e, e), Size(w - 2 * e, h - 2 * e), CornerRadius(r.x * 0.9f), style = Stroke(e))
    }

    /** What you see through a clear case: board, chips, motor, flywheel and belt. */
    private fun DrawScope.internals() {
        val u = min(w, h) / 100f
        // Circuit board along the bottom and up one side.
        val pcb = Color(0xFF1E4D36)
        drawRect(pcb, Offset(w * 0.06f, h * 0.72f), Size(w * 0.88f, h * 0.22f))
        drawRect(pcb, Offset(w * 0.78f, h * 0.18f), Size(w * 0.16f, h * 0.56f))
        val trace = Color(0xFFC9A646).copy(alpha = 0.55f)
        val tr = Random(9)
        repeat(26) {
            val x = w * 0.08f + tr.nextFloat() * w * 0.82f
            val y = h * 0.74f + tr.nextFloat() * h * 0.18f
            val len = (6 + tr.nextFloat() * 16) * u
            drawLine(trace, Offset(x, y), Offset(x + len, y), 0.5f * u)
            drawLine(trace, Offset(x + len, y), Offset(x + len, y + 3 * u), 0.5f * u)
            drawCircle(trace, 0.9f * u, Offset(x, y))
        }
        // Chips and parts.
        listOf(Rect(0.14f, 0.78f, 0.30f, 0.86f), Rect(0.36f, 0.79f, 0.46f, 0.84f), Rect(0.80f, 0.30f, 0.92f, 0.40f)).forEach { f ->
            drawRect(Color(0xFF151618), Offset(w * f.left, h * f.top), Size(w * f.width, h * f.height))
            drawRect(Color.White.copy(alpha = 0.08f), Offset(w * f.left, h * f.top), Size(w * f.width, h * f.height), style = Stroke(0.4f * u))
        }
        listOf(0.55f, 0.62f, 0.69f).forEach { fx ->
            drawCircle(Color(0xFF2F5FA8), 2.4f * u, Offset(w * fx, h * 0.82f))
            drawCircle(Color.White.copy(alpha = 0.3f), 2.4f * u, Offset(w * fx, h * 0.82f), style = Stroke(0.4f * u))
        }
        // Motor can and flywheel joined by the drive belt.
        val motor = Offset(w * 0.2f, h * 0.3f)
        val fly = Offset(w * 0.55f, h * 0.48f)
        drawLine(Color(0xFF0B0B0C), motor + Offset(0f, -2.5f * u), fly + Offset(0f, -12f * u), 1.6f * u)
        drawLine(Color(0xFF0B0B0C), motor + Offset(0f, 2.5f * u), fly + Offset(0f, 12f * u), 1.6f * u)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFE9EBEE), Color(0xFF8A8E94), Color(0xFF55595F)), motor - Offset(2 * u, 2 * u), 10 * u), 8 * u, motor)
        drawCircle(Color(0xFF2A2C30), 2.5f * u, motor)
        drawCircle(Color(0xFF9AA0A7), 12 * u, fly)
        drawCircle(Color(0xFF6B7077), 12 * u, fly, style = Stroke(1.2f * u))
        for (k in 0 until 6) rotate(k * 60f, fly) {
            drawLine(Color(0xFF4E5258), fly, fly + Offset(0f, -10f * u), 1.2f * u)
        }
        drawCircle(Color(0xFFD7DADF), 2.6f * u, fly)
        // A couple of nylon gears.
        listOf(Offset(w * 0.35f, h * 0.6f) to 5.5f, Offset(w * 0.42f, h * 0.2f) to 4f).forEach { (c, rr) ->
            drawCircle(Color(0xFFE8E2D2).copy(alpha = 0.85f), rr * u, c)
            for (k in 0 until 14) rotate(k * 360f / 14, c) {
                drawRect(Color(0xFFE8E2D2).copy(alpha = 0.85f), Offset(c.x - 0.6f * u, c.y - rr * u - 1.1f * u), Size(1.2f * u, 1.4f * u))
            }
            drawCircle(Color(0xFF8D8778), 1.2f * u, c)
        }
    }

    /** Deep glossy paint with a mirror highlight and chrome trim. */
    private fun DrawScope.gloss() {
        val c = skin.body
        drawRect(Brush.verticalGradient(0f to lerp(c, Color.White, 0.22f), 0.35f to c, 1f to lerp(c, Color.Black, 0.38f)))
        // Wet-look reflection: a broad soft band near the top and a crisp window highlight.
        drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.04f), Color.Transparent), 0f, h * 0.38f), Offset.Zero, Size(w, h * 0.38f))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent), w * 0.05f, w * 0.4f), Offset(0f, h * 0.06f), Size(w, h * 0.012f))
        drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent), Offset(w * 0.5f, h * 1.2f), h * 0.9f))
        capPiece(lerp(c, Color.White, 0.18f))
        trimRing(skin.trim)
    }

    /** Soft-touch black with a fine texture and an orange pinstripe. */
    private fun DrawScope.matte() {
        val c = skin.body
        drawRect(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.07f), c, lerp(c, Color.Black, 0.3f))))
        drawPath(grain, Color.White.copy(alpha = 0.035f))
        drawPath(grainDark, Color.Black.copy(alpha = 0.18f))
        val y = h * 0.86f
        drawLine(skin.trim, Offset(0f, y), Offset(w, y), min(w, h) * 0.008f + 1f)
        drawLine(skin.trim.copy(alpha = 0.5f), Offset(0f, y + min(w, h) * 0.018f), Offset(w, y + min(w, h) * 0.018f), 1f)
        capPiece(lerp(c, Color.White, 0.06f), seamLight = 0.08f)
        bevel(strength = 0.5f)
    }

    // endregion

    /** The separate cap piece across the top, with its seam. */
    private fun DrawScope.capPiece(color: Color, seamLight: Float = 0.25f) {
        if (cap <= 0f) return
        drawRect(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.12f), color), 0f, cap), size = Size(w, cap))
        drawLine(Color.Black.copy(alpha = 0.4f), Offset(0f, cap), Offset(w, cap), 2f)
        drawLine(Color.White.copy(alpha = seamLight), Offset(0f, cap + 2f), Offset(w, cap + 2f), 1f)
    }

    private fun DrawScope.bevel(strength: Float = 1f) {
        drawRoundRect(
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f * strength), Color.Transparent, Color.Black.copy(alpha = 0.16f * strength)), Offset.Zero, Offset(w, h)),
            cornerRadius = r,
        )
    }

    /** A polished metal strip just inside the edge. */
    private fun DrawScope.trimRing(metal: Color) {
        val t = min(w, h) * 0.018f
        val inset = t * 1.2f
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White, metal, lerp(metal, Color.Black, 0.45f), metal, Color.White)),
            Offset(inset, inset), Size(w - 2 * inset, h - 2 * inset), CornerRadius(r.x * 0.85f), style = Stroke(t),
        )
    }
}
