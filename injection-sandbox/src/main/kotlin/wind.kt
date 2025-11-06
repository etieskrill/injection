import org.etieskrill.engine.application.App
import org.etieskrill.engine.audio.Audio
import org.jtransforms.fft.FloatFFT_1D
import org.lwjgl.BufferUtils
import kotlin.random.Random

fun main() = Wind.run()

object Wind : App() {
    init {
        val audioBuffer = BufferUtils.createShortBuffer(10 * 44100)
        val random = Random(69420)
        while (audioBuffer.position() < audioBuffer.capacity()) {
            audioBuffer.put(random.nextDouble().toRawBits().toShort())
        }
        audioBuffer.flip()

        val values = FloatArray(audioBuffer.capacity()) { audioBuffer.get().toFloat() }
        for (i in values.indices) values[i] /= 4
        audioBuffer.rewind()

        val fft = FloatFFT_1D(audioBuffer.capacity().toLong())
        fft.realForward(values)

        for ((index, i) in (values.size / 25..<values.size).withIndex()) {
            if (index < (values.size - values.size / 25) / 25) values[i] = values[i] * 1f
            else values[i] = 0f
        }
        //TODO
        // - improve low pass filter (or make it a low pass filter to begin with)
        // - boost bass / why is it so flat to begin with?
        // - resonance/q
        // - lfo
        // OR give up and
        // - get free wind sound from somewhere
        // - find way to modulate (hopefully) monotonous wind sound

        fft.realInverse(values, true)

        values.forEach { audioBuffer.put(it.toInt().toShort()) }
        audioBuffer.flip()

        val source = Audio.createSource(44100, audioBuffer)
        source.play()
    }

    override fun loop(delta: Double) {}
}