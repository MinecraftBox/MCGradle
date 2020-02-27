package club.ampthedev.mcgradle.user.utils

import club.ampthedev.mcgradle.base.utils.VERSION_DIRECTORY

const val MOD_JAR = "@MOD_JAR@"
const val MOD_HASH = "@MOD_HASH@"
const val REPO = "@REPO@"

val MCBIN_DEP = mapOf("group" to "net.minecraft", "name" to "minecraft", "version" to "mcbin")
val MCSRC_DEP = mapOf("group" to "net.minecraft", "name" to "minecraft", "version" to "mc")

private const val MCBIN_PATH = "net/minecraft/minecraft/mcbin/minecraft-mcbin.jar"
private const val MCSRC_BIN_PATH = "net/minecraft/minecraft/mc/minecraft-mc.jar"
private const val MCSRC_PATH = "net/minecraft/minecraft/mc/minecraft-mc-sources.jar"

const val USER_VERSION_DIR = "$VERSION_DIRECTORY/U-$MOD_HASH"
const val PATCHED_CLIENT = "$USER_VERSION_DIR/client.patched.jar"
const val PATCHED_SERVER = "$USER_VERSION_DIR/server.patched.jar"
const val PATCHED_MERGED = "$USER_VERSION_DIR/merged.patched.jar"
const val PATCHED_INJECTED = "$USER_VERSION_DIR/injected.patched.jar"
const val INJECTED = "$USER_VERSION_DIR/injected.jar"
const val INJECTED_DEOBF = "$USER_VERSION_DIR/deobf.jar"
const val INJECTED_DECOMP = "$USER_VERSION_DIR/decomp.jar"
const val INJECTED_PATCHED = "$USER_VERSION_DIR/patched.jar"
const val INJECTED_SOURCE_MAPPED = "$USER_VERSION_DIR/sourceMapped.jar"
const val INJECTED_RECOMPILED = "$USER_VERSION_DIR/recompiled.jar"
const val VANILLA_REPO = "$VERSION_DIRECTORY/repo"
const val MOD_REPO = "$USER_VERSION_DIR/repo"

const val DEOBF_MCP = "$REPO/$MCBIN_PATH"
const val DECOMP_BIN = "$REPO/$MCSRC_BIN_PATH"
const val DECOMP_SRC = "$REPO/$MCSRC_PATH"

const val MCP_DEOBF = "deobfMCP"
const val CONFIGURATION_MOD = "minecraft"
const val MERGE_PATCHED_JARS = "mergeJarsPatched"
const val BINPATCH_CLIENT = "applyClientPatches"
const val BINPATCH_SERVER = "applyServerPatches"
const val BINPATCH_MERGED = "applyBinaryPatches"
const val INJECT_CLASSES = "injectModPatched"
const val INJECT_CLASSES_2 = "injectMod"
const val GEN_START = "genStart"
const val GEN_CLIENT_RUN = "genClientRun"
const val GEN_SERVER_RUN = "genServerRun"
const val GEN_RUNS = "genRunConfigs"
const val SETUP_CI = "setupCIWorkspace"
const val SETUP_DEV = "setupDevWorkspace"
const val SETUP_DECOMP = "setupDecompWorkspace"
const val COPY_SOURCE = "copySourceJar"
const val REOBF_JAR = "reobfJar"