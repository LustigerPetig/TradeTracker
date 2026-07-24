package trade.tracker.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class TradeTrackerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 1. Set the config directory for your common storage class
        RolodexStorage.setConfigDir(FabricLoader.getInstance().getConfigDir());

        // 2. Load your common config
        TradeTrackerConfig.load(FabricLoader.getInstance().getConfigDir());

        // 3. Bootstrap your common initialization class (replacing the template's CommonClass.init())
        TradeTracker.init();
    }
}