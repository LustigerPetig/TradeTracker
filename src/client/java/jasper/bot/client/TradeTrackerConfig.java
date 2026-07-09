package jasper.bot.client;

import net.fabricmc.loader.api.FabricLoader;
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

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("jasperbot.properties");

    public static void load() {
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

            props.setProperty("glowDurationMs", String.valueOf(glowDurationMs));
            props.setProperty("showDistance", String.valueOf(showDistance));
            props.setProperty("doLiveDistance", String.valueOf(doLiveDistance));
            props.setProperty("doLiveSorting", String.valueOf(doLiveSorting));
            props.setProperty("showLvl",String.valueOf(showLvl));
            props.setProperty("scrollSpeed", String.valueOf(scrollSpeed));
            props.setProperty("glowStyle", glowStyle.name());

            props.store(out, "Jasper Bot Configuration");
        } catch (Exception e) {
            System.err.println("[JasperBot] Failed to save config!");
        }
    }
}