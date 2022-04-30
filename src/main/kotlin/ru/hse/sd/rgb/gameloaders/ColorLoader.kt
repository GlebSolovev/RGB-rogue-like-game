package ru.hse.sd.rgb.gameloaders

import java.io.File

// TODO
class ColorLoader(colorsFilename: String?) {
    private val colorsFile = colorsFilename?.let { File(colorsFilename) }
}
