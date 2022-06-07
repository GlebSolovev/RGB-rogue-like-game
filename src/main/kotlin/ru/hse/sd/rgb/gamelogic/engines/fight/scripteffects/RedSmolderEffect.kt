package ru.hse.sd.rgb.gamelogic.engines.fight.scripteffects

import ru.hse.sd.rgb.gamelogic.engines.fight.BaseColorUpdateEffect
import ru.hse.sd.rgb.gamelogic.engines.fight.ControlParams
import ru.hse.sd.rgb.gamelogic.engines.fight.FightEngine
import ru.hse.sd.rgb.gamelogic.entities.GameUnit
import ru.hse.sd.rgb.utils.structures.RGBDelta
import ru.hse.sd.rgb.utils.structures.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("red_smolder")
class RedSmolderEffect(private val attack: Int, private val decreaseRed: Int) : BaseColorUpdateEffect {

    override suspend fun activate(
        unit: GameUnit,
        controlParams: ControlParams,
        unsafeMethods: FightEngine.UnsafeMethods
    ) {
        unsafeMethods.unsafeAttackDirectly(unit, attack)
        unsafeMethods.unsafeChangeRGB(unit, unit.gameColor + RGBDelta(-decreaseRed, 0, 0))
    }

}
