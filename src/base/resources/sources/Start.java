package tk.amplifiable.mcgradle;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Start {
    private static final File RUN_DIRECTORY = new File("${runDirectory}");
    private static final File NATIVE_DIRECTORY = new File("${nativeDirectory}");
    private static final String CLIENT_MAIN_CLASS = "${clientMainClass}";
    private static final String SERVER_MAIN_CLASS = "${serverMainClass}";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Start() {
    }

    public static void main(String... args) throws Exception {
        addNatives();
        boolean client = true;
        List<String> effectiveArgs = Lists.newArrayList();
        String username = null;
        String password = null;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.equalsIgnoreCase("--server")) {
                client = false;
            } else if (s.equalsIgnoreCase("--username")) {
                username = args[i + 1];
                i++;
            } else if (s.equalsIgnoreCase("--password")) {
                password = args[i + 1];
                i++;
            } else {
                effectiveArgs.add(s);
            }
        }
        String accessToken = "MCGradle";
        if (username != null && password == null) effectiveArgs.addAll(Arrays.asList("--username", username));
        else if (password != null && username == null) effectiveArgs.addAll(Arrays.asList("--password", password));
        else if (username != null) { // password is always null if reached
            JsonObject config = null;
            File configFile = new File(RUN_DIRECTORY, "mcgradle/auth.json");
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    JsonParser parser = new JsonParser();
                    config = parser.parse(reader).getAsJsonObject();
                } catch (ClassCastException | JsonSyntaxException ex) {
                    config = null; // just to be sure
                }
            }
            String clientToken = UUID.randomUUID().toString();
            if (config != null && config.has("clientToken") && config.get("clientToken").isJsonPrimitive() && config.getAsJsonPrimitive("clientToken").isString()) clientToken = config.get("clientToken").getAsString();
            YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Proxy.NO_PROXY, clientToken);
            UserAuthentication auth = authService.createUserAuthentication(Agent.MINECRAFT);
            boolean loggedIn = false;
            if (config != null && config.has("authlibData") && config.get("authlibData").isJsonObject()) {
                JsonObject authlibData = config.getAsJsonObject("authlibData");
                try {
                    Map<String, Object> data = GSON.fromJson(authlibData, new TypeToken<Map<String, Object>>() {}.getType());
                    auth.loadFromStorage(data);
                    auth.logIn();
                    loggedIn = true;
                } catch (Exception ex) {
                    loggedIn = false; // just to be sure
                }
            }
            if (!loggedIn) {
                auth = authService.createUserAuthentication(Agent.MINECRAFT); // Authlib is dumb and prioritizes loadfromstorage so we have to create a new auth thing
                auth.setUsername(username);
                auth.setPassword(password);
                auth.logIn();
            }
            config = new JsonObject();
            config.addProperty("clientToken", authService.getClientToken());
            config.add("authlibData", GSON.toJsonTree(auth.saveForStorage()));
            if (!configFile.getParentFile().exists()) {
                if (!configFile.getParentFile().mkdirs()) {
                    throw new IOException("Couldn't create configuration file directory");
                }
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
            }
            accessToken =
            effectiveArgs.addAll(Arrays.asList("--username", auth.getSelectedProfile().getName(), "--uuid", auth.getSelectedProfile().getId().toString()));
        }
        effectiveArgs.add("--version");
        effectiveArgs.add("MCGradle");
        effectiveArgs.add("--accessToken");
        effectiveArgs.add(accessToken);
        effectiveArgs.add("--assetsDir");
        effectiveArgs.add(new File("${assetsDirectory}").getAbsolutePath());
        effectiveArgs.add("--assetIndex");
        effectiveArgs.add("${assetIndex}");
        effectiveArgs.add("--userProperties");
        effectiveArgs.add("{}");
        effectiveArgs.add("--gameDir");
        effectiveArgs.add(RUN_DIRECTORY.getAbsolutePath());
        Class<?> mainClass = Class.forName(client ? CLIENT_MAIN_CLASS : SERVER_MAIN_CLASS);
        Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        mainMethod.setAccessible(true);
        System.gc();
        mainMethod.invoke(null, (Object) effectiveArgs.toArray(new String[0]));
    }

    private static void addNatives() {
        String nativesDir = NATIVE_DIRECTORY.getAbsolutePath();
        String paths = System.getProperty("java.library.path");
        if (Strings.isNullOrEmpty(paths)) {
            paths = nativesDir;
        } else {
            paths += File.pathSeparator + nativesDir;
        }
        System.setProperty("java.library.path", paths);
        try {
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Throwable ignored) {
        }
    }
}
