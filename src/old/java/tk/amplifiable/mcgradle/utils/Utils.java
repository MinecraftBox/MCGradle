package club.ampthedev.mcgradle.utils;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import groovy.json.StringEscapeUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import club.ampthedev.mcgradle.MCGradleConstants;
import club.ampthedev.mcgradle.Names;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static void writeString(String str, File file) throws IOException {
        if (file.getParentFile() != null) {
            MCGradleConstants.prepareDirectory(file.getParentFile());
        }
        FileWriter writer = new FileWriter(file);
        writer.write(str);
        writer.close();
    }

    public static JsonObject readJsonObj(File file) throws IOException {
        FileReader reader = new FileReader(file);
        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();
        return obj;
    }

    public static File getCacheFile(String name) {
        return new File(MCGradleConstants.CACHE_DIRECTORY, name);
    }

    public static JsonObject readVersionManifest() throws IOException {
        return readJsonObj(getCacheFile(Names.VERSION_MANIFEST));
    }

    public static JsonObject readVersionJson(String version) throws IOException {
        return readJsonObj(getCacheFile(String.format(Names.VERSION_JSON, version)));
    }

    public static String escape(String s) {
        return StringEscapeUtils.escapeJava(s);
    }

    public static String sha1(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return Hashing.sha1()
                    .hashBytes(IOUtils.toByteArray(input))
                    .toString();
        }
    }

    public static String sha256(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return Hashing.sha256()
                    .hashBytes(IOUtils.toByteArray(input))
                    .toString();
        }
    }

    public static class ExtractingFileVisitor implements FileVisitor {
        private final File out;

        public ExtractingFileVisitor(File out) {
            this.out = out;
        }

        @Override
        public void visitDir(FileVisitDetails dirDetails) {
            File dir = new File(out, dirDetails.getPath());
            MCGradleConstants.prepareDirectory(dir);
        }

        @Override
        public void visitFile(FileVisitDetails fileDetails) {
            File out = new File(this.out, fileDetails.getPath());
            if (out.getParentFile() != null) {
                MCGradleConstants.prepareDirectory(out.getParentFile());
            }
            fileDetails.copyTo(out);
        }
    }
}
