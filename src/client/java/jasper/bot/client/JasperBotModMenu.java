package jasper.bot.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class JasperBotModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // We inject the previous screen (ModMenu) as the parent!
        return JasperBotConfigScreen::new;
    }
}