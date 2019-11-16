package tk.amplifiable.mcgradle.tasks.mc;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.nothome.delta.Delta;
import lzma.streams.LzmaOutputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.*;
import tk.amplifiable.mcgradle.MCGradleConstants;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.jar.*;
import java.util.zip.Adler32;

public class TaskGenerateBinPatches extends DefaultTask {
    @Input
    private String version = MCGradleConstants.EXTENSION.version;

    @InputFile
    private File cleanClient = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/client.jar", version));

    @InputFile
    private File cleanServer = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/server.jar", version));

    @InputFile
    private File cleanMerged = new File(MCGradleConstants.CACHE_DIRECTORY, String.format("jars/%s/merged.jar", version));

    @InputFile
    private File dirtyJar = new File(getProject().getChildProjects().get("generated").getBuildDir(), "/projectCache/reobfuscated.jar");

    @InputFile
    private File srg = new File(MCGradleConstants.CACHE_DIRECTORY, "jars/" + version + "/mappings/srgs/notch-srg.srg");

    @OutputFile
    private File runBinPatches = new File(getProject().getBuildDir(), "distrib/binpatches.lzma");

    @OutputFile
    private File devBinPatches = new File(getProject().getBuildDir(), "distrib/binpatches.dev.lzma");

    private List<File> patchSets = Lists.newArrayList(new File(getProject().getRootDir(), "patches"));
    private Map<String, String> obfMapping = Maps.newHashMap();
    private Map<String, String> srgMapping = Maps.newHashMap();
    private Multimap<String, String> innerClasses = ArrayListMultimap.create();
    private Set<String> patchedFiles = Sets.newHashSet();
    private Delta delta = new Delta();

    @TaskAction
    private void generateBinPatches() throws IOException {
        loadMappings();

        for (File f : patchSets) {
            String base = f.getAbsolutePath();
            for (File patch : getProject().fileTree(f)) {
                String path = patch.getAbsolutePath().replace(".java.patch", "");
                path = path.substring(base.length() + 1).replace('.', '/');
                String obfName = srgMapping.get(path);
                patchedFiles.add(obfName);
                addInnerClasses(path, patchedFiles);
            }
        }
        Map<String, byte[]> runtime = Maps.newHashMap();
        Map<String, byte[]> devtime = Maps.newHashMap();

        createBinPatches(runtime, "client/", cleanClient, dirtyJar);
        createBinPatches(runtime, "server/", cleanServer, dirtyJar);
        createBinPatches(devtime, "merged/", cleanMerged, dirtyJar);

        MCGradleConstants.prepareDirectory(runBinPatches.getParentFile());
        MCGradleConstants.prepareDirectory(devBinPatches.getParentFile());

        byte[] runtimeData = createPatchJar(runtime);
        runtimeData = compress(runtimeData);
        Files.write(runtimeData, runBinPatches);

        byte[] devtimeData = createPatchJar(devtime);
        devtimeData = compress(devtimeData);
        Files.write(devtimeData, devBinPatches);
    }

    private void createBinPatches(Map<String, byte[]> patches, String root, File base, File target) throws IOException {
        JarFile cleanJar = new JarFile(base);
        JarFile dirtyJar = new JarFile(target);

        for (Map.Entry<String, String> entry : obfMapping.entrySet()) {
            String obf = entry.getKey();
            String srg = entry.getValue();
            if (!patchedFiles.contains(obf)) continue;
            JarEntry cleanEntry = cleanJar.getJarEntry(obf + ".class");
            JarEntry dirtyEntry = dirtyJar.getJarEntry(obf + ".class");
            if (dirtyEntry == null) continue;
            byte[] clean = cleanEntry != null ? ByteStreams.toByteArray(cleanJar.getInputStream(cleanEntry)) : new byte[0];
            byte[] dirty = ByteStreams.toByteArray(dirtyJar.getInputStream(dirtyEntry));

            byte[] diff = delta.compute(clean, dirty);
            ByteArrayDataOutput out = ByteStreams.newDataOutput(diff.length + 50);
            out.writeUTF(obf);
            out.writeUTF(obf.replace('/', '.'));
            out.writeUTF(srg.replace('/', '.'));
            out.writeBoolean(cleanEntry != null);
            if (cleanEntry != null) {
                out.writeLong(adlerHash(clean));
            }
            out.writeInt(diff.length);
            out.write(diff);
            patches.put(root + srg.replace('/', '.') + ".binpatch", out.toByteArray());
        }
        cleanJar.close();
        dirtyJar.close();
    }

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LzmaOutputStream lzma = new LzmaOutputStream.Builder(out).useEndMarkerMode(true).build();
        lzma.write(data);
        lzma.close();
        return out.toByteArray();
    }

    private byte[] createPatchJar(Map<String, byte[]> patches) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jar = new JarOutputStream(out);
        for (Map.Entry<String, byte[]> entry : patches.entrySet()) {
            jar.putNextEntry(new JarEntry("binpatch/" + entry.getKey()));
            jar.write(entry.getValue());
        }
        jar.close();
        return out.toByteArray();
    }

    private long adlerHash(byte[] input) {
        Adler32 hasher = new Adler32();
        hasher.update(input);
        return hasher.getValue();
    }

    private void addInnerClasses(String parent, Set<String> patchList) {
        for (String inner : innerClasses.get(parent)) {
            patchList.add(srgMapping.get(inner));
            addInnerClasses(inner, patchList);
        }
    }

    private void loadMappings() throws IOException {
        Files.asCharSource(srg, StandardCharsets.UTF_8).readLines(new LineProcessor<String>() {
            Splitter splitter = Splitter.on(CharMatcher.anyOf(": ")).omitEmptyStrings().trimResults();

            @Override
            public boolean processLine(@Nonnull String line) {
                if (!line.startsWith("CL")) return true;
                String[] parts = Iterables.toArray(splitter.split(line), String.class);
                obfMapping.put(parts[1], parts[2]);
                String srgName = parts[2];
                srgMapping.put(srgName, parts[1]);
                int inner = srgName.lastIndexOf('$');
                if (inner > 0) {
                    String outer = srgName.substring(0, inner);
                    innerClasses.put(outer, srgName);
                }
                return true;
            }

            @Override
            public String getResult() {
                return null;
            }
        });
    }

    @InputFiles
    public FileCollection getPatchSets() {
        FileCollection collection = null;
        for (File f : patchSets) {
            FileCollection c = getProject().fileTree(f);
            if (collection == null) {
                collection = c;
            } else {
                collection = collection.plus(c);
            }
        }
        return collection;
    }
}
