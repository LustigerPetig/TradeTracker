package trade.tracker.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;


public class TradeTrackerClient implements ClientModInitializer {

    public static final KeyMapping.Category TradeTracker_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(TradeTracker.MOD_ID, "tradetracker.binds")
    );
    public static KeyMapping searchKeyBinding;

    @Override
    public void onInitializeClient() {

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            TradeTrackerEspRenderer.render(context.poseStack(), context.submitNodeCollector());
        });

        searchKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tradetracker.search",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_V,
                TradeTracker_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKeyBinding.consumeClick()) {
                client.gui.setScreen(new RolodexSearchScreen());
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            TradeTracker.LOGGER.info("[TradeTracker] Joined world, loading Rolodex data...");
            RolodexStorage.load();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TradeTracker.LOGGER.info("[TradeTracker] Disconnected, clearing Rolodex RAM...");
            VillagerRolodex.CACHE.clear();
        });
    }
}