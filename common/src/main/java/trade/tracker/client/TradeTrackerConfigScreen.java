package trade.tracker.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TradeTrackerConfigScreen {

    public static Screen createScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Trade Tracker Settings"));

        builder.setSavingRunnable(() -> {
            TradeTrackerConfig.save();
        });

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        var glowSubCategory = entryBuilder.startSubCategory(Component.literal("Villager Glow Settings"))
                .setExpanded(true)
                .setTooltip(Component.literal("All settings related to the glow effect."));

        var glowDurationField = entryBuilder.startIntField(Component.literal("Villager Glow Duration (Seconds)"), TradeTrackerConfig.glowDurationMs / 1000)
                .setDefaultValue(10)
                .setMin(1)
                .setMax(3600)
                .setTooltip(Component.literal("How many seconds the targeted villager will glow."))
                .setSaveConsumer(newValue -> TradeTrackerConfig.glowDurationMs = newValue * 1000)
                .build();

        var styleSelector = entryBuilder.startEnumSelector(Component.literal("ESP Render Style"), TradeTrackerConfig.GlowStyle.class, TradeTrackerConfig.glowStyle)
                .setDefaultValue(TradeTrackerConfig.GlowStyle.VANILLA)
                .setTooltip(Component.literal("Vanilla uses standard outlines. Bounding Box draws a clean bounding box visible through blocks."))
                .setSaveConsumer(newValue -> TradeTrackerConfig.glowStyle = newValue)
                .build();

        var glowColorField = entryBuilder.startAlphaColorField(Component.literal("Color of Bounding Box"), TradeTrackerConfig.glowColor)
                .setDefaultValue(0xFF00FF00)
                .setDisplayRequirement(() -> styleSelector.getValue() == TradeTrackerConfig.GlowStyle.BOUNDING_BOX)
                .setTooltip(Component.literal("Sets the Color of the Bounding Box"))
                .setSaveConsumer(newValue -> TradeTrackerConfig.glowColor = newValue)
                .build();

        glowSubCategory.add(glowDurationField);
        glowSubCategory.add(styleSelector);
        glowSubCategory.add(glowColorField);

        general.addEntry(glowSubCategory.build());

        var uiSubCategory = entryBuilder.startSubCategory(Component.literal("User interface Settings"))
                .setExpanded(true)
                .setTooltip(Component.literal("All settings related to the user interface."));

        var distanceToggle = entryBuilder.startBooleanToggle(Component.literal("Show Trade Menu Distance"), TradeTrackerConfig.showDistance)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Shows the Distance of the Villager of the Selected Trade"))
                .setSaveConsumer(newValue -> TradeTrackerConfig.showDistance = newValue)
                .build();

        var liveDistanceToggle = entryBuilder.startBooleanToggle(Component.literal("Do live updates of Distances"), TradeTrackerConfig.doLiveDistance)
                .setDefaultValue(true)
                .setDisplayRequirement(distanceToggle::getValue)
                .setTooltip(Component.literal("Does live updates of distance values while the Menu is open"))
                .setSaveConsumer(newValue -> TradeTrackerConfig.doLiveDistance = newValue)
                .build();

        var liveSortingToggle = entryBuilder.startBooleanToggle(Component.literal("Do live distance sorting"), TradeTrackerConfig.doLiveSorting)
                .setDefaultValue(false)
                .setDisplayRequirement(() -> distanceToggle.getValue() && liveDistanceToggle.getValue())
                .setTooltip(Component.literal("Does live updates of the list sorting with distance values while the Menu is open"))
                .setSaveConsumer(newValue -> TradeTrackerConfig.doLiveSorting = newValue)
                .build();

        var showLvlToggle = entryBuilder.startBooleanToggle(Component.literal("Show Villager Level"), TradeTrackerConfig.showLvl)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Shows the Level of the Trader next to its role"))
                .setSaveConsumer(newValue -> TradeTrackerConfig.showLvl = newValue)
                .build();

        var scrollSpeedField = entryBuilder.startIntField(Component.literal("Scroll Speed of Text (ms)"), TradeTrackerConfig.scrollSpeed)
                .setDefaultValue(500)
                .setMin(1)
                .setMax(60000)
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
    }
}