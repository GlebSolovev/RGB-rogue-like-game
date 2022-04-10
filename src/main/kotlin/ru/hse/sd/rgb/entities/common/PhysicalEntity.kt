package ru.hse.sd.rgb.entities.common

abstract class PhysicalEntity() {
    abstract val isSolid: Boolean
}

// ssssssssssnake
//abstract class ComplexPhysicalEntity(parentEntity: GameEntity) : PhysicalEntity(parentEntity) {
//    private val parts = mutableListOf<PhysicalEntity>()
//}
