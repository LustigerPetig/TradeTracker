package trade.tracker.client;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeTracker {
    public static final String MOD_ID = "tradetracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Generic initialization method to be called by loader-specific entrypoints
    public static void init() {
        LOGGER.info("TradeTracker is initializing!");
    }

    // Hilfsmethode für Identifiers (angepasst auf Mojang Mappings)
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}