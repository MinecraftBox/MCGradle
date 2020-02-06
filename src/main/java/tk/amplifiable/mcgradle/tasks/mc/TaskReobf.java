package club.ampthedev.mcgradle.tasks.mc;

import com.google.common.io.Files;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JarProvider;
import net.md_5.specialsource.provider.JointProvider;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.bundling.Jar;
import club.ampthedev.mcgradle.MCGradleConstants;
import club.ampthedev.mcgradle.Names;
import club.ampthedev.mcgradle.utils.ReobfExceptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Set;

public class TaskReobf extends DefaultTask {
    @InputFile
    private File input = new File(getProject().getChildProjects().get("generated").getBuildDir(), "/projectCache/recompiled.jar");
    @Input
    private String version = MCGradleConstants.EXTENSION.version;
    @InputFile
    private File preFfJar = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/deobfuscated.jar", version));
    @InputFile
    private File srg = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/mappings/srgs/mcp-notch.srg");
    @InputFile
    private File exc = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/mappings/srgs/mcp.exc");
    @InputFile
    private File methodsCsv = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/mappings/methods.csv");
    @InputFile
    private File fieldsCsv = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/mappings/fields.csv");
    @InputFiles
    private FileCollection libs = getProject().getConfigurations().getByName(Names.MC_DEPENDENCIES_CONF);

    @OutputFile
    private File output = new File(getProject().getChildProjects().get("generated").getBuildDir(), "/projectCache/reobfuscated.jar");

    @TaskAction
    public void doReobfuscate() throws IOException {
        File srg = getSrg();
        {
            ReobfExceptor exceptor = new ReobfExceptor();
            exceptor.toReobfJar = input;
            exceptor.deobfJar = getPreFfJar();
            exceptor.excConfig = getExc();
            exceptor.fieldCSV = getFieldsCsv();
            exceptor.methodCSV = getMethodsCsv();
            File out = new File(getTemporaryDir(), "class_reobf.srg");
            exceptor.doFirstThings();
            exceptor.buildSrg(srg, out);
            srg = out;
        }
        JarMapping mapping = new JarMapping();
        mapping.loadMappings(Files.newReader(srg, Charset.defaultCharset()), null, null,  false);
        JarRemapper remapper = new JarRemapper(null, mapping);
        net.md_5.specialsource.Jar input = net.md_5.specialsource.Jar.init(this.input);
        JointProvider inheritanceProviders = new JointProvider();
        inheritanceProviders.add(new JarProvider(input));
        inheritanceProviders.add(new ClassLoaderProvider(new URLClassLoader(toUrlArray(libs))));
        mapping.setFallbackInheritanceProvider(inheritanceProviders);
        if (!output.getParentFile().exists()) {
            output.getParentFile().mkdirs();
        }
        remapper.remapJar(input, output);
    }

    private URL[] toUrlArray(FileCollection collection) throws MalformedURLException {
        Set<File> files = collection.getAsFileTree().getFiles();
        URL[] array = new URL[files.size()];
        int i = 0;
        for (File f : files) {
            array[i++] = f.toURI().toURL();
        }
        return array;
    }

    @Input
    public String getVersion() {
        return version;
    }

    @InputFiles
    public FileCollection getLibs() {
        return libs;
    }

    @InputFile
    public File getInput() {
        return input;
    }

    @OutputFile
    public File getOutput() {
        return output;
    }

    @InputFile
    public File getPreFfJar() {
        return preFfJar;
    }

    @InputFile
    public File getSrg() {
        return srg;
    }

    @InputFile
    public File getExc() {
        return exc;
    }

    @InputFile
    public File getMethodsCsv() {
        return methodsCsv;
    }

    @InputFile
    public File getFieldsCsv() {
        return fieldsCsv;
    }

    public void setInput(File input) {
        this.input = input;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPreFfJar(File preFfJar) {
        this.preFfJar = preFfJar;
    }

    public void setSrg(File srg) {
        this.srg = srg;
    }

    public void setExc(File exc) {
        this.exc = exc;
    }

    public void setMethodsCsv(File methodsCsv) {
        this.methodsCsv = methodsCsv;
    }

    public void setFieldsCsv(File fieldsCsv) {
        this.fieldsCsv = fieldsCsv;
    }

    public void setLibs(FileCollection libs) {
        this.libs = libs;
    }

    public void setOutput(File output) {
        this.output = output;
    }
}
