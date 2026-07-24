package trade.tracker.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class TradeTrackerModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Points directly to the shared screen builder in your common project
        return TradeTrackerConfigScreen::createScreen;
    }
}