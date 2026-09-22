package trade.tracker.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class TradeTrackerClient implements ClientModInitializer {

    public static final String TRADETRACKER_CATEGORY = "key.category.tradetracker.tradetracker.binds";
    public static KeyMapping searchKeyBinding;

    @Override
    public void onInitializeClient() {

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            TradeTrackerEspRenderer.render(context.matrixStack(), context.consumers());
        });

        searchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.tradetracker.search",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                TRADETRACKER_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKeyBinding.consumeClick()) {
                client.setScreen(new RolodexSearchScreen());
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