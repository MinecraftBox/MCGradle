package club.ampthedev.mcgradle.tasks.mc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.gradle.api.DefaultTask;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import club.ampthedev.mcgradle.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class TaskRunJarTask extends DefaultTask {
    private File projectDir = getProject().getChildProjects().get("generated").getProjectDir();
    private String callLine = "jar";
    private Map<String, Object> replacements = Maps.newHashMap();

    public TaskRunJarTask() {
        replacements.put("recompDir", new File(getProject().getChildProjects().get("generated").getBuildDir(), "/projectCache"));
        replacements.put("jarName", "recompiled.jar");
    }

    @TaskAction
    public void runJarTask() throws IOException {
        File file = new File(getTemporaryDir(), "initscript0");
        String content = Resources.toString(TaskRunJarTask.class.getResource("/jar_init_script.gradle"), StandardCharsets.UTF_8);
        for (Map.Entry<String, Object> entry : replacements.entrySet()) {
            content = content.replace("${" + entry.getKey() + "}", Utils.escape(entry.getValue().toString()));
        }
        Files.asCharSink(file, StandardCharsets.UTF_8).write(content);
        Gradle gradle = getProject().getGradle();

        ProjectConnection connection = GradleConnector.newConnector()
                .useGradleUserHomeDir(gradle.getGradleUserHomeDir())
                .useInstallation(gradle.getGradleHomeDir())
                .forProjectDirectory(projectDir)
                .connect();

        List<String> args = Lists.newArrayList();
        args.add(callLine);
        args.add("-I" + file.getCanonicalPath());
        getLogger().lifecycle("Running build in subproject");
        connection.newBuild()
                .setStandardOutput(System.out)
                .setStandardInput(System.in)
                .setStandardError(System.err)
                .withArguments(args.toArray(new String[0]))
                .setColorOutput(false)
                .run();
        getLogger().lifecycle("Subproject build completed");
    }
}
