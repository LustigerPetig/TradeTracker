package trade.tracker.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(Constants.MOD_ID)
public class TradeTrackerNeoForge {

    public TradeTrackerNeoForge(IEventBus eventBus) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        TradeTrackerConfig.load(FMLPaths.CONFIGDIR.get());
        RolodexStorage.setConfigDir(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());

        // Use NeoForge to bootstrap the Common mod.
        Constants.LOG.info("Hello NeoForge world!");
        CommonClass.init();

    }
}