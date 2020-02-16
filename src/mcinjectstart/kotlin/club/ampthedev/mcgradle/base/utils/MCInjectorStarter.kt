package club.ampthedev.mcgradle.base.utils

import de.oceanlabs.mcp.mcinjector.MCInjector
import de.oceanlabs.mcp.mcinjector.lvt.LVTNaming
import java.io.File
import java.io.PrintStream

object MCInjectorStarter {
    @JvmStatic
    fun start(input: File, output: File, exc: File, acc: File, ctr: File) {
        MCInjector(
            input.toPath(),
            output.toPath()
        ).lvt(LVTNaming.LVT)
            .exceptions(exc.toPath())
            .access(acc.toPath())
            .constructors(ctr.toPath()).process()
    }
}