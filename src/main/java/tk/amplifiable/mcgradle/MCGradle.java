package tk.amplifiable.mcgradle;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import tk.amplifiable.mcgradle.tasks.DownloadTask;
import tk.amplifiable.mcgradle.tasks.DownloadVersionJsonTask;
import tk.amplifiable.mcgradle.tasks.TaskSetup;
import tk.amplifiable.mcgradle.tasks.mc.*;
import tk.amplifiable.mcgradle.tasks.properties.TaskGenerateProperties;
import tk.amplifiable.mcgradle.tasks.subprojects.TaskCopySources;
import tk.amplifiable.mcgradle.tasks.subprojects.TaskGenerateProject;
import tk.amplifiable.mcgradle.utils.Utils;

import javax.annotation.Nonnull;
import java.io.File;

public class MCGradle implements Plugin<Project> {
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void apply(@Nonnull Project p) {
        if (!p.equals(p.getRootProject())) return;
        p.getLogger().warn("hey the only correct date formats are DD/MM/YYYY and YYYY-MM-DD");
        MCGradleConstants.setProjectFields(p);
        if (!p.getChildProjects().containsKey("generated")) {
            throw new GradleException("The 'generated' project isn't included.");
        } else {
            MCGradleConstants.LOGGER.info("Generated project is included");
        }
        p.getExtensions().add(Names.EXTENSION, MCGradleConstants.EXTENSION);
        p.getConfigurations().create(Names.MCP_DATA_CONF);
        p.getConfigurations().create(Names.MCP_MAPPINGS_CONF);
        p.getConfigurations().create(Names.MC_DEPENDENCIES_CONF);
        p.afterEvaluate(proj -> {
            proj.getRepositories().mavenCentral();
            proj.getRepositories().maven(repo -> repo.setUrl("https://files.minecraftforge.net/maven"));
            proj.getRepositories().maven(repo -> repo.setUrl("https://libraries.minecraft.net"));

            // TODO: Support both new and old versions. Detection is already done, just need to tweak genmappings and sourcedeobf

            boolean isNewDependencySystem = false;
            if (MCGradleConstants.EXTENSION.version.contains(".")) {
                String versionUsed = MCGradleConstants.EXTENSION.version;
                if (versionUsed.contains("-pre")) {
                    versionUsed = versionUsed.substring(0, versionUsed.indexOf('-'));
                }
                String[] parts = versionUsed.split("\\.");
                int majorVersion = Integer.parseInt(parts[1]);
                isNewDependencySystem = majorVersion >= 12; // 1.12 is available with both systems
            } else {
                String[] parts = MCGradleConstants.EXTENSION.version.split("w");
                String parts12 = parts[1].substring(0, parts[1].length() - 1);
                int year = Integer.parseInt(parts[0]);
                int week = Integer.parseInt(parts12);
                if (year >= 17) {
                    if (year == 17) {
                        isNewDependencySystem = week >= 47;
                    } else {
                        isNewDependencySystem = true;
                    }
                }
            }

            proj.getDependencies().add(Names.MCP_DATA_CONF, ImmutableMap.of(
                    "group", "de.oceanlabs.mcp",
                    "name", "mcp_" + MCGradleConstants.EXTENSION.mappingChannel,
                    "version", MCGradleConstants.EXTENSION.mappingVersion + "-" + MCGradleConstants.EXTENSION.version,
                    "ext", "zip"
            ));

            proj.getDependencies().add(Names.MCP_MAPPINGS_CONF, ImmutableMap.of(
                    "group", "de.oceanlabs.mcp",
                    "name", "mcp",
                    "version", MCGradleConstants.EXTENSION.version,
                    "classifier", "srg",
                    "ext", "zip"
            ));

            Task download = p.getTasks().create(Names.DOWNLOAD_VERSION_JSON, DownloadVersionJsonTask.class);
            download.dependsOn(Names.DOWNLOAD_MANIFEST);
            download.setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_MANIFEST, DownloadTask.class, MCGradleConstants.VERSION_MANIFEST_URL, Utils.getCacheFile(Names.VERSION_MANIFEST), true).setGroup(Names.OTHER_GROUP);
            TaskGenerateProject gen = p.getTasks().create(Names.GENERATE_PROJECT, TaskGenerateProject.class);
            gen.dependsOn(Names.DOWNLOAD_VERSION_JSON, Names.DOWNLOAD_ASSETS, Names.DOWNLOAD_NATIVES);
            gen.setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_NATIVES, TaskDownloadNatives.class).dependsOn(Names.RESOLVE_DEPENDENCIES).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_ASSETS, TaskDownloadAssets.class).dependsOn(Names.DOWNLOAD_VERSION_JSON).setGroup(Names.OTHER_GROUP);

            TaskGenerateMappings genMappings = p.getTasks().create(Names.GEN_MAPPINGS, TaskGenerateMappings.class);
            genMappings.setGroup(Names.OTHER_GROUP);

