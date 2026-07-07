package jasper.bot.client;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class JasperBotConfig {

    // This is our global setting! We default to true.
    public static boolean forceOpenGL = true;

    public static int glowDurationMs = 10000;

    // We tell Fabric to save this exactly in the standard config folder
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("jasperbot.properties");

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                    Properties props = new Properties();
                    props.load(in);
                    // Read the boolean from the file
                    forceOpenGL = Boolean.parseBoolean(props.getProperty("forceOpenGL", "true"));
                }
            } else {
                save(); // If no file exists, create a fresh one!
            }
        } catch (Exception e) {
            System.err.println("Failed to load Jasper Bot config!");
        }
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            Properties props = new Properties();
            // Write the current boolean state to the file
            props.setProperty("forceOpenGL", String.valueOf(forceOpenGL));
            props.store(out, "Jasper Bot Configuration");
        } catch (Exception e) {
            System.err.println("Failed to save Jasper Bot config!");
        }
    }
}