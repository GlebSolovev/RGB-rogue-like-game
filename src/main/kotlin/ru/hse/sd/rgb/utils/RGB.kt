package ru.hse.sd.rgb.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
