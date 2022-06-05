package ru.hse.sd.rgb.gamelogic.engines.behaviour

import ru.hse.sd.rgb.gamelogic.entities.GameEntity
import ru.hse.sd.rgb.utils.messaging.Message
import ru.hse.sd.rgb.utils.messaging.Ticker

object BehaviourBuilder {

    private fun pipeBlock(entity: GameEntity, to: Behaviour) = object : BehaviourBuildingBlock(entity, null) {
        override suspend fun handleMessage(message: Message) = to.handleMessage(message)
    }

    interface BlockBuilderContext {
        val entity: GameEntity
        val childBlock: BehaviourBuildingBlock?
    }

    interface MetaFromBlocksBuilder {
        fun add(block: BlockBuilderContext.() -> BehaviourBuildingBlock): MetaFromBlocksBuilder
        fun build(): MetaBehaviour
    }

    fun metaFromBlocks(base: Behaviour) = object : MetaFromBlocksBuilder {
        private var topBuildingBlock: BehaviourBuildingBlock? = pipeBlock(base.entity, base)
        private val context = object : BlockBuilderContext {
            override val entity = base.entity
            override val childBlock get() = topBuildingBlock
        }

        override fun add(block: BlockBuilderContext.() -> BehaviourBuildingBlock): MetaFromBlocksBuilder {
            topBuildingBlock = context.block()
            return this
        }

        override fun build(): MetaBehaviour {
            return object : MetaBehaviour(base.entity, base) {
                override suspend fun handleMessage(message: Message) {
                    topBuildingBlock?.handleMessage(message)
                }

                override fun traverseTickers(onEach: (Ticker) -> Unit) {
                    var currentBlock: BehaviourBuildingBlock? = topBuildingBlock
                    while (currentBlock != null) {
                        val ticker = currentBlock.ticker
                        if (ticker != null) onEach(ticker)
                        currentBlock = currentBlock.childBlock
                    }
                }
            }
        }
    }

    interface BehaviourBuilderContext {
        val entity: GameEntity
        val childBehaviour: Behaviour
    }

    interface InlineBehaviourFromBlocksBuilder {
        fun add(block: BlockBuilderContext.() -> BehaviourBuildingBlock)
    }

    interface LifecycleBuilder {
        fun add(block: BehaviourBuilderContext.() -> Behaviour): LifecycleBuilder
        fun addBlocks(b: InlineBehaviourFromBlocksBuilder.() -> Unit): LifecycleBuilder
        fun build(): Lifecycle
    }

    fun lifecycle(
        entity: GameEntity,
        baseBehaviour: Behaviour = NoneBehaviour(entity)
    ) = object : LifecycleBuilder {
        private var topBehaviour: Behaviour = baseBehaviour
        private val context = object : BehaviourBuilderContext {
            override val entity = entity
            override val childBehaviour get() = topBehaviour
        }

        override fun add(block: BehaviourBuilderContext.() -> Behaviour): LifecycleBuilder {
            topBehaviour = context.block()
            return this
        }

        // TODO: reuse previous builder
        override fun addBlocks(b: InlineBehaviourFromBlocksBuilder.() -> Unit): LifecycleBuilder {
            var inlineTopBlock: BehaviourBuildingBlock? = pipeBlock(topBehaviour.entity, topBehaviour)
            val inlineBuilder = object : InlineBehaviourFromBlocksBuilder {
                private val inlineContext = object : BlockBuilderContext {
                    override val entity = entity
                    override val childBlock get() = inlineTopBlock
                }

                override fun add(block: BlockBuilderContext.() -> BehaviourBuildingBlock) {
                    inlineTopBlock = inlineContext.block()
                }
            }

            inlineBuilder.b()

            topBehaviour = object : MetaBehaviour(entity, topBehaviour) {
                override suspend fun handleMessage(message: Message) {
                    inlineTopBlock?.handleMessage(message)
                }

                override fun traverseTickers(onEach: (Ticker) -> Unit) {
                    var currentBlock: BehaviourBuildingBlock? = inlineTopBlock
                    while (currentBlock != null) {
                        val ticker = currentBlock.ticker
                        if (ticker != null) onEach(ticker)
                        currentBlock = currentBlock.childBlock
                    }
                }
            }
            return this
        }

        override fun build(): Lifecycle {
            return Lifecycle(entity, topBehaviour)
        }
    }

}
