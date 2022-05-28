package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.utils.messaging.*
import ru.hse.sd.rgb.utils.messaging.messages.*
import ru.hse.sd.rgb.utils.unreachable

open class State {

    suspend fun next(message: Message): State = when (message) {
        is ReceivedAttack -> handleReceivedAttack(message)
        is CollidedWith -> handleCollidedWith(message)
        is UserMoved -> handleUserMoved(message)
        is UserToggledInventory -> handleUserToggledInventory()
        is UserSelect -> handleUserSelect()
        is UserDrop -> handleUserDrop()
        is ColorTick -> handleColorTick(message)
        is MoveTick -> handleMoveTick()
        is RepaintTick -> handleRepaintTick()
        is DieTick -> handleDieTick()
        is ContinueTick -> handleContinueTick()
        else -> unreachable("State doesn't have method for this message")
    }

    open suspend fun handleReceivedAttack(message: ReceivedAttack): State = messageNotSupported
    open suspend fun handleCollidedWith(message: CollidedWith): State = messageNotSupported

    open suspend fun handleUserMoved(message: UserMoved): State = messageNotSupported
    open suspend fun handleUserToggledInventory(): State = messageNotSupported
    open suspend fun handleUserSelect(): State = messageNotSupported
    open suspend fun handleUserDrop(): State = messageNotSupported

    open suspend fun handleColorTick(tick: ColorTick): State = messageNotSupported

    open suspend fun handleMoveTick(): State = messageNotSupported
    open suspend fun handleRepaintTick(): State = messageNotSupported
    open suspend fun handleDieTick(): State = messageNotSupported
    open suspend fun handleContinueTick(): State = messageNotSupported

    // TODO: message to change Behaviour, service, stop ticker

    private val messageNotSupported: Nothing
        get() = throw IllegalStateException("message not supported")

}
