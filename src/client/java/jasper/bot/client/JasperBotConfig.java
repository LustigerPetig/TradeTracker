package jasper.bot.client;

import net.fabricmc.loader.api.FabricLoader;

import javax.swing.text.StyledEditorKit;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class JasperBotConfig {

    // This is our global setting! We default to true.
    public static boolean forceOpenGL = true;

    // The global variable for the glow timer! Default is 10000ms (10 seconds)
    public static int glowDurationMs = 10000;

    // We tell Fabric to save this exactly in the standard config folder

    public static boolean showDistance = false;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("jasperbot.properties");

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                    Properties props = new Properties();
                    props.load(in);

                    // Read variables from the file
                    forceOpenGL = Boolean.parseBoolean(props.getProperty("forceOpenGL", "true"));

                    // NEW: Read the glow duration, with a safe fallback to 10000 if the property is missing
                    glowDurationMs = Integer.parseInt(props.getProperty("glowDurationMs", "10000"));

                    showDistance = Boolean.parseBoolean(props.getProperty("showDistance", "false"));
                }
            } else {
                save(); // If no file exists, create a fresh one!
            }
        } catch (Exception e) {
            System.err.println("[JasperBot] Failed to load config!");
        }
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            Properties props = new Properties();

            // Write the current states to the file
            props.setProperty("forceOpenGL", String.valueOf(forceOpenGL));

            // NEW: Write the glow duration to the file
            props.setProperty("glowDurationMs", String.valueOf(glowDurationMs));

            props.setProperty("showDistance", String.valueOf(showDistance));

            props.store(out, "Jasper Bot Configuration");
        } catch (Exception e) {
            System.err.println("[JasperBot] Failed to save config!");
        }
    }
}