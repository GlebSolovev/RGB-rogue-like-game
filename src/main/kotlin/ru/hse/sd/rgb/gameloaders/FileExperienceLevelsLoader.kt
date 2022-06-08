@file:Suppress("WildcardImport")

package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceLevelAction
import ru.hse.sd.rgb.gamelogic.engines.experience.ExperienceLevelDescription
import ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions.IncreaseMaxHpAction
import ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions.NoAction
import ru.hse.sd.rgb.gamelogic.engines.experience.scriptactions.SpawnItemAction
import ru.hse.sd.rgb.gamelogic.engines.items.ItemEntityCreator
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.ColorInverterEntityCreator
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.ColorModificationEntityCreator
import ru.hse.sd.rgb.gamelogic.engines.items.scriptitems.InstantHealEntityCreator
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.modules.*
import java.io.File

private val module = SerializersModule {
    polymorphic(ExperienceLevelAction::class) {
        subclass(NoAction::class)
        subclass(SpawnItemAction::class)
        subclass(IncreaseMaxHpAction::class)
    }
    polymorphic(ItemEntityCreator::class) {
        subclass(ColorInverterEntityCreator::class)
        subclass(ColorModificationEntityCreator::class)
        subclass(InstantHealEntityCreator::class)
    }
}

class FileExperienceLevelsLoader(private val heroExperienceLevelsFilename: String) : ExperienceLevelsLoader {

    override fun loadHeroExperienceLevels(): List<ExperienceLevelDescription> {
        val stream = File(heroExperienceLevelsFilename).inputStream()
        val format = Yaml(serializersModule = module)
        return format.decodeFromStream(stream)
    }
}
