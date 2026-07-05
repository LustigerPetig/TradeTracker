package jasper.bot.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class JasperBotClient implements ClientModInitializer {

    private static KeyMapping searchKeyBinding;

    @Override
    public void onInitializeClient() {
        // 1. Create the Keybind using KeyMappingHelper (Mojmap name)
        searchKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.jasperbot.search",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                // Use KeyMapping.Category to define the group
                KeyMapping.Category.GAMEPLAY
        ));

        // 2. Listen for the key press every frame
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKeyBinding.consumeClick()) {
                // If they press R, open our new search screen!
                client.gui.setScreen(new RolodexSearchScreen());
            }
        });
    }
}