package tk.amplifiable.mcgradle.tasks.mc;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.gradle.api.AntBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import tk.amplifiable.mcgradle.MCGradleConstants;
import tk.amplifiable.mcgradle.Names;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class TaskRecompileClean extends DefaultTask {
    @Input
    private String version = MCGradleConstants.EXTENSION.version;

    @InputFiles
    private File sources = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/sourceMapped");

    @OutputFile
    private File outputJar = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/recompiled.jar");

    @InputFiles
    private FileCollection classpath = getProject().getConfigurations().getByName(Names.MC_DEPENDENCIES_CONF);

    @TaskAction
    private void recompile() throws IOException {
        File tempSource = new File(getTemporaryDir(), "sources");
        File tempClasses = new File(getTemporaryDir(), "compiled");
        getProject().delete(tempClasses, tempSource);
        tempSource.mkdirs();
        tempClasses.mkdirs();
        getProject().fileTree(sources).visit(new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails dirDetails) {

            }

            @Override
            public void visitFile(FileVisitDetails fileDetails) {
                String name = fileDetails.getName();
                String path = fileDetails.getPath();
                if (path.startsWith("META-INF/") || name.startsWith("Log4j-") || name.endsWith(".der"))
                    return;
                File outputFile = new File(tempSource, path);
                File parent = outputFile.getParentFile();
                if (parent != null) MCGradleConstants.prepareDirectory(parent);
                fileDetails.copyTo(outputFile);
            }
        });
        AntBuilder ant = getAnt();
        getExtPath();
        ant.invokeMethod("javac",
                ImmutableMap.builder()
                        .put("srcDir", tempSource.getCanonicalPath())
                        .put("destDir", tempClasses.getCanonicalPath())
                        .put("failonerror", true)
                        .put("includeantruntime", false)
                        .put("classpath", classpath.getAsPath())
                        .put("encoding", "utf-8")
                        .put("source", "1.8")
                        .put("target", "1.8")
                        .put("debug", true)
                        .build());
        outputJar.getParentFile().mkdirs();
        createOutput(outputJar, tempClasses, tempSource);
    }

    private static String getExtPath() {
        String currentExtDirs = System.getProperty("java.ext.dirs");
        StringBuilder newExtDirs = new StringBuilder();
        String[] parts = currentExtDirs.split(File.pathSeparator);
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            for (String part : parts) {
                if (!part.equals("/System/Library/Java/Extensions")) {
                    newExtDirs.append(part);
                    if (!part.equals(lastPart)) {
                        newExtDirs.append(File.pathSeparator);
                    }
                }
            }
        }
        System.setProperty("java.ext.dirs", newExtDirs.toString());
        return newExtDirs.toString();
    }

    private static void extractSources(File tempDir, File inJar) throws IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(inJar));
        ZipEntry entry;

        while ((entry = zin.getNextEntry()) != null) {
            if (entry.isDirectory() || !entry.getName().endsWith(".java"))
                continue;

            File out = new File(tempDir, entry.getName());
            out.getParentFile().mkdirs();
            Files.asByteSink(out).writeFrom(zin);
        }

        zin.close();
    }

    private void createOutput(File outJar, File classDir, File sourceDir) throws IOException {
        Set<String> elementsAdded = Sets.newHashSet();

        // make output
        JarOutputStream zout = new JarOutputStream(new FileOutputStream(outJar));

        Visitor visitor = new Visitor(zout, elementsAdded);

        getProject().fileTree(sourceDir).visit(visitor);
        getProject().fileTree(classDir).visit(visitor); // then the classes

        zout.close();
    }

    private static final class Visitor implements FileVisitor {
        private final ZipOutputStream zout;
        private final Set<String> entries;

        public Visitor(ZipOutputStream zout, Set<String> entries) {
            this.zout = zout;
            this.entries = entries;
        }

        @Override
        public void visitDir(FileVisitDetails dir) {
            try {
                String name = dir.getRelativePath().toString().replace('\\', '/');
                if (!name.endsWith("/"))
                    name += "/";

                if (entries.contains(name))
                    return;

                entries.add(name);
                ZipEntry entry = new ZipEntry(name);
                zout.putNextEntry(entry);
            } catch (IOException e) {
                Throwables.throwIfUnchecked(e);
            }
        }

        @Override
        public void visitFile(FileVisitDetails file) {
            try {
                String name = file.getRelativePath().toString().replace('\\', '/');

                if (entries.contains(name) || name.endsWith(".java"))
                    return;

                entries.add(name);
                zout.putNextEntry(new ZipEntry(name));

                file.copyTo(zout);
            } catch (IOException e) {
                Throwables.throwIfUnchecked(e);
            }
        }
    }

    @Input
    public String getVersion() {
        return version;
    }

    @InputFiles
    public File getSources() {
        return sources;
    }

    @OutputFile
    public File getOutputJar() {
        return outputJar;
    }

    @InputFiles
    public FileCollection getClasspath() {
        return classpath;
    }
}
