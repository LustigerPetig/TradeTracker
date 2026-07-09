package trade.tracker;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeTracker implements ModInitializer {
    public static final String MOD_ID = "tradetracker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("TradeTracker is initializing!");
    }

    // Hilfsmethode für Identifiers (angepasst auf Java)
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}