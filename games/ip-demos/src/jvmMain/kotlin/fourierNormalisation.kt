import org.etieskrill.engine.audio.Audio
import org.joml.Math
import org.jtransforms.fft.FloatFFT_1D
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.sin

fun main() {
    transform(FloatArray(2) { 1f })
    transform(FloatArray(4) { 1f })
    transform(FloatArray(8) { 1f })
    println()
    transform(FloatArray(10) { 1f })
    transform(FloatArray(10) { -1f })
    println()
    transform(FloatArray(2) { if (it % 2 == 0) 1f else -1f })
    transform(FloatArray(4) { if (it % 2 == 0) 1f else -1f })
    transform(FloatArray(8) { if (it % 2 == 0) 1f else -1f })
    println()
    transform(FloatArray(10) { if (it % 2 == 0) 1f else -1f })
    transform(FloatArray(10) { if (it % 2 == 0) 1f else -1f })
    println()
    transform(FloatArray(10) { if (it % 2 == 0) 2f else -2f })
    transform(FloatArray(10) { if (it % 2 == 0) 4f else -4f })
    transform(FloatArray(10) { if (it % 2 == 0) 8f else -8f })
    println()
    transform(FloatArray(2) { if (it % 2 == 0) 4f else -6f })
    transform(FloatArray(4) { if (it % 2 == 0) 4f else -6f })
    transform(FloatArray(8) { if (it % 2 == 0) 4f else -6f })
    transform(FloatArray(16) { if (it % 2 == 0) 4f else -6f })
    println()
    val freq: Double = 1.0 / 8
    transform(FloatArray(16) { sin(freq * (it.toDouble() * Math.PI_OVER_2)).toFloat() })

//    val sineSample = Audio.read("audio/1khz-sine.ogg", retainBuffer = true)
//    val buffer = ShortArray(1024)
//    transform(sineSample.buffer[2 * 44100, buffer.])
}

fun transform(a: FloatArray) {
    val maxRealValue = a.max()
//    println(a.joinToString(separator = ", ", prefix = "[", postfix = "]") { if (abs(it) > 1e-6) it.toString() else "\"0.0\"" })
    println(a.contentToString())

    FloatFFT_1D(a.size.toLong()).realForward(a)
    print(a.joinToString(separator = ", ", prefix = "[", postfix = "]") { if (abs(it) > 1e-6) it.toString() else "\"0.0\"" })

    val copy = a.map { it / (maxRealValue * a.size) }
    println(" -> ${copy.joinToString(separator = ", ", prefix = "[", postfix = "]") { if (abs(it) > 1e-6) it.toString() else "\"0.0\"" }}")
}
