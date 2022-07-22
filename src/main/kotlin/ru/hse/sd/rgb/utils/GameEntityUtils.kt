package ru.hse.sd.rgb.utils

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import kotlin.random.Random

fun GameEntity.randomCell(random: Random = Random) = this.units.random(random).cell
