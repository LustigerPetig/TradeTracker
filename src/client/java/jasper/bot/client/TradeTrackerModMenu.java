package jasper.bot.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class TradeTrackerModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Jasper Bot Settings"));


            builder.setSavingRunnable(() -> {
                TradeTrackerConfig.save();
            });


            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();


            general.addEntry(entryBuilder.startIntField(Component.literal("Villager Glow Duration (Seconds)"), TradeTrackerConfig.glowDurationMs / 1000)
                    .setDefaultValue(10)
                    .setMin(1)
                    .setMax(3600) // Max 1 hour
                    .setTooltip(Component.literal("How many seconds the targeted villager will glow."))

                    .setSaveConsumer(newValue -> TradeTrackerConfig.glowDurationMs = newValue * 1000)
                    .build());


            general.addEntry(entryBuilder.startEnumSelector(Component.literal("ESP Render Style"), TradeTrackerConfig.GlowStyle.class, TradeTrackerConfig.glowStyle)
                    .setDefaultValue(TradeTrackerConfig.GlowStyle.VANILLA)
                    .setTooltip(Component.literal("Vanilla uses standard outlines. Bounding Box draws a clean X-Ray square through walls."))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.glowStyle = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Trade Menu Distance"), TradeTrackerConfig.showDistance)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Shows the Distance of the Villager of the Selected Trade"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.showDistance = newValue)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Do live updates of Distances"), TradeTrackerConfig.doLiveDistance)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Does live updates of distance values while the Menu is open"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.doLiveDistance = newValue)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Do live distance sorting"), TradeTrackerConfig.doLiveSorting)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Does live updates of the list sorting with distance values while the Menu is open"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.doLiveSorting = newValue)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Villager Level"), TradeTrackerConfig.showLvl)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Shows the Level of the Trader next to its role"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.showLvl = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntField(Component.literal("Scroll Speed of Text (ms)"), TradeTrackerConfig.scrollSpeed)
                    .setDefaultValue(500)
                    .setMin(1)
                    .setMax(60000) // Max 1 min
                    .setTooltip(Component.literal("How many milliseconds it takes to scroll through the an entire out of bounds text ."))

                    .setSaveConsumer(newValue -> TradeTrackerConfig.scrollSpeed = newValue )
                    .build());


            return builder.build();
        };
    }
}