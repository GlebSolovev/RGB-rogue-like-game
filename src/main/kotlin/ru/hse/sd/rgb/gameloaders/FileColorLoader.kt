@file:Suppress("WildcardImport")

package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorStats
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects.*
import ru.hse.sd.rgb.utils.WrongConfigError
import ru.hse.sd.rgb.utils.structures.Grid2D
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.*
import java.io.File

@Serializable
data class FileColorsDesc(
    val baseColors: List<BaseColorStats>,
    val interactionMatrixRepresentation: List<String>,
)

private val module = SerializersModule {
    polymorphic(BaseColorUpdateEffect::class) {
        subclass(FireballEffect::class)
        subclass(WaveEffect::class)
        subclass(LaserEffect::class)
        subclass(HealEffect::class)
        subclass(ConfuseEffect::class)
        subclass(RedSmolderEffect::class)
        subclass(IciclesBombEffect::class)
    }
}

class FileColorLoader(private val colorsFilename: String) : ColorLoader {

    override fun loadColors(): BaseColorParams {
        val stream = File(colorsFilename).inputStream()
        val format = Yaml(serializersModule = module)
        val desc = format.decodeFromStream<FileColorsDesc>(stream)
        val matrix = parseInteractionMatrix(desc.interactionMatrixRepresentation)
        if (matrix.h != matrix.w || matrix.h != desc.baseColors.size)
            throw WrongConfigError("bad interaction matrix")
        return BaseColorParams(desc.baseColors, matrix)
    }
}

private fun parseInteractionMatrix(representation: List<String>) = Grid2D(
    representation.map { line ->
        line.split("\\s+".toRegex()).map { it.toInt() }
    }
)
