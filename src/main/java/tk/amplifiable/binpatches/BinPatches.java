package tk.amplifiable.binpatches;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.nothome.delta.GDiffPatcher;
import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;

import java.io.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.zip.*;

public class BinPatches {
    private static final GDiffPatcher patcher = new GDiffPatcher();

    /**
     * Applies the binary patches to the original file.
     *
     * @param patchContainer an input stream to a file/resource containing the patches (usually binpatches.lzma)
     * @param extraEntries an input stream to a file/resource containing extra stuff that should be added to the JAR, or null if you don't want any extras.
     * @param root the patch root to apply (e.g. client, server, merged)
     * @param original the original jarfile
     * @param output the output jarfile
     * @throws IOException if something goes wrong
     */
    public static void applyPatches(InputStream patchContainer, InputStream extraEntries, String root, File original, File output) throws IOException {
        dbg("Applying patches in %s (%s -> %s)", root, original, output);
        Map<String, ClassPatch> patches = Maps.newHashMap();
        Pattern matcher = Pattern.compile("binpatch/" + root + "/.*.binpatch");
        dbg("Reading patch container");
        LzmaInputStream decompressed = new LzmaInputStream(patchContainer, new Decoder());
        JarInputStream jis = new JarInputStream(decompressed);
        dbg("Reading patches...");
        for (;;) {
            try {
                JarEntry entry = jis.getNextJarEntry();
                if (entry == null) break;
                if (matcher.matcher(entry.getName()).matches()) {
                    ClassPatch cp = readPatch(entry, jis);
                    patches.put(cp.sourceClassName.replace('.', '/') + ".class", cp);
                } else jis.closeEntry();
            } catch (IOException ignored) {}
        }
        dbg("%d patches read", patches.size());

        if (output.exists()) {
            if (output.delete()) {
                throw new IOException("Failed to delete previous output");
            }
        }

        ZipFile in = new ZipFile(original);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
        Set<String> entries = Sets.newHashSet();
        dbg("Patching classes...");
        for (ZipEntry entry : Collections.list(in.entries())) {
            if (entry.getName().contains("META-INF")) continue;
            if (entry.isDirectory()) {
                out.putNextEntry(entry);
            } else {
                ZipEntry newEntry = new ZipEntry(entry.getName());
                newEntry.setTime(entry.getTime());
                out.putNextEntry(newEntry);
                byte[] data = ByteStreams.toByteArray(in.getInputStream(entry));
                ClassPatch patch = patches.get(entry.getName().replace('\\', '/'));
                if (patch != null) {
                    dbg("Patching %s (%s), size %d", patch.targetClassName, patch.sourceClassName, data.length);
                    long checksum = adlerHash(data);
                    if (patch.inputChecksum != checksum) {
                        throw new IllegalStateException(String.format("Failed to verify input integrity of %s (%s) (expected %x, got %x).", patch.targetClassName, patch.sourceClassName, checksum, patch.inputChecksum));
                    }
                    synchronized (patcher) {
                        data = patcher.patch(data, patch.patch);
                    }
                }
                out.write(data);
            }
            entries.add(entry.getName());
        }

        if (extraEntries != null) {
            dbg("Adding extra entries");
            ZipInputStream extraIn = new ZipInputStream(extraEntries);
            ZipEntry entry;
            while ((entry = extraIn.getNextEntry()) != null) {
                if (entries.contains(entry.getName())) continue;
                out.putNextEntry(entry);
                out.write(ByteStreams.toByteArray(extraIn));
                entries.add(entry.getName());
            }
            extraIn.close();
        }
        in.close();
        out.close();
    }

    private static ClassPatch readPatch(JarEntry patchEntry, JarInputStream jis) throws IOException {
        dbg("Reading patch %s", patchEntry.getName());
        ByteArrayDataInput input = ByteStreams.newDataInput(ByteStreams.toByteArray(jis));
        String name = input.readUTF();
        String sourceClassName = input.readUTF();
        String targetClassName = input.readUTF();
        boolean exists = input.readBoolean();
        long inputChecksum = -1;
        if (exists) {
            inputChecksum = input.readLong();
        }
        int patchLength = input.readInt();
        byte[] patchBytes = new byte[patchLength];
        input.readFully(patchBytes);
        return new ClassPatch(name, sourceClassName, targetClassName, exists, patchBytes, inputChecksum);
    }

    private static void dbg(String message, Object... format) {
        System.out.println(String.format("[dbg] %s", String.format(message, format)));
    }

    private static long adlerHash(byte[] bytes) {
        Adler32 hasher = new Adler32();
        hasher.update(bytes);
        return hasher.getValue();
    }

    public static void main(String... args) throws IOException {
        applyPatches(new FileInputStream("build/distrib/binpatches.lzma"), null, "client", new File("C:/Users/Gerrit/.gradle/caches/mcgradle/jars/1.8.9/client.jar"), new File("patched.jar"));
    }

    private static class ClassPatch {
        public final String name;
        final String sourceClassName;
        final String targetClassName;
        final boolean existsAtTarget;
        final byte[] patch;
        final long inputChecksum;

        ClassPatch(String name, String sourceClassName, String targetClassName, boolean existsAtTarget, byte[] patch, long inputChecksum) {
            this.name = name;
            this.sourceClassName = sourceClassName;
            this.targetClassName = targetClassName;
            this.existsAtTarget = existsAtTarget;
            this.patch = patch;
            this.inputChecksum = inputChecksum;
        }
    }
}
