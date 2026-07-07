package jasper.bot.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class JasperBotModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // 1. Initialize the fancy Cloth Config Builder
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Jasper Bot Settings"));

            // 2. Tell it to save our global config when the user clicks "Save and Quit"
            builder.setSavingRunnable(() -> {
                JasperBotConfig.save();
            });

            // 3. Create a "General" tab
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // 4. Add the Force OpenGL Toggle
            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Force OpenGL 4.6"), JasperBotConfig.forceOpenGL)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Requires a game restart to take effect!"))
                    .setSaveConsumer(newValue -> JasperBotConfig.forceOpenGL = newValue)
                    .build());

            // 5. Add the Glow Duration Number Field
            general.addEntry(entryBuilder.startIntField(Component.literal("Villager Glow Duration (Seconds)"), JasperBotConfig.glowDurationMs / 1000)
                    .setDefaultValue(10)
                    .setMin(1)
                    .setMax(3600) // Max 1 hour
                    .setTooltip(Component.literal("How many seconds the targeted villager will glow."))
                    // Automatically convert back to milliseconds for our internal engine!
                    .setSaveConsumer(newValue -> JasperBotConfig.glowDurationMs = newValue * 1000)
                    .build());

            // 6. Build and return the screen to ModMenu!
            return builder.build();
        };
    }
}