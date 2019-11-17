package tk.amplifiable.mcgradle.user.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import tk.amplifiable.binpatches.BinPatches;
import tk.amplifiable.mcgradle.MCGradleConstants;
import tk.amplifiable.mcgradle.user.MCGradleUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class TaskApplyBinPatches extends DefaultTask {
    @Input
    private String version = MCGradleConstants.EXTENSION.version;
    @InputFile
    private File input = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/recompiled.jar", version));
    @InputFile
    private File distribution = MCGradleUser.distributionDependency;
    @OutputFile
    private File output = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/binpatched-%s.jar", version, MCGradleUser.distributionHash));//new File(MCGradleConstants.CACHE_DIRECTORY, String.format("repo/net/minecraft/minecraft/%s-%s/minecraft-%s-%s.jar", version, MCGradleUser.distributionHash, version, MCGradleUser.distributionHash));

    @TaskAction
    private void applyPatches() throws IOException {
        try (ZipFile file = new ZipFile(getDistribution())) {
            try (InputStream inputStream = file.getInputStream(file.getEntry("binpatches.dev.lzma"))) {
                try (FileInputStream extraInputStream = new FileInputStream(getDistribution())) {
                    BinPatches.applyPatches(inputStream, extraInputStream, "merged", getInput(), getOutput());
                }
            }
        }
    }

    public String getVersion() {
        return version;
    }

    public File getInput() {
        return input;
    }

    public File getDistribution() {
        return distribution;
    }

    public File getOutput() {
        return output;
    }
}
