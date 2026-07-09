package trade.tracker.client;

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
                    .setTitle(Component.literal("Trade Tracker Settings"));

            builder.setSavingRunnable(() -> {
                TradeTrackerConfig.save();
            });

            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            var glowSubCategory = entryBuilder.startSubCategory(Component.literal("Villager Glow Settings"))
                    .setExpanded(true) // true = am Anfang ausgeklappt, false = zugeklappt
                    .setTooltip(Component.literal("All settings related to the glow effect."));

            // 1. Glow Duration
            var glowDurationField = entryBuilder.startIntField(Component.literal("Villager Glow Duration (Seconds)"), TradeTrackerConfig.glowDurationMs / 1000)
                    .setDefaultValue(10)
                    .setMin(1)
                    .setMax(3600) // Max 1 hour
                    .setTooltip(Component.literal("How many seconds the targeted villager will glow."))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.glowDurationMs = newValue * 1000)
                    .build();

            // 2. ESP Render Style
            var styleSelector = entryBuilder.startEnumSelector(Component.literal("ESP Render Style"), TradeTrackerConfig.GlowStyle.class, TradeTrackerConfig.glowStyle)
                    .setDefaultValue(TradeTrackerConfig.GlowStyle.VANILLA)
                    .setTooltip(Component.literal("Vanilla uses standard outlines. Bounding Box draws a clean bounding box visible through blocks."))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.glowStyle = newValue)
                    .build();

            // 3. Color of Bounding Box (Hängt von styleSelector ab)
            var glowColorField = entryBuilder.startAlphaColorField(Component.literal("Color of Bounding Box"), TradeTrackerConfig.glowColor)
                    .setDefaultValue(0xFF00FF00) // Standard: 100% Green.
                    .setDisplayRequirement(() -> styleSelector.getValue() == TradeTrackerConfig.GlowStyle.BOUNDING_BOX)
                    .setTooltip(Component.literal("Sets the Color of the Bounding Box"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.glowColor = newValue)
                    .build();

            glowSubCategory.add(glowDurationField);
            glowSubCategory.add(styleSelector);
            glowSubCategory.add(glowColorField);

            general.addEntry(glowSubCategory.build());

            var uiSubCategory = entryBuilder.startSubCategory(Component.literal("User interface Settings"))
                    .setExpanded(true) // true = am Anfang ausgeklappt, false = zugeklappt
                    .setTooltip(Component.literal("All settings related to the user interface."));

            // 4. Show Trade Menu Distance
            var distanceToggle = entryBuilder.startBooleanToggle(Component.literal("Show Trade Menu Distance"), TradeTrackerConfig.showDistance)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Shows the Distance of the Villager of the Selected Trade"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.showDistance = newValue)
                    .build();


            // 5. Live Updates of Distances (Hängt von distanceToggle ab)
            var liveDistanceToggle = entryBuilder.startBooleanToggle(Component.literal("Do live updates of Distances"), TradeTrackerConfig.doLiveDistance)
                    .setDefaultValue(true)
                    .setDisplayRequirement(() -> distanceToggle.getValue())
                    .setTooltip(Component.literal("Does live updates of distance values while the Menu is open"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.doLiveDistance = newValue)
                    .build();


            // 6. Live Distance Sorting
            var liveSortingToggle = entryBuilder.startBooleanToggle(Component.literal("Do live distance sorting"), TradeTrackerConfig.doLiveSorting)
                    .setDefaultValue(false)
                    .setDisplayRequirement(() -> distanceToggle.getValue() && liveDistanceToggle.getValue())
                    .setTooltip(Component.literal("Does live updates of the list sorting with distance values while the Menu is open"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.doLiveSorting = newValue)
                    .build();


            // 7. Show Villager Level
            var showLvlToggle = entryBuilder.startBooleanToggle(Component.literal("Show Villager Level"), TradeTrackerConfig.showLvl)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Shows the Level of the Trader next to its role"))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.showLvl = newValue)
                    .build();


            // 8. Scroll Speed of Text
            var scrollSpeedField = entryBuilder.startIntField(Component.literal("Scroll Speed of Text (ms)"), TradeTrackerConfig.scrollSpeed)
                    .setDefaultValue(500)
                    .setMin(1)
                    .setMax(60000) // Max 1 min
                    .setTooltip(Component.literal("How many milliseconds it takes to scroll through an entire out of bounds text."))
                    .setSaveConsumer(newValue -> TradeTrackerConfig.scrollSpeed = newValue)
                    .build();

            uiSubCategory.add(distanceToggle);
            uiSubCategory.add(liveDistanceToggle);
            uiSubCategory.add(liveSortingToggle);
            uiSubCategory.add(showLvlToggle);
            uiSubCategory.add(scrollSpeedField);

            general.addEntry(uiSubCategory.build());

            return builder.build();
        };
    }
}