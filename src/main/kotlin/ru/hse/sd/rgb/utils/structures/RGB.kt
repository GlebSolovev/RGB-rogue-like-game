package ru.hse.sd.rgb.utils.structures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Serializable(with = RGBSerializer::class)
data class RGB(val r: Int, val g: Int, val b: Int) {

    companion object {
        const val MIN_COMPONENT_VALUE = 0
        const val MAX_COMPONENT_VALUE = 255
        val COMPONENT_RANGE = MIN_COMPONENT_VALUE..MAX_COMPONENT_VALUE

        const val COMPONENTS_NUMBER = 3
    }

    init {
        require(r in COMPONENT_RANGE && g in COMPONENT_RANGE && b in COMPONENT_RANGE)
    }
}

object RGBSerializer : KSerializer<RGB> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RGB", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: RGB) {
        val (r, g, b) = value
        encoder.encodeString("$r $g $b")
    }

    override fun deserialize(decoder: Decoder): RGB {
        val (r, g, b) = decoder.decodeString().split(" +".toRegex()).map { it.toInt() }
        return RGB(r, g, b)
    }
}

data class RGBDelta(val dr: Int, val dg: Int, val db: Int) {

    companion object {
        private const val MIN_COMPONENT_VALUE = -255
        private const val MAX_COMPONENT_VALUE = 255
        val COMPONENT_RANGE = MIN_COMPONENT_VALUE..MAX_COMPONENT_VALUE
    }

    init {
        require(dr in COMPONENT_RANGE && dg in COMPONENT_RANGE && db in COMPONENT_RANGE)
    }

    @Suppress("MagicNumber")
    fun convertToViewRGB(): RGB { // TODO: maybe improve
        fun exaggerate(dc: Int) = (dc + 10) * 4

        var rgbR = exaggerate(dr)
        var rgbG = exaggerate(dg)
        var rgbB = exaggerate(db)
        if (dr < 0 && dg < 0 && db < 0) return RGB(
            RGB.MIN_COMPONENT_VALUE,
            RGB.MIN_COMPONENT_VALUE,
            RGB.MIN_COMPONENT_VALUE
        )
        when (maxOf(dr, dg, db)) {
            dr -> rgbR += 100 + (dr + 10)
            dg -> rgbG += 100 + (dg + 10)
            db -> rgbB += 100 + (db + 10)
        }
        return RGB(limitComponent(rgbR), limitComponent(rgbG), limitComponent(rgbB))
    }
}

operator fun RGB.plus(delta: RGBDelta): RGB {
    val (r, g, b) = this
    val (dr, dg, db) = delta
    return RGB(limitComponent(r + dr), limitComponent(g + dg), limitComponent(b + db))
}

fun RGB.invert(): RGB {
    val (r, g, b) = this
    return RGB(RGB.MAX_COMPONENT_VALUE - r, RGB.MAX_COMPONENT_VALUE - g, RGB.MAX_COMPONENT_VALUE - b)
}

private fun limitComponent(c: Int) = min(RGB.MAX_COMPONENT_VALUE, max(RGB.MIN_COMPONENT_VALUE, c))

fun generateRandomColor(random: Random = Random): RGB =
    List(RGB.COMPONENTS_NUMBER) { random.nextInt(RGB.MAX_COMPONENT_VALUE + 1) }.let { (r, g, b) -> RGB(r, g, b) }
