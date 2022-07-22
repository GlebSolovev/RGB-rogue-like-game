package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gamelogic.engines.experience.Experience
import ru.hse.sd.rgb.gamelogic.engines.items.InventoryPersistence
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.utils.structures.GridShift
import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.generateRandomColor
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.random.Random

@Serializable
data class HeroInitParams(
    val unitsInitParams: List<HpUnitInitParams>,
    val inventoryDescription: InventoryDescription,
    val singleDirMovePeriodLimitMillis: Long,
) {
    fun convertToInitialHeroPersistence(): HeroPersistence = HeroPersistence(
        unitsInitParams.map {
            HeroPersistence.HpUnitPersistence(it.relativeShift, it.color, it.maxHp, it.maxHp)
        },
        InventoryPersistence(inventoryDescription.invGridW, inventoryDescription.invGridH),
        singleDirMovePeriodLimitMillis,
        Experience(0, 0)
    )
}

@Serializable
data class HpUnitInitParams(
    val relativeShift: GridShift,
    val color: RGB,
    val maxHp: Int,
)

interface HeroLoader {
    fun loadHeroInitParams(): HeroInitParams
}

class FileHeroLoader(private val heroFilename: String) : HeroLoader {
    override fun loadHeroInitParams() = Yaml().decodeFromStream<HeroInitParams>(File(heroFilename).inputStream())
}

@Suppress("MagicNumber")
class RandomHeroLoader(private val random: Random = Random) : HeroLoader {
    override fun loadHeroInitParams() = HeroInitParams(
        listOf(
            run {
                val relativeShift = GridShift(0, 0)
                val color = generateRandomColor(random)
                val maxHp = random.nextInt(7, 15)
                HpUnitInitParams(relativeShift, color, maxHp)
            }
        ),
        run {
            val invGridWidth = random.nextInt(1, 4)
            val invGridHeight = random.nextInt(1, 4)
            InventoryDescription(invGridWidth, invGridHeight)
        },
        singleDirMovePeriodLimitMillis = 50L
    )
}
