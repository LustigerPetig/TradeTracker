package jasper.bot.client;

import jasper.bot.JasperBot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class JasperBotClient implements ClientModInitializer {
    public static final KeyMapping.Category JASPERBOT_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(JasperBot.MOD_ID, "jasperbot.binds")
    );
    public static KeyMapping searchKeyBinding;

    @Override
    public void onInitializeClient() {

        JasperBotConfig.load();

        searchKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.jasperbot.search",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                JASPERBOT_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKeyBinding.consumeClick()) {
                client.gui.setScreen(new RolodexSearchScreen());
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("[JasperBot] Joined world, loading Rolodex data...");
            RolodexStorage.load();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            System.out.println("[JasperBot] Disconnected, clearing Rolodex RAM...");
            VillagerRolodex.CACHE.clear();
        });

        JasperBotEspRenderer.register();

    }
}