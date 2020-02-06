package club.ampthedev.mcgradle.base.utils

const val EXTENSION_NAME = "mcgradle"

const val GROUP_MAIN = "mcgradle"
const val GROUP_OTHER = "mcgradle-other"

const val CONFIGURATION_MC_DEPS = "mcgradle_mc_deps"

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:70.0) Gecko/20100101 Firefox/70.0"

// Replacements
const val CACHE_DIR = "@CACHE_DIR@"
const val PROJECT_DIR = "@PROJECT_DIR@"
const val MC_VERSION = "@MC_VERSION@"
const val MAPPING_CHANNEL = "@MAPPING_CHANNEL@"
const val MAPPING_VERSION = "@MAPPING_VERSION@"
const val RUN_DIRECTORY = "@RUN_DIRECTORY@"
const val BUILD_DIR = "@BUILD_DIR@"

// Paths
const val VERSIONS_DIRECTORY = "$CACHE_DIR/versions"
const val MANIFESTS_DIRECTORY = "$VERSIONS_DIRECTORY/manifests"
const val VERSION_MANIFEST_LOCATION = "$MANIFESTS_DIRECTORY/manifest.json"
const val VERSION_DATA_LOCATION = "$MANIFESTS_DIRECTORY/MC_$MC_VERSION.json"
const val VERSION_DIRECTORY = "$VERSIONS_DIRECTORY/$MC_VERSION-${MAPPING_CHANNEL}_$MAPPING_VERSION"

// URLs
const val VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"