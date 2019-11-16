package tk.amplifiable.mcgradle.tasks.mc;

import com.google.common.io.ByteStreams;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import tk.amplifiable.mcgradle.MCGradleConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TaskGenerateExtra extends DefaultTask {
    @InputFile
    private File reobfuscated = new File(getProject().getChildProjects().get("generated").getBuildDir(), "/projectCache/reobfuscated.jar");

    @Input
    private String version = MCGradleConstants.EXTENSION.version;

    @InputFile
    private File original = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/merged.jar", version));

    @OutputFile
    private File output = new File(getProject().getBuildDir(), "distrib/extra.jar");

    @TaskAction
    private void generate() throws IOException {
        ZipFile reobf = new ZipFile(reobfuscated);
        ZipFile original = new ZipFile(this.original);
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(this.output))) {
            List<? extends ZipEntry> entries = reobf.stream().filter(entry -> {
                String name = entry.getName();
                return !(name.equals("tk/amplifiable/mcgradle/Start.class") || name.equals("tk/amplifiable/mcgradle/Properties.class"));
            }).collect(Collectors.toList());
            entries.forEach(entry -> {
                try {
                    if (original.getEntry(entry.getName()) != null) return;
                    if (entry.isDirectory()) {
                        String name = entry.getName().replace('\\', '/');
                        if (entries.stream().anyMatch(e -> e.getName().replace('\\', '/').startsWith(name + "/"))) {{
                            output.putNextEntry(entry);
                        }}
                    } else {
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        newEntry.setTime(entry.getTime());
                        output.putNextEntry(entry);
                        output.write(ByteStreams.toByteArray(reobf.getInputStream(entry)));
                    }
                } catch (IOException ignored) {}
            });
        }
        reobf.close();
        original.close();
    }
}
