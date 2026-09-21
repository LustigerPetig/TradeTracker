package trade.tracker.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class TradeTrackerNeoForge {

    public static KeyMapping searchKeyBinding;

    public TradeTrackerNeoForge(IEventBus modEventBus, ModContainer modContainer) {

        TradeTrackerConfig.load(FMLPaths.CONFIGDIR.get());
        RolodexStorage.setConfigDir(FMLPaths.CONFIGDIR.get());

        TradeTracker.init();

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> TradeTrackerConfigScreen.createScreen(parent));

        modEventBus.addListener(this::registerKeyBindings);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
        NeoForge.EVENT_BUS.addListener(this::onSubmitCustomGeometry);
    }

    private void registerKeyBindings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Constants.MOD_ID, "tradetracker.binds")
        );

        searchKeyBinding = new KeyMapping(
                "key.tradetracker.search",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_V,
                category
        );

        event.register(searchKeyBinding);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        while (searchKeyBinding != null && searchKeyBinding.consumeClick()) {
            Minecraft.getInstance().gui.setScreen(new RolodexSearchScreen());
        }
    }

    private void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        Constants.LOG.info("[TradeTracker] Joined world, loading Rolodex data...");
        RolodexStorage.load();
    }

    private void onPlayerLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        Constants.LOG.info("[TradeTracker] Disconnected, clearing Rolodex RAM...");
        VillagerRolodex.CACHE.clear();
    }

    private void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {

        TradeTrackerEspRenderer.render(event.getPoseStack(), event.getSubmitNodeCollector());
    }

}