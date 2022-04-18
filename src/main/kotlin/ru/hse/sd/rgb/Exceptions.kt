package ru.hse.sd.rgb

open class GameError(msg: String, cause: Throwable? = null) : Error(msg, cause)

class WrongConfigError(msg: String) : GameError(msg)
