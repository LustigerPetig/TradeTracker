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
import org.lwjgl.glfw.GLFW;

@Mod(Constants.MOD_ID)
public class TradeTrackerNeoForge {

    public static KeyMapping searchKeyBinding;

    // Notice we added ModContainer here!
    public TradeTrackerNeoForge(IEventBus modEventBus, ModContainer modContainer) {

        // 1. Setup Config Paths (from your current file)
        TradeTrackerConfig.load(FMLPaths.CONFIGDIR.get());
        RolodexStorage.setConfigDir(FMLPaths.CONFIGDIR.get());

        // 2. Bootstrap your Common Code (replacing CommonClass.init())
        TradeTracker.init();

        // 3. Tell NeoForge where to find your Cloth Config screen
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> TradeTrackerConfigScreen.createScreen(parent));

        // 4. Register Keybindings on the Mod Event Bus
        modEventBus.addListener(this::registerKeyBindings);

        // 5. Register gameplay events on the NeoForge Event Bus
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
        NeoForge.EVENT_BUS.addListener(this::onSubmitCustomGeometry);
    }

    private void registerKeyBindings(RegisterKeyMappingsEvent event) {
        // 1. Create the Category object using NeoForge's ResourceLocation
        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(Constants.MOD_ID, "jasperbot.binds")
        );

        // 2. Pass the Category object into the KeyMapping instead of a String
        searchKeyBinding = new KeyMapping(
                "key.jasperbot.search",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                category
        );

        event.register(searchKeyBinding);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        // Listen for the key press and open the UI
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
        // Replaces the old RenderLevelStageEvent + SubmitNodeCollector hook, which no longer
        // exists on RenderLevelStageEvent as of the 26.2 rendering rewrite. This new event is
        // purpose-built for exactly this: submitting custom world-space geometry (not tied to
        // an entity/block-entity renderer) outside the normal feature pipeline.
        TradeTrackerEspRenderer.render(event.getPoseStack(), event.getSubmitNodeCollector());
    }

}