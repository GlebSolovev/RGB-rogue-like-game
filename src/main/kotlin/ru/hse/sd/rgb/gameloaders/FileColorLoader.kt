package ru.hse.sd.rgb.gameloaders

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.*
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorStats
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects.FireballEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects.WaveEffect
import ru.hse.sd.rgb.utils.Grid2D
import ru.hse.sd.rgb.utils.WrongConfigError
import java.io.File

@Serializable
data class FileColorsDesc(
    val baseColors: List<BaseColorStats>,
    val interactionMatrix: List<List<Int>>,
)

private val module = SerializersModule {
    polymorphic(BaseColorUpdateEffect::class) {
        subclass(FireballEffect::class)
        subclass(WaveEffect::class)
    }
}

class FileColorLoader(private val colorsFilename: String) : ColorLoader {

    override fun loadColors(): BaseColorParams {
        val stream = File(colorsFilename).inputStream()
        val format = Yaml(serializersModule = module)
        val desc = format.decodeFromStream<FileColorsDesc>(stream)
        val interactionMatrix = Grid2D(desc.interactionMatrix)
        if (interactionMatrix.h != interactionMatrix.w || interactionMatrix.h != desc.baseColors.size)
            throw WrongConfigError("bad interaction matrix")
        return BaseColorParams(desc.baseColors, interactionMatrix)
    }

}
