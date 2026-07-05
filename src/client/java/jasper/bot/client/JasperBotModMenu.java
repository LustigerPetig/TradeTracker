package jasper.bot.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class JasperBotModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> {

            // Return a blank placeholder screen for now.
            // Later, you will replace this with a real config screen!
            return new Screen(Component.literal("Jasper Bot Settings")) {
                @Override
                public void onClose() {
                    // When the user presses ESC, send them safely back to ModMenu
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(parentScreen);
                    }
                }
            };

        };
    }
}