            p.getTasks().create(Names.DOWNLOAD_CLIENT_JAR, TaskDownloadClient.class).dependsOn(Names.DOWNLOAD_VERSION_JSON).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.DOWNLOAD_SERVER_JAR, TaskDownloadServer.class).dependsOn(Names.DOWNLOAD_VERSION_JSON).setGroup(Names.OTHER_GROUP);

            Task downloadJars = p.getTasks().create(Names.DOWNLOAD_JARS);
            downloadJars.dependsOn(Names.DOWNLOAD_CLIENT_JAR, Names.DOWNLOAD_SERVER_JAR);
            downloadJars.setGroup(Names.OTHER_GROUP);

            p.getTasks().create(Names.DEOBF, TaskDeobf.class).dependsOn(Names.MERGE, Names.GEN_MAPPINGS).setGroup(Names.OTHER_GROUP);

            TaskDecomp decomp = p.getTasks().create(Names.DECOMP, TaskDecomp.class);
            decomp.dependsOn(Names.DEOBF, Names.RESOLVE_DEPENDENCIES);
            decomp.setGroup(Names.OTHER_GROUP);

            p.getTasks().create(Names.SPLIT_SERVER, TaskSplitServerAndDeps.class).dependsOn(Names.DOWNLOAD_SERVER_JAR).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.MERGE, TaskMerge.class).dependsOn(Names.DOWNLOAD_JARS, Names.SPLIT_SERVER).setGroup(Names.OTHER_GROUP);

            Task t = p.getTasks().create(Names.SETUP, TaskSetup.class);
            t.dependsOn(Names.GENERATE_PROJECT, Names.GENERATE_PROPERTIES, Names.CUSTOM_PATCHES);
            t.setGroup(Names.MAIN_GROUP);

            TaskGenerateProperties generateProperties = p.getTasks().create(Names.GENERATE_PROPERTIES, TaskGenerateProperties.class);
            generateProperties.properties = MCGradleConstants.EXTENSION.properties;
            generateProperties.setGroup(Names.OTHER_GROUP);

            TaskExtractSources extractSources = p.getTasks().create(Names.EXTRACT_SOURCES, TaskExtractSources.class);
            extractSources.setGroup(Names.OTHER_GROUP);

            TaskApplyPatches applyMcpPatches = p.getTasks().create(Names.MCP_PATCHES, TaskApplyPatches.class);
            applyMcpPatches.setSources(extractSources.getOutput());
            applyMcpPatches.setPatches(new File(genMappings.getOutputDirectory(), "patches/minecraft_merged_ff"));
            applyMcpPatches.setOutput(new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + MCGradleConstants.EXTENSION.version + "/patched/mcp"));
            applyMcpPatches.setGroup(Names.OTHER_GROUP);
            applyMcpPatches.dependsOn(Names.EXTRACT_SOURCES, Names.DECOMP);

            TaskApplyPatches applyCustomPatches = p.getTasks().create(Names.CUSTOM_PATCHES, TaskApplyPatches.class);
            applyCustomPatches.setSources(new File(MCGradleConstants.ROOT_PROJECT_DIR, "generated/src/main/java"));
            applyCustomPatches.setPatches(new File(MCGradleConstants.ROOT_PROJECT_DIR, "patches"));
            applyCustomPatches.setOutput(new File(MCGradleConstants.ROOT_PROJECT_DIR,  "generated/src/main/java"));
            applyCustomPatches.setGroup(Names.OTHER_GROUP);
            applyCustomPatches.dependsOn(Names.COPY, Names.GENERATE_PROJECT);

            MCGradleConstants.prepareDirectory(applyCustomPatches.getPatches());

            TaskGeneratePatches genPatches = p.getTasks().create(Names.GENERATE_PATCHES, TaskGeneratePatches.class);
            genPatches.dependsOn(Names.SRC_DEOBF);
            genPatches.setGroup(Names.MAIN_GROUP);

            p.getTasks().create(Names.SRC_DEOBF, TaskSourceDeobf.class).dependsOn(Names.MCP_PATCHES).setGroup(Names.OTHER_GROUP);
            p.getTasks().create(Names.COPY, TaskCopySources.class).dependsOn(Names.SRC_DEOBF, Names.GENERATE_PROJECT).setGroup(Names.OTHER_GROUP);

            registerBuildTasks(p);
        });
    }

    private void registerBuildTasks(Project p) {
        p.getTasks().create(Names.RESOLVE_DEPENDENCIES, TaskResolveDependencies.class).setGroup(Names.OTHER_GROUP);

        p.getTasks().create(Names.RUN_JAR_TASK, TaskRunJarTask.class).setGroup(Names.OTHER_GROUP);

        TaskRecompileClean recomp = p.getTasks().create(Names.RECOMPILE, TaskRecompileClean.class);
        recomp.dependsOn(Names.RESOLVE_DEPENDENCIES);
        //recomp.dependsOn(Names.SRC_DEOBF, Names.RESOLVE_DEPENDENCIES).setGroup(Names.OTHER_GROUP);

        p.getTasks().create(Names.REOBFUSCATE_JAR, TaskReobf.class).dependsOn(Names.DEOBF, Names.RUN_JAR_TASK, Names.RESOLVE_DEPENDENCIES, Names.GEN_MAPPINGS).setGroup(Names.OTHER_GROUP);

        p.getTasks().create(Names.GEN_BIN_PATCHES, TaskGenerateBinPatches.class).dependsOn(Names.GENERATE_PATCHES, Names.RECOMPILE, Names.DEOBF, Names.REOBFUSCATE_JAR).setGroup(Names.OTHER_GROUP);

        TaskReobf task = p.getTasks().create(Names.REOBFUSCATE_CLEAN, TaskReobf.class);
        task.setInput(recomp.getOutputJar());
        task.setOutput(new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/reobf.jar", MCGradleConstants.EXTENSION.version)));
        task.dependsOn(Names.DEOBF, Names.RECOMPILE, Names.RESOLVE_DEPENDENCIES, Names.GEN_MAPPINGS).setGroup(Names.OTHER_GROUP);

        p.getTasks().create(Names.GENERATE_EXTRA, TaskGenerateExtra.class).dependsOn(Names.GEN_BIN_PATCHES, Names.REOBFUSCATE_JAR, Names.MERGE).setGroup(Names.OTHER_GROUP);

        p.getTasks().create("distrib").dependsOn(Names.GENERATE_EXTRA, Names.GEN_BIN_PATCHES).setGroup("build");
    }
}
