package club.ampthedev.mcgradle.user.utils

import club.ampthedev.mcgradle.base.utils.hasReplacement
import org.gradle.api.Project

val Project.vanilla: Boolean
    get() = !hasReplacement(MOD_JAR) || !hasReplacement(MOD_HASH)