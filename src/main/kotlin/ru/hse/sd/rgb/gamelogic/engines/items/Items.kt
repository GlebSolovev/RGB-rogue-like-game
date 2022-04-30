package ru.hse.sd.rgb.gamelogic.engines.items

import java.awt.image.BufferedImage

abstract class Item {

    abstract inner class ViewItem {
        abstract fun getSwingAppearance(): BufferedImage
    }

    abstract val viewItem: ViewItem

    abstract fun use()
    abstract val isReusable: Boolean
}


