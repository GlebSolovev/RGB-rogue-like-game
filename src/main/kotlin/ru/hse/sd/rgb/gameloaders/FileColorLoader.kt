package ru.hse.sd.rgb.gameloaders

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.*
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorStats
import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects.FireballEffect
import ru.hse.sd.rgb.utils.Grid2D
import java.io.File

@Serializable
data class FileColorsDesc(
    val baseColors: List<BaseColorStats>,
    val interactionMatrix: List<List<Int>>,
)

private val module = SerializersModule {
    polymorphic(BaseColorUpdateEffect::class) {
        subclass(FireballEffect::class)
    }
}

class FileColorLoader(private val colorsFilename: String) : ColorLoader {

    override fun loadColors(): BaseColorParams {
        val stream = File(colorsFilename).inputStream()
        val format = Yaml(serializersModule = module)
        val desc = format.decodeFromStream<FileColorsDesc>(stream)
        return BaseColorParams(desc.baseColors, Grid2D(desc.interactionMatrix))
    }

}