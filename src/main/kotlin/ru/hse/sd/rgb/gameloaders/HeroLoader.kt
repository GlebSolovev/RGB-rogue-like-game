package ru.hse.sd.rgb.gameloaders

import ru.hse.sd.rgb.gamelogic.engines.experience.Experience
import ru.hse.sd.rgb.gamelogic.engines.items.InventoryPersistence
import ru.hse.sd.rgb.gamelogic.entities.scriptentities.HeroPersistence
import ru.hse.sd.rgb.utils.structures.GridShift
import ru.hse.sd.rgb.utils.structures.RGB
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.random.Random

@Serializable
data class HeroInitParams(
    val unitsInitParams: List<HpUnitInitParams>,
    val inventoryDescription: InventoryDescription,
    val singleDirMovePeriodLimit: Long,
) {
    fun convertToInitialHeroPersistence(): HeroPersistence = HeroPersistence(
        unitsInitParams.map {
            HeroPersistence.HpUnitPersistence(it.relativeShift, it.color, it.maxHp, it.maxHp)
        },
        InventoryPersistence(inventoryDescription.invGridW, inventoryDescription.invGridH),
        singleDirMovePeriodLimit,
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
                val (r, g, b) = List(3) { random.nextInt(0, 255) }
                HpUnitInitParams(GridShift(0, 0), RGB(r, g, b), random.nextInt(7, 15))
            }
        ),
        InventoryDescription(random.nextInt(1, 4), random.nextInt(1, 4)),
        50L
    )
}
