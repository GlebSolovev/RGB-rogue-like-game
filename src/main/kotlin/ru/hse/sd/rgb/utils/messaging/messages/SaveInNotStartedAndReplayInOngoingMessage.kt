package ru.hse.sd.rgb.utils.messaging.messages

import ru.hse.sd.rgb.utils.messaging.Message

// message that inherits this class
// will be saved during NOT_STARTED lifecycle state and replayed afterwards during ONGOING
open class SaveInNotStartedAndReplayInOngoingMessage : Message()
