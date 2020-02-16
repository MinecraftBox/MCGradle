package club.ampthedev.mcgradle.base.tasks.impl.decomp

import java.io.File

interface Decompiler {
    fun addInput(file: File)

    fun addLibrary(file: File)

    fun start()
}