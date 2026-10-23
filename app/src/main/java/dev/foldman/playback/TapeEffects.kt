package dev.foldman.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlin.random.Random

/**
 * What the tape sounds like: hiss, wow and flutter, MEGA BASS and a five-band graphic EQ. The UI
 * writes these and [TapeEffectsProcessor] reads them on the audio thread, so they are volatile and
 * every change bumps [version] for the processor to pick up.
 */
object TapeFx {
    val BANDS_HZ = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
    const val EQ_RANGE_DB = 12f

    @Volatile var hiss = false
    @Volatile var flutter = false
    @Volatile var megaBass = false
    @Volatile var eq: FloatArray = FloatArray(5)
        private set
    @Volatile var version = 0
        private set

    val presets: Map<String, FloatArray> = linkedMapOf(
        "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Rock" to floatArrayOf(5f, 2f, -2f, 3f, 5f),
        "Pop" to floatArrayOf(-1f, 3f, 4f, 2f, -1f),
        "Vocal" to floatArrayOf(-3f, -1f, 4f, 3f, 0f),
        "Jazz" to floatArrayOf(3f, 1f, -1f, 2f, 4f),
        "Treble" to floatArrayOf(-2f, -1f, 0f, 4f, 7f),
    )

    private const val PREFS = "foldman"

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        hiss = p.getBoolean("fx_hiss", false)
        flutter = p.getBoolean("fx_flutter", false)
        megaBass = p.getBoolean("fx_mega_bass", false)
        eq = FloatArray(5) { p.getFloat("fx_eq_$it", 0f) }
        version++
    }

    fun update(context: Context, hiss: Boolean = this.hiss, flutter: Boolean = this.flutter, megaBass: Boolean = this.megaBass, eq: FloatArray = this.eq) {
        this.hiss = hiss
        this.flutter = flutter
        this.megaBass = megaBass
        this.eq = eq.copyOf().also { a -> for (i in a.indices) a[i] = a[i].coerceIn(-EQ_RANGE_DB, EQ_RANGE_DB) }
        version++
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean("fx_hiss", hiss)
            putBoolean("fx_flutter", flutter)
            putBoolean("fx_mega_bass", megaBass)
            this@TapeFx.eq.forEachIndexed { i, g -> putFloat("fx_eq_$i", g) }
        }
    }

    val anyOn: Boolean get() = hiss || flutter || megaBass || eq.any { abs(it) > 0.05f }
}

/** A second-order IIR section (RBJ cookbook), one state pair per channel. */
private class Biquad(channels: Int) {
    var b0 = 1f; var b1 = 0f; var b2 = 0f; var a1 = 0f; var a2 = 0f
    private val z1 = FloatArray(channels)
    private val z2 = FloatArray(channels)
    var enabled = false

    fun peaking(fs: Float, f0: Float, gainDb: Float, q: Float) {
        enabled = abs(gainDb) > 0.05f
        val a = 10f.pow(gainDb / 40f)
        val w = 2f * PI.toFloat() * f0 / fs
        val alpha = sin(w) / (2f * q)
        val cw = cos(w)
        set(1 + alpha * a, -2 * cw, 1 - alpha * a, 1 + alpha / a, -2 * cw, 1 - alpha / a)
    }

    fun lowShelf(fs: Float, f0: Float, gainDb: Float) {
        enabled = abs(gainDb) > 0.05f
        val a = 10f.pow(gainDb / 40f)
        val w = 2f * PI.toFloat() * f0 / fs
        val cw = cos(w)
        val alpha = sin(w) / 2f * sqrt(2f) // S = 1
        val sa = 2f * sqrt(a) * alpha
        set(
            a * ((a + 1) - (a - 1) * cw + sa), 2 * a * ((a - 1) - (a + 1) * cw), a * ((a + 1) - (a - 1) * cw - sa),
            (a + 1) + (a - 1) * cw + sa, -2 * ((a - 1) + (a + 1) * cw), (a + 1) + (a - 1) * cw - sa,
        )
    }

    private fun set(nb0: Float, nb1: Float, nb2: Float, na0: Float, na1: Float, na2: Float) {
        b0 = nb0 / na0; b1 = nb1 / na0; b2 = nb2 / na0; a1 = na1 / na0; a2 = na2 / na0
    }

    fun process(x: Float, ch: Int): Float {
        // Transposed direct form II.
        val y = b0 * x + z1[ch]
        z1[ch] = b1 * x - a1 * y + z2[ch]
        z2[ch] = b2 * x - a2 * y
        return y
    }

    fun reset() { z1.fill(0f); z2.fill(0f) }
}

/**
 * Runs the tape effects on the local player's PCM (16-bit or float), ahead of the VU meter tap so
 * the needles hear what you hear. With everything off it is a straight copy.
 */
@OptIn(UnstableApi::class)
class TapeEffectsProcessor : BaseAudioProcessor() {
    private var channels = 2
    private var rate = 44100f
    private var seenVersion = -1

    private var eq = emptyList<Biquad>()
    private var bass = Biquad(2)

