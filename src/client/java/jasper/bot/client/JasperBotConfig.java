package jasper.bot.client;

import net.fabricmc.loader.api.FabricLoader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class JasperBotConfig {

    // NEW: Define the two styles of ESP rendering available
    public enum GlowStyle {
        VANILLA,
        BOUNDING_BOX
    }

    public static boolean forceOpenGL = true;
    public static int glowDurationMs = 10000;
    public static boolean showDistance = true;
    public static boolean showLvl = false;

    // NEW: Store the current selected style (Defaults to VANILLA)
    public static GlowStyle glowStyle = GlowStyle.VANILLA;

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("jasperbot.properties");

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                    Properties props = new Properties();
                    props.load(in);

                    forceOpenGL = Boolean.parseBoolean(props.getProperty("forceOpenGL", "true"));
                    glowDurationMs = Integer.parseInt(props.getProperty("glowDurationMs", "10000"));
                    showDistance = Boolean.parseBoolean(props.getProperty("showDistance", "true"));
                    showLvl = Boolean.parseBoolean(props.getProperty("showLvl", "false"));

                    // NEW: Safely read the enum value from the config file string
                    try {
                        glowStyle = GlowStyle.valueOf(props.getProperty("glowStyle", "VANILLA"));
                    } catch (IllegalArgumentException e) {
                        glowStyle = GlowStyle.VANILLA; // Safe fallback if file gets corrupted
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            System.err.println("[JasperBot] Failed to load config!");
        }
    }

    public static void save() {
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            Properties props = new Properties();

            props.setProperty("forceOpenGL", String.valueOf(forceOpenGL));
            props.setProperty("glowDurationMs", String.valueOf(glowDurationMs));
            props.setProperty("showDistance", String.valueOf(showDistance));
            props.setProperty("showLvl",String.valueOf(showLvl));

            // NEW: Write the selected style enum as a text string to the file
            props.setProperty("glowStyle", glowStyle.name());

            props.store(out, "Jasper Bot Configuration");
        } catch (Exception e) {
            System.err.println("[JasperBot] Failed to save config!");
        }
    }
}