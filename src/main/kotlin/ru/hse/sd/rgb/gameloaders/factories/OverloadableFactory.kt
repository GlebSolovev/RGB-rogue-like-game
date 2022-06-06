@file:Suppress("unused")

package ru.hse.sd.rgb.gameloaders.factories

import ru.hse.sd.rgb.utils.structures.RGB
import ru.hse.sd.rgb.utils.structures.RGBDelta
import kotlinx.serialization.Serializable
import kotlin.properties.ReadOnlyProperty

@Serializable
enum class OverloadableFactoryType {
    COLD {
        override val factory = ColdFactory()
    },
    FIERY {
        override val factory = FieryFactory()
    },
    ;

    abstract val factory: LevelContentFactory
}

@Serializable
data class ColorModificationOutcome(
    val weight: Int,
    val result: RGBDelta,
)

@Serializable
data class InstantHealOutcome(
    val weight: Int,
    val result: Int,
)

@Serializable
@Suppress("LongParameterList", "CanBeParameter")
class OverloadableFactory constructor(
    private val baseFactoryType: OverloadableFactoryType,

    private val customBgColor: RGB? = null,
    private val customWallColor: RGB? = null,

    private val customGlitchSpawnCount: Int? = null,
    private val customGlitchHp: Int? = null,
    private val customGlitchClonePeriod: Long? = null,

    private val customSharpySpawnCount: Int? = null,
    private val customSharpyColor: RGB? = null,
    private val customSharpyHp: Int? = null,
    private val customSharpyMovePeriodMillis: Long? = null,
    private val customSharpySeeingDepth: Int? = null,

    private val customColorModificationSpawnCount: Int? = null,
    private val customColorModificationOutcomes: List<ColorModificationOutcome>? = null,
    private val customInstantHealSpawnCount: Int? = null,
    private val customInstantHealOutcomes: List<InstantHealOutcome>? = null,
) : LevelContentFactory() {

    private val factory get() = baseFactoryType.factory

    override val bgColor by customBgColor orElse { factory.bgColor }
    override val wallColor by customWallColor orElse { factory.wallColor }

    override val glitchSpawnCount by customGlitchSpawnCount orElse { factory.glitchSpawnCount }
    override val glitchHp by customGlitchHp orElse { factory.glitchHp }
    override val glitchClonePeriod by customGlitchClonePeriod orElse { factory.glitchClonePeriod }

    override val sharpySpawnCount by customSharpySpawnCount orElse { factory.sharpySpawnCount }
    override val sharpyColor by customSharpyColor orElse { factory.sharpyColor }
    override val sharpyHp by customSharpyHp orElse { factory.sharpyHp }
    override val sharpyMovePeriodMillis by customSharpyMovePeriodMillis orElse { factory.sharpyMovePeriodMillis }
    override val sharpySeeingDepth by customSharpySeeingDepth orElse { factory.sharpySeeingDepth }

    override val colorModificationSpawnCount by customColorModificationSpawnCount orElse {
        factory.colorModificationSpawnCount
    }
    override val colorModificationRGBDeltaGenerationTable
        by ReadOnlyProperty<OverloadableFactory, GenerationTable<RGBDelta>> { _, _ ->
            if (customColorModificationOutcomes == null) {
                factory.colorModificationRGBDeltaGenerationTable
            } else {
                GenerationTable(customColorModificationOutcomes.map { (w, r) -> TableEntry(w) { r } })
            }
        }

    override val instantHealSpawnCount by customInstantHealSpawnCount orElse {
        factory.instantHealSpawnCount
    }
    override val instantHealGenerationTable
        by ReadOnlyProperty<OverloadableFactory, GenerationTable<Int>> { _, _ ->
            if (customInstantHealOutcomes == null) {
                factory.instantHealGenerationTable
            } else {
                GenerationTable(customInstantHealOutcomes.map { (w, r) -> TableEntry(w) { r } })
            }
        }
}

private infix fun <T, V> (V?).orElse(getter: () -> V) = ReadOnlyProperty<T, V> { _, _ -> this@orElse ?: getter() }
