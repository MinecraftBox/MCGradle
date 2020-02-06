package club.ampthedev.mcgradle.patch

import club.ampthedev.mcgradle.base.BasePlugin

class MCGradlePatch : BasePlugin<PatchExtension>() {
    override val extension = PatchExtension()
}