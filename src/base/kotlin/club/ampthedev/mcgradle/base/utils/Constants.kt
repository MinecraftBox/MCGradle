package club.ampthedev.mcgradle.base.utils

const val EXTENSION_NAME = "mcgradle"

const val GROUP_MAIN = "mcgradle"
const val GROUP_OTHER = "mcgradle-other"

const val CONFIGURATION_MC_DEPS = "mcgradle_mc_deps"
const val CONFIGURATION_MCP_DATA = "mcgradle_mcp_data"
const val CONFIGURATION_MCP_MAPS = "mcgradle_mcp_maps"

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:70.0) Gecko/20100101 Firefox/70.0"

// Task names
const val DOWNLOAD_CLIENT = "downloadClient"
const val DOWNLOAD_SERVER = "downloadServer"
const val SPLIT_SERVER = "splitServer"
const val MERGE_JARS = "mergeJars"
const val GENERATE_MAPPINGS = "generateMappings"
const val DEOBF_JAR = "deobfJar"
const val DECOMP = "decompile"

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
const val CLIENT_JAR = "$VERSION_DIRECTORY/client.jar"
const val SERVER_JAR = "$VERSION_DIRECTORY/server.jar"
const val SPLIT_SERVER_JAR = "$VERSION_DIRECTORY/split_server.jar"
const val MERGED_JAR = "$VERSION_DIRECTORY/merged.jar"
const val MAPPINGS_DIRECTORY = "$VERSION_DIRECTORY/mappings"
const val JOINED_SRG = "$MAPPINGS_DIRECTORY/joined.srg"
const val JOINED_EXC = "$MAPPINGS_DIRECTORY/joined.exc"
const val METHODS_CSV = "$MAPPINGS_DIRECTORY/methods.csv"
const val FIELDS_CSV = "$MAPPINGS_DIRECTORY/fields.csv"
const val SRG_DIRECTORY = "$MAPPINGS_DIRECTORY/srgs"
const val NOTCH_SRG = "$SRG_DIRECTORY/notch-srg.srg"
const val NOTCH_MCP = "$SRG_DIRECTORY/notch-mcp.srg"
const val SRG_MCP = "$SRG_DIRECTORY/srg-mcp.srg"
const val MCP_SRG = "$SRG_DIRECTORY/mcp-srg.srg"
const val MCP_NOTCH = "$SRG_DIRECTORY/mcp-notch.srg"
const val SRG_EXC = "$SRG_DIRECTORY/srg.exc"
const val MCP_EXC = "$SRG_DIRECTORY/mcp.exc"
const val EXCEPTOR_JSON = "$MAPPINGS_DIRECTORY/exceptor.json"
const val TRANSFORMED_EXCEPTOR_JSON = "$VERSION_DIRECTORY/transformed.json"
const val DEOBFED_JAR = "$VERSION_DIRECTORY/deobf.jar"
const val DEOBF_TEMP_JAR = "$VERSION_DIRECTORY/deobf.temp.jar"
const val DECOMP_JAR = "$VERSION_DIRECTORY/decomp.jar"
const val DECOMP_TEMP = "$VERSION_DIRECTORY/decompiler"

// URLs
const val VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json"