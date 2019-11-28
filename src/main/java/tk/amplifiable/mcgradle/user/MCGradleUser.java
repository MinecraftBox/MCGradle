package tk.amplifiable.mcgradle.user;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import tk.amplifiable.mcgradle.MCGradleConstants;
import tk.amplifiable.mcgradle.Names;
import tk.amplifiable.mcgradle.mc.DependencyUtilities;
import tk.amplifiable.mcgradle.tasks.DownloadTask;
import tk.amplifiable.mcgradle.tasks.DownloadVersionJsonTask;
import tk.amplifiable.mcgradle.tasks.mc.TaskApplyPatches;
import tk.amplifiable.mcgradle.tasks.mc.TaskDecomp;
import tk.amplifiable.mcgradle.tasks.mc.TaskDeobf;
import tk.amplifiable.mcgradle.tasks.mc.TaskDownloadAssets;
import tk.amplifiable.mcgradle.tasks.mc.TaskDownloadClient;
import tk.amplifiable.mcgradle.tasks.mc.TaskDownloadNatives;
import tk.amplifiable.mcgradle.tasks.mc.TaskDownloadServer;
import tk.amplifiable.mcgradle.tasks.mc.TaskExtractSources;
import tk.amplifiable.mcgradle.tasks.mc.TaskGenerateMappings;
import tk.amplifiable.mcgradle.tasks.mc.TaskMerge;
import tk.amplifiable.mcgradle.tasks.mc.TaskRecompileClean;
import tk.amplifiable.mcgradle.tasks.mc.TaskResolveDependencies;
import tk.amplifiable.mcgradle.tasks.mc.TaskSourceDeobf;
import tk.amplifiable.mcgradle.tasks.mc.TaskSplitServerAndDeps;
import tk.amplifiable.mcgradle.tasks.properties.TaskGenerateProperties;
import tk.amplifiable.mcgradle.user.tasks.TaskApplyBinPatches;
import tk.amplifiable.mcgradle.utils.Utils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class MCGradleUser implements Plugin<Project> {
    public static File distributionDependency = null;
    public static String distributionHash = null;

    @Override
    public void apply(@Nonnull Project target) {
        MCGradleConstants.setProjectFields(target);
        target.getExtensions().add(Names.EXTENSION, MCGradleConstants.EXTENSION);
        target.getConfigurations().create(MCGradleUserConstants.CONFIGURATION_DISTRIB);
        target.getConfigurations().create(Names.MC_DEPENDENCIES_CONF);
        target.getConfigurations().create(Names.MCP_DATA_CONF);
        target.getConfigurations().create(Names.MCP_MAPPINGS_CONF);

        target.afterEvaluate(p -> {
            distributionDependency = null;
            for (File f : p.getConfigurations().getByName(MCGradleUserConstants.CONFIGURATION_DISTRIB)) {
                if (distributionDependency != null) {
                    throw new GradleException("You can only use one Minecraft distribution");
                }
                distributionDependency = f;
            }
            if (distributionDependency == null) throw new GradleException("You must specify the Minecraft distribution");
            try {
                distributionHash = Utils.sha256(distributionDependency);
            } catch (IOException e) {
                throw new GradleException("Failed to hash distribution", e);
            }
            p.getRepositories().mavenCentral();
            p.getRepositories().maven(repo -> repo.setUrl("https://files.minecraftforge.net/maven"));
            p.getRepositories().maven(repo -> repo.setUrl("https://libraries.minecraft.net"));

            p.getDependencies().add(Names.MCP_DATA_CONF, ImmutableMap.of(
                    "group", "de.oceanlabs.mcp",
                    "name", "mcp_" + MCGradleConstants.EXTENSION.mappingChannel,
                    "version", MCGradleConstants.EXTENSION.mappingVersion + "-" + MCGradleConstants.EXTENSION.version,
                    "ext", "zip"
            ));

            p.getDependencies().add(Names.MCP_MAPPINGS_CONF, ImmutableMap.of(
                    "group", "de.oceanlabs.mcp",
                    "name", "mcp",
                    "version", MCGradleConstants.EXTENSION.version,
                    "classifier", "srg",
                    "ext", "zip"
            ));

            p.getTasks().create(Names.RESOLVE_DEPENDENCIES, TaskResolveDependencies.class)
                    .dependsOn(Names.DOWNLOAD_VERSION_JSON);

            p.getTasks().create(Names.DOWNLOAD_VERSION_JSON, DownloadVersionJsonTask.class)
                    .dependsOn(Names.DOWNLOAD_MANIFEST);

            p.getTasks().create(Names.DOWNLOAD_MANIFEST, DownloadTask.class, MCGradleConstants.VERSION_MANIFEST_URL, Utils.getCacheFile(Names.VERSION_MANIFEST), true);

            TaskApplyBinPatches applyPatches = p.getTasks().create(MCGradleUserConstants.APPLY_BIN_PATCHES, TaskApplyBinPatches.class);
            applyPatches.dependsOn(Names.RECOMPILE).setGroup(Names.OTHER_GROUP);

            TaskDeobf deobf = p.getTasks().create(Names.DEOBF, TaskDeobf.class);
            deobf.dependsOn(Names.MERGE, Names.GEN_MAPPINGS).setGroup(Names.OTHER_GROUP);

            TaskGenerateMappings genMappings = p.getTasks().create(Names.GEN_MAPPINGS, TaskGenerateMappings.class);
            genMappings.setGroup(Names.OTHER_GROUP);

            p.getTasks().create(Names.MERGE, TaskMerge.class).dependsOn(Names.DOWNLOAD_JARS, Names.SPLIT_SERVER).setGroup(Names.OTHER_GROUP);

            p.getTasks().create(Names.DOWNLOAD_CLIENT_JAR, TaskDownloadClient.class).dependsOn(Names.DOWNLOAD_VERSION_JSON).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_SERVER_JAR, TaskDownloadServer.class).dependsOn(Names.DOWNLOAD_VERSION_JSON).setGroup(Names.OTHER_GROUP);

            Task downloadJars = p.getTasks().create(Names.DOWNLOAD_JARS);
            downloadJars.dependsOn(Names.DOWNLOAD_CLIENT_JAR, Names.DOWNLOAD_SERVER_JAR);
            downloadJars.setGroup(Names.OTHER_GROUP);

            p.getTasks().create(Names.SPLIT_SERVER, TaskSplitServerAndDeps.class).dependsOn(Names.DOWNLOAD_SERVER_JAR).setGroup(Names.OTHER_GROUP);

            TaskDecomp decomp = p.getTasks().create(Names.DECOMP, TaskDecomp.class);
            decomp.dependsOn(Names.DEOBF, Names.RESOLVE_DEPENDENCIES);
            decomp.setGroup(Names.OTHER_GROUP);

            TaskExtractSources extractSources = p.getTasks().create(Names.EXTRACT_SOURCES, TaskExtractSources.class);
            extractSources.setGroup(Names.OTHER_GROUP);

            TaskApplyPatches applyMcpPatches = p.getTasks().create(Names.MCP_PATCHES, TaskApplyPatches.class);
            applyMcpPatches.setSources(extractSources.getOutput());
            applyMcpPatches.setPatches(new File(genMappings.getOutputDirectory(), "patches/minecraft_merged_ff"));
            applyMcpPatches.setOutput(new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + MCGradleConstants.EXTENSION.version + "/patched/mcp"));
            applyMcpPatches.setGroup(Names.OTHER_GROUP);
            applyMcpPatches.dependsOn(Names.EXTRACT_SOURCES, Names.DECOMP);

            p.getTasks().create(Names.SRC_DEOBF, TaskSourceDeobf.class).dependsOn(Names.MCP_PATCHES).setGroup(Names.OTHER_GROUP);

            TaskRecompileClean recomp = p.getTasks().create(Names.RECOMPILE, TaskRecompileClean.class);
            recomp.dependsOn("copyStart", Names.GENERATE_PROPERTIES, Names.RESOLVE_DEPENDENCIES).setGroup(Names.OTHER_GROUP);

            Task copyTask = p.getTasks().create("copyJar", CopyJarTask.class);
            copyTask.dependsOn(MCGradleUserConstants.APPLY_BIN_PATCHES);
            copyTask.setGroup("mcgradle-other");

            p.getRepositories().maven(repo -> repo.setUrl(new File(MCGradleConstants.CACHE_DIRECTORY, "repo").toURI()));
            p.getDependencies().add("compile", ImmutableMap.of(
                    "group", "net.minecraft",
                    "name", "minecraft",
                    "version", MCGradleConstants.EXTENSION.version + "-" + distributionHash
            ));

            TaskGenerateProperties generateProperties = p.getTasks().create(Names.GENERATE_PROPERTIES, TaskGenerateProperties.class);
            generateProperties.properties = MCGradleConstants.EXTENSION.properties;
            generateProperties.dependsOn(Names.SRC_DEOBF);
            generateProperties.setGroup(Names.OTHER_GROUP);
            generateProperties.out = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/sourceMapped/tk/amplifiable/mcgradle/Properties.java", MCGradleConstants.EXTENSION.version));

            {

            }

            p.getTasks().create("copyStart", CopyStartTask.class).dependsOn(Names.SRC_DEOBF).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_NATIVES, TaskDownloadNatives.class).dependsOn(Names.RESOLVE_DEPENDENCIES).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_ASSETS, TaskDownloadAssets.class).dependsOn(Names.DOWNLOAD_VERSION_JSON).setGroup(Names.OTHER_GROUP);
            if (new File(MCGradleConstants.CACHE_DIRECTORY, String.format(Names.VERSION_JSON, MCGradleConstants.EXTENSION.version)).exists()) {
                JsonObject json = null;
                try {
                    json = Utils.readJsonObj(new File(MCGradleConstants.CACHE_DIRECTORY, String.format(Names.VERSION_JSON, MCGradleConstants.EXTENSION.version)));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (JsonElement element : Objects.requireNonNull(json).getAsJsonArray("libraries")) {
                    JsonObject obj = element.getAsJsonObject();
                    if (DependencyUtilities.shouldInclude(obj)) {
                        p.getDependencies().add("compile", DependencyUtilities.getDependencyString(obj));
                    }
                }
            }

            p.getTasks().create("setup").dependsOn("copyJar").setGroup("mcgradle");
        });
    }

    public static class CopyStartTask extends DefaultTask {
        @OutputFile
        private File output = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/sourceMapped/tk/amplifiable/mcgradle/Start.java", MCGradleConstants.EXTENSION.version));
        @Input
        private Map<String, String> options = Maps.newHashMap();

        public CopyStartTask() {
            File runDir = new File(getProject().getRootDir(), MCGradleConstants.EXTENSION.runDirectory);
            options.put("runDirectory", runDir.getAbsolutePath());
            options.put("nativeDirectory", new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + MCGradleConstants.EXTENSION.version + "/natives").getAbsolutePath());
            options.put("clientMainClass", MCGradleConstants.EXTENSION.clientMainClass);
            options.put("serverMainClass", MCGradleConstants.EXTENSION.serverMainClass);
            options.put("assetsDirectory", new File(MCGradleConstants.CACHE_DIRECTORY, "assets").getAbsolutePath());
            JsonObject versionJson;
            try {
                versionJson = Utils.readVersionJson(MCGradleConstants.EXTENSION.version);
            } catch (IOException e) {
                throw new GradleException("Failed to read version JSON", e);
            }
            options.put("assetIndex", versionJson.getAsJsonObject("assetIndex").get("id").getAsString());
        }

        @TaskAction
        private void copy() throws IOException {
            MCGradleConstants.prepareDirectory(output.getParentFile());
            String source = IOUtils.toString(getClass().getResourceAsStream("/sources/Start.java"));
            for (Map.Entry<String, String> entry : options.entrySet()) {
                source = source.replace("${" + entry.getKey() + "}", Utils.escape(entry.getValue()));
            }
            try (FileWriter outputStream = new FileWriter(output)) {
                outputStream.write(source);
            }
        }
    }

    public static class CopyJarTask extends DefaultTask {
        @Input
        private String version = MCGradleConstants.EXTENSION.version;

        @InputFile
        private File input = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/binpatched-" + distributionHash + ".jar");

        @OutputFile
        private File output = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("repo/net/minecraft/minecraft/%s-%s/minecraft-%s-%s.jar", version, MCGradleUser.distributionHash, version, MCGradleUser.distributionHash));

        @TaskAction
        private void copy() throws IOException {
            FileUtils.copyFile(input, output);
        }

        public String getVersion() {
            return version;
        }

        public File getInput() {
            return input;
        }

        public File getOutput() {
            return output;
        }
    }
}
