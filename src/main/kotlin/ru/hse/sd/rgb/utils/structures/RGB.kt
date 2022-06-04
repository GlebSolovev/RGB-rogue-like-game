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

@Serializable(with = RGBSerializer::class)
data class RGB(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255)
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
    init {
        require(dr in -255..255 && dg in -255..255 && db in -255..255)
    }

    fun saturate(): RGB {
        // TODO: not representative
        val max = maxOf(dr, dg, db, 1).toDouble()
        val min = minOf(dr, dg, db, 0).toDouble()
        fun scale(dc: Int) = (255 * (dc - min) / (max - min)).toInt()
        return RGB(scale(dr), scale(dg), scale(db))
    }
}

operator fun RGB.plus(delta: RGBDelta): RGB {
    val (r, g, b) = this
    val (dr, dg, db) = delta
    fun limit(value: Int) = max(0, min(value, 255))
    return RGB(limit(r + dr), limit(g + dg), limit(b + db))
}
