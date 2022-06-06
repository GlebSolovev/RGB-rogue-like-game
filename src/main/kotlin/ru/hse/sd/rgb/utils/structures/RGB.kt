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

    fun convertToViewRGB(): RGB { // TODO: maybe improve
        fun limit(c: Int) = min(255, max(0, c))
        fun exaggerate(dc: Int) = (dc + 10) * 4

        var rgbR = exaggerate(dr)
        var rgbG = exaggerate(dg)
        var rgbB = exaggerate(db)
        if(dr < 0 && dg < 0 && db < 0) return RGB(0, 0, 0)
        when (maxOf(dr, dg, db)) {
            dr -> rgbR += 100 + (dr + 10)
            dg -> rgbG += 100 + (dg + 10)
            db -> rgbB += 100 + (db + 10)
        }
        return RGB(limit(rgbR), limit(rgbG), limit(rgbB))
    }
}

operator fun RGB.plus(delta: RGBDelta): RGB {
    val (r, g, b) = this
    val (dr, dg, db) = delta
    fun limit(value: Int) = max(0, min(value, 255))
    return RGB(limit(r + dr), limit(g + dg), limit(b + db))
}