    // Wow and flutter: a short delay line whose read point drifts, which bends the pitch.
    private var delay = Array(2) { FloatArray(DELAY_SIZE) }
    private var writePos = 0
    private var phaseWow = 0.0
    private var phaseFlutter = 0.0
    private var drift = 0f

    // Hiss: white noise tilted towards the treble, and a gentle tape roll-off.
    private val rnd = Random(42)
    private var hissHp = FloatArray(2)
    private var hissPrev = FloatArray(2)
    private var rolloff = FloatArray(2)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        rate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        eq = List(TapeFx.BANDS_HZ.size) { Biquad(channels) }
        bass = Biquad(channels)
        delay = Array(channels) { FloatArray(DELAY_SIZE) }
        writePos = 0
        hissHp = FloatArray(channels)
        hissPrev = FloatArray(channels)
        rolloff = FloatArray(channels)
        seenVersion = -1
    }

    private fun refresh() {
        if (seenVersion == TapeFx.version) return
        seenVersion = TapeFx.version
        val gains = TapeFx.eq
        eq.forEachIndexed { i, b -> b.peaking(rate, TapeFx.BANDS_HZ[i].coerceAtMost(rate * 0.45f), gains.getOrElse(i) { 0f }, 1.1f) }
        bass.lowShelf(rate, 95f, if (TapeFx.megaBass) 10f else 0f)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val out = replaceOutputBuffer(bytes)
        if (!TapeFx.anyOn) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        refresh()
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val hiss = TapeFx.hiss
        val flutter = TapeFx.flutter
        val bassOn = bass.enabled
        val anyEq = eq.any { it.enabled }
        // Leave a little headroom for boosts, then catch peaks with a soft limiter.
        val preGain = when {
            bassOn -> 0.6f
            anyEq && TapeFx.eq.max() > 3f -> 0.8f
            else -> 1f
        }
        val wowStep = 2.0 * PI * WOW_HZ / rate
        val flutterStep = 2.0 * PI * FLUTTER_HZ / rate
        val sampleBytes = if (isFloat) 4 else 2
        val frames = bytes / (sampleBytes * channels)
        for (f in 0 until frames) {
            // One read offset per frame keeps the channels in step.
            var readOffset = 0f
            if (flutter) {
                phaseWow += wowStep
                phaseFlutter += flutterStep
                if (phaseWow > 2 * PI) phaseWow -= 2 * PI
                if (phaseFlutter > 2 * PI) phaseFlutter -= 2 * PI
                drift += (rnd.nextFloat() - 0.5f - drift) * 0.0005f
                val ms = BASE_DELAY_MS + WOW_DEPTH_MS * sin(phaseWow).toFloat() +
                    FLUTTER_DEPTH_MS * sin(phaseFlutter).toFloat() + drift * 0.4f
                readOffset = ms * rate / 1000f
            }
            for (c in 0 until channels) {
                var x = if (isFloat) input.float else input.short / 32768f
                x *= preGain
                if (bassOn) x = bass.process(x, c)
                if (anyEq) for (b in eq) if (b.enabled) x = b.process(x, c)
                if (flutter) {
                    val line = delay[c]
                    line[writePos] = x
                    var rp = writePos - readOffset
                    if (rp < 0) rp += DELAY_SIZE
                    val i0 = rp.toInt() % DELAY_SIZE
                    val i1 = (i0 + 1) % DELAY_SIZE
                    val t = rp - rp.toInt()
                    x = line[i0] * (1 - t) + line[i1] * t
                }
                if (hiss) {
                    // Treble-tilted noise around -50 dBFS plus a soft roll-off near 11 kHz.
                    val white = rnd.nextFloat() * 2f - 1f
                    hissHp[c] = 0.86f * (hissHp[c] + white - hissPrev[c])
                    hissPrev[c] = white
                    rolloff[c] += (x - rolloff[c]) * 0.82f
                    x = rolloff[c] + hissHp[c] * HISS_LEVEL
                }
                x = softLimit(x)
                if (isFloat) out.putFloat(x) else out.putShort((x * 32767f).toInt().coerceIn(-32768, 32767).toShort())
            }
            if (flutter) writePos = (writePos + 1) % DELAY_SIZE
        }
        // Any trailing partial frame is copied through untouched.
        while (input.hasRemaining()) out.put(input.get())
        out.flip()
    }

    override fun onReset() {
        eq.forEach { it.reset() }
        bass.reset()
    }

    private fun softLimit(x: Float): Float {
        val a = abs(x)
        return if (a <= KNEE) x else sign(x) * (KNEE + (1 - KNEE) * tanh((a - KNEE) / (1 - KNEE)))
    }

    private companion object {
        const val DELAY_SIZE = 4096
        const val BASE_DELAY_MS = 6f
        const val WOW_HZ = 0.55
        const val WOW_DEPTH_MS = 1.1f
        const val FLUTTER_HZ = 7.3
        const val FLUTTER_DEPTH_MS = 0.035f
        const val HISS_LEVEL = 0.0032f
        const val KNEE = 0.85f
    }
}
