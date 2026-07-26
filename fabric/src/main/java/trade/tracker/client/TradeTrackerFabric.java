package trade.tracker.client;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class TradeTrackerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        RolodexStorage.setConfigDir(FabricLoader.getInstance().getConfigDir());

        TradeTrackerConfig.load(FabricLoader.getInstance().getConfigDir());

        TradeTracker.init();
    }
}