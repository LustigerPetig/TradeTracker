package jasper.bot.client;

import jasper.bot.JasperBot;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class JasperBotClient implements ClientModInitializer {
    public static final KeyMapping.Category JASPERBOT_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(JasperBot.MOD_ID, "jasperbot.binds")
    );
    private static KeyMapping searchKeyBinding;
    @Override
    public void onInitializeClient() {
        // 1. Create the Keybind using KeyMappingHelper (Mojmap name)
        searchKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.jasperbot.search",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                // Use KeyMapping.Category to define the group
                this.JASPERBOT_CATEGORY    ));

        // 2. Listen for the key press every frame
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (searchKeyBinding.consumeClick()) {
                // If they press R, open our new search screen!
                client.gui.setScreen(new RolodexSearchScreen());
            }
        });
    }
}