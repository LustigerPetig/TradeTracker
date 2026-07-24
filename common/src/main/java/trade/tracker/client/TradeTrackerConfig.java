package trade.tracker.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class TradeTrackerConfig {

    public enum GlowStyle {
        VANILLA,
        BOUNDING_BOX
    }

    public static int glowDurationMs = 10000;
    public static boolean showDistance = true;
    public static boolean doLiveDistance = true;
    public static boolean doLiveSorting = false;
    public static boolean showLvl = false;
    public static int scrollSpeed = 500;
    public static GlowStyle glowStyle = GlowStyle.VANILLA;
    public static int glowColor = 0xFF00FF00;

    // Make this mutable so loaders can inject the correct path
    private static Path CONFIG_PATH;

    // Loaders will call this and pass their specific config directory
    public static void load(Path configDir) {
        CONFIG_PATH = configDir.resolve("tradetracker.properties");
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                    Properties props = new Properties();
                    props.load(in);

                    glowDurationMs = Integer.parseInt(props.getProperty("glowDurationMs", "10000"));
                    showDistance = Boolean.parseBoolean(props.getProperty("showDistance", "true"));
                    doLiveDistance = Boolean.parseBoolean(props.getProperty("doLiveDistance", "true"));
                    doLiveSorting = Boolean.parseBoolean(props.getProperty("doLiveSorting", "false"));
                    showLvl = Boolean.parseBoolean(props.getProperty("showLvl", "false"));
                    scrollSpeed = Integer.parseInt(props.getProperty("scrollSpeed","500"));

                    try {
                        glowStyle = GlowStyle.valueOf(props.getProperty("glowStyle", "VANILLA"));
                    } catch (IllegalArgumentException e) {
                        glowStyle = GlowStyle.VANILLA;
                    }
                    glowColor = Integer.parseInt(props.getProperty("glowColor", "0xFF00FF00"));
                }
            } else {
                save();
            }
        } catch (Exception e) {
            System.err.println("[TradeTracker] Failed to load config!");
        }
    }

    public static void save() {
        if (CONFIG_PATH == null) return; // Prevent saving before initialization

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            Properties props = new Properties();

            props.setProperty("glowDurationMs", String.valueOf(glowDurationMs));
            props.setProperty("showDistance", String.valueOf(showDistance));
            props.setProperty("doLiveDistance", String.valueOf(doLiveDistance));
            props.setProperty("doLiveSorting", String.valueOf(doLiveSorting));
            props.setProperty("showLvl",String.valueOf(showLvl));
            props.setProperty("scrollSpeed", String.valueOf(scrollSpeed));
            props.setProperty("glowStyle", glowStyle.name());
            props.setProperty("glowColor", String.valueOf(glowColor));

            props.store(out, "Trade Tracker Configuration");
        } catch (Exception e) {
            System.err.println("[TradeTracker] Failed to save config!");
        }
    }
}