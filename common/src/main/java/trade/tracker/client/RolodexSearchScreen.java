package trade.tracker.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class RolodexSearchScreen extends Screen {

    private int liveSortTimer;

    private enum SortMode {
        DISTANCE("Sort: Distance"),
        NAME("Sort: Name"),
        PROFESSION("Sort: Profession"),
        COST("Sort: Cost");

        final String label;
        SortMode(String label) { this.label = label; }
        SortMode next() {
            SortMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private record RowEntry(VillagerRolodex.IndexedVillager villager, VillagerRolodex.TradeInfo trade, double distance, boolean isHeader) {}
    private record VillagerGroup(VillagerRolodex.IndexedVillager villager, List<VillagerRolodex.TradeInfo> trades, double distance) {}

    private EditBox searchBox;
    private final List<Button> tradeButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private Button nextButton;
    private Button prevButton;

    private Button sortButton;
    private Button toggleModeButton;

    private Button filterBookButton;

    private static SortMode sortMode = SortMode.DISTANCE;
    private static boolean groupByVillager = false;
    private static boolean onlyEnchantedBooks = false;

    private int tradesPerPage = 5;

    private final Map<UUID, Float> liveDistances = new HashMap<>();

    private final List<RowEntry> listEntries = new ArrayList<>();

    public RolodexSearchScreen() {
        super(Component.literal("Villager Rolodex"));
    }

    @Override
    protected void init() {
        super.init();

        this.tradeButtons.clear();

        int usableHeight = this.height - 135;
        this.tradesPerPage = Math.max(1, usableHeight / 26);

        this.searchBox = new EditBox(this.font, this.width / 2 - 80, 28, 160, 20, Component.literal("Search Trades"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setValue(VillagerRolodex.searchQuery);
        this.searchBox.setResponder(text -> {
            VillagerRolodex.searchQuery = text.trim();
            this.scrollOffset = 0;
            refreshList();
        });

        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        this.toggleModeButton = Button.builder(Component.literal(groupByVillager ? "View: Villagers" : "View: Trades"), btn -> {
            groupByVillager = !groupByVillager;
            btn.setMessage(Component.literal(groupByVillager ? "View: Villagers" : "View: Trades"));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 - 165, 52, 100, 20).build();
        this.addRenderableWidget(this.toggleModeButton);

        this.filterBookButton = Button.builder(Component.literal(onlyEnchantedBooks ? "Filter: Books" : "Filter: All"), btn -> {
            onlyEnchantedBooks = !onlyEnchantedBooks;
            btn.setMessage(Component.literal(onlyEnchantedBooks ? "Filter: Books" : "Filter: All"));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 - 50, 52, 100, 20).build();
        this.addRenderableWidget(this.filterBookButton);

        this.sortButton = Button.builder(Component.literal(sortMode.label), btn -> {
            sortMode = sortMode.next();
            btn.setMessage(Component.literal(sortMode.label));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 + 65, 52, 100, 20).build();
        this.addRenderableWidget(this.sortButton);

        this.prevButton = Button.builder(Component.literal("< Prev"), btn -> {
            if (this.scrollOffset > 0) {
                this.scrollOffset--;
                updateButtonLabels();
            }
        }).bounds(this.width / 2 - 165, this.height - 40, 80, 20).build();
        this.addRenderableWidget(this.prevButton);

        this.nextButton = Button.builder(Component.literal("Next >"), btn -> {
            if ((this.scrollOffset + 1) * this.tradesPerPage < this.listEntries.size()) {
                this.scrollOffset++;
                updateButtonLabels();
            }
        }).bounds(this.width / 2 + 85, this.height - 40, 80, 20).build();
        this.addRenderableWidget(this.nextButton);

        for (int i = 0; i < this.tradesPerPage; i++) {
            final int buttonIndex = i;
            Button btn = Button.builder(Component.empty(), button -> {
                int actualIndex = (this.scrollOffset * this.tradesPerPage) + buttonIndex;
                if (actualIndex < this.listEntries.size()) {
                    RowEntry entry = this.listEntries.get(actualIndex);
                    UUID targetVillager = entry.villager().uuid;

                    if (targetVillager.equals(VillagerRolodex.targetedVillager)) {
                        VillagerRolodex.targetedVillager = null;
                        VillagerRolodex.glowExpiration = 0;
                        this.onClose();
                    } else {
                        VillagerRolodex.targetedVillager = targetVillager;
                        VillagerRolodex.glowExpiration = System.currentTimeMillis() + TradeTrackerConfig.glowDurationMs;
                        this.onClose();
                    }
                }
            }).bounds(this.width / 2 - 165, 85 + (i * 26), 330, 24).build();

            this.tradeButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        refreshList();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (this.getFocused() != this.searchBox) {
            if (this.getFocused() instanceof Button clickedButton) {
                clickedButton.setFocused(false);
            }
            this.setFocused(this.searchBox);
        }
        return handled;
    }

    @Override
    public void tick() {
        super.tick();
        if (TradeTrackerConfig.doLiveDistance) {
            updateLiveDistances();

            if (this.sortMode == SortMode.DISTANCE && TradeTrackerConfig.doLiveSorting) {
                this.liveSortTimer++;

                if (this.liveSortTimer >= 20) {
                    this.liveSortTimer = 0;
                    refreshList();
                }
            }
        }
    }

    private void updateLiveDistances() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.player != null) {
            this.liveDistances.clear();

            AABB searchBox = client.player.getBoundingBox().inflate(128.0);
            List<Villager> loadedVillagers =
                    client.level.getEntitiesOfClass(Villager.class, searchBox);

            for (Villager entity : loadedVillagers) {
                float exactDistance = client.player.distanceTo(entity);
                this.liveDistances.put(entity.getUUID(), exactDistance);
            }
        }
    }

    private String formatProfessionName(String profession) {
        String key = profession.replace("minecraft:", "");
        if (key.isEmpty()) return "Unknown";
        return key.substring(0, 1).toUpperCase() + key.substring(1).replace("_", " ");
    }

    private ItemStack getWorkstationIcon(String profession) {
        String key = profession.replace("minecraft:", "").toLowerCase();
        return switch (key) {
            case "armorer" -> new ItemStack(Items.BLAST_FURNACE);
            case "butcher" -> new ItemStack(Items.SMOKER);
            case "cartographer" -> new ItemStack(Items.CARTOGRAPHY_TABLE);
            case "cleric" -> new ItemStack(Items.BREWING_STAND);
            case "farmer" -> new ItemStack(Items.COMPOSTER);
            case "fisherman" -> new ItemStack(Items.BARREL);
            case "fletcher" -> new ItemStack(Items.FLETCHING_TABLE);
            case "leatherworker" -> new ItemStack(Items.CAULDRON);
            case "librarian" -> new ItemStack(Items.LECTERN);
            case "mason" -> new ItemStack(Items.STONECUTTER);
            case "shepherd" -> new ItemStack(Items.LOOM);
            case "toolsmith" -> new ItemStack(Items.SMITHING_TABLE);
            case "weaponsmith" -> new ItemStack(Items.GRINDSTONE);
            case "nitwit", "none" -> new ItemStack(Items.VILLAGER_SPAWN_EGG);
            default -> new ItemStack(Items.EMERALD);
        };
    }

    private void refreshList() {
        this.listEntries.clear();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        double playerX = client.player.getX();
        double playerZ = client.player.getZ();

        List<VillagerRolodex.IndexedVillager> villagerPool = new ArrayList<>(VillagerRolodex.CACHE.values());

        String query = VillagerRolodex.searchQuery.toLowerCase();

        if (groupByVillager) {
            List<VillagerGroup> groups = new ArrayList<>();

            for (VillagerRolodex.IndexedVillager villager : villagerPool) {
                double dist = Math.sqrt(
                        Math.pow(villager.cordX - playerX, 2) +
                                Math.pow(villager.cordZ - playerZ, 2)
                );

                List<VillagerRolodex.TradeInfo> matchingTrades = villager.trades.stream()
                        .filter(t -> matchesQuery(t, villager, query))
                        .filter(t -> !onlyEnchantedBooks || t.result.is(Items.ENCHANTED_BOOK))
                        .toList();

                if (!matchingTrades.isEmpty()) {
                    groups.add(new VillagerGroup(villager, matchingTrades, dist));
                }
            }

            sortGroups(groups);

            for (VillagerGroup group : groups) {
                this.listEntries.add(new RowEntry(group.villager(), null, group.distance(), true));
                for (VillagerRolodex.TradeInfo trade : group.trades()) {
                    this.listEntries.add(new RowEntry(group.villager(), trade, group.distance(), false));
                }
            }

        } else {
            for (VillagerRolodex.IndexedVillager villager : villagerPool) {
                double dist = Math.sqrt(
                        Math.pow(villager.cordX - playerX, 2) +
                                Math.pow(villager.cordZ - playerZ, 2)
                );

                for (VillagerRolodex.TradeInfo trade : villager.trades) {
                    if (onlyEnchantedBooks && !trade.result.is(Items.ENCHANTED_BOOK)) continue;

                    if (matchesQuery(trade, villager, query)) {
                        this.listEntries.add(new RowEntry(villager, trade, dist, false));
                    }
                }
            }

            sortEntries(this.listEntries);
        }

        updateButtonLabels();
    }

    private boolean matchesQuery(VillagerRolodex.TradeInfo trade, VillagerRolodex.IndexedVillager villager, String query) {
        if (query.isEmpty()) return true;

        if (villager.nameTag != null && villager.nameTag.toLowerCase().contains(query)) return true;

        if (trade.niceResultName.toLowerCase().contains(query)) return true;
        if (trade.result.getHoverName().getString().toLowerCase().contains(query)) return true;

        if (formatProfessionName(villager.profession).toLowerCase().contains(query)) return true;
        if (trade.costA.getHoverName().getString().toLowerCase().contains(query)) return true;
        if (trade.costB.getHoverName().getString().toLowerCase().contains(query)) return true;

        return false;
    }

    private void sortGroups(List<VillagerGroup> groups) {
        switch (sortMode) {
            case DISTANCE -> groups.sort(Comparator.comparingDouble(VillagerGroup::distance));
            case NAME -> groups.sort((a, b) -> {
                String nameA = a.trades().isEmpty() ? "" : a.trades().get(0).niceResultName;
                String nameB = b.trades().isEmpty() ? "" : b.trades().get(0).niceResultName;
                return nameA.compareToIgnoreCase(nameB);
            });
            case PROFESSION -> groups.sort(Comparator.comparing(g -> g.villager().profession));
            case COST -> groups.sort((a, b) -> {
                int costA = a.trades().isEmpty() ? 0 : a.trades().get(0).costA.getCount();
                int costB = b.trades().isEmpty() ? 0 : b.trades().get(0).costA.getCount();
                return Integer.compare(costA, costB);
            });
        }
    }

    private void sortEntries(List<RowEntry> entries) {
        switch (sortMode) {
            case DISTANCE -> entries.sort(Comparator.comparingDouble(RowEntry::distance));
            case NAME -> entries.sort((a, b) -> a.trade().niceResultName.compareToIgnoreCase(b.trade().niceResultName));
            case PROFESSION -> entries.sort(Comparator.comparing(e -> e.villager().profession));
            case COST -> entries.sort(Comparator.comparingInt(e -> e.trade().costA.getCount()));
        }
    }

    private void updateButtonLabels() {
        for (int i = 0; i < this.tradesPerPage; i++) {
            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            Button btn = this.tradeButtons.get(i);

            if (actualIndex < this.listEntries.size()) {
                btn.active = true;
                btn.visible = true;

                RowEntry entry = this.listEntries.get(actualIndex);
                if (groupByVillager && !entry.isHeader()) {
                    btn.setX(this.width / 2 - 145);
                    btn.setWidth(310);
                } else {
                    btn.setX(this.width / 2 - 165);
                    btn.setWidth(330);
                }
            } else {
                btn.active = false;
                btn.visible = false;
            }
        }

        this.prevButton.active = this.scrollOffset > 0;
        this.nextButton.active = (this.scrollOffset + 1) * this.tradesPerPage < this.listEntries.size();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        String titleText = "Trade Tracker";
        guiGraphics.drawString(this.font, titleText, this.width / 2 - this.font.width(titleText) / 2, 12, 0xFFFFFFFF);

        float totalPages = (float) this.listEntries.size() / this.tradesPerPage;
        String pageText = this.listEntries.isEmpty()
                ? "No cached trades nearby!"
                : "Page " + (this.scrollOffset + 1) + " / " + (int) Math.ceil(totalPages);
        guiGraphics.drawString(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2, this.height - 22, 0xFFFFFFFF);

        for (int i = 0; i < this.tradesPerPage; i++) {
            Button btn = this.tradeButtons.get(i);
            if (!btn.visible) continue;

            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            if (actualIndex >= this.listEntries.size()) continue;

            RowEntry entry = this.listEntries.get(actualIndex);

            int drawX = btn.getX() + 10;
            int drawY = btn.getY() + 4;
            int maxWidth;
            int nameTextWidth;
            double liveDistance;
            if (TradeTrackerConfig.doLiveDistance) {
                liveDistance = this.liveDistances.getOrDefault(entry.villager().uuid, (float) entry.distance());
            } else {
                liveDistance = (float) entry.distance();
            }
            String distanceText = String.format("%.0fm", liveDistance);

            if (entry.isHeader()) {
                ItemStack workstation = getWorkstationIcon(entry.villager().profession);
                guiGraphics.renderItem(workstation, drawX, drawY);
                drawX += 20;
                String cleanName = formatProfessionName(entry.villager().profession);
                guiGraphics.drawString(this.font, cleanName, drawX, drawY + 4, 0xFFFFD700);
                if (TradeTrackerConfig.showLvl) {
                    drawX += this.font.width(cleanName) + 2;
                    String LvlTxt = "(Lvl " + entry.villager().level + "/5)";
                    guiGraphics.drawString(this.font, LvlTxt, drawX, drawY + 4, 0xFFFFD700);
                    drawX += this.font.width(LvlTxt) + 5;
                } else {
                    drawX += this.font.width(cleanName) + 5;
                }

                maxWidth = btn.getWidth() - (drawX - btn.getX()) - 5;

                if (!List.of("armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "shepherd", "toolsmith", "weaponsmith", "nitwit", "villager").contains(entry.villager().nameTag.toLowerCase())) {
                    nameTextWidth = this.font.width(entry.villager().nameTag);
                    if (!(nameTextWidth > maxWidth)) {
                        guiGraphics.drawString(this.font, "'" + entry.villager().nameTag + "'", drawX, drawY + 4, 0xFFFFFFFF);
                    } else {
                        if (TradeTrackerConfig.showDistance) {
                            maxWidth -= this.font.width(distanceText) + 5;
                        }
                        guiGraphics.enableScissor(drawX, drawY - 2, drawX + maxWidth, drawY + 12);

                        long time = Util.getMillis();
                        int maxScroll = nameTextWidth - maxWidth;

                        double wave = (Math.sin((double) time / TradeTrackerConfig.scrollSpeed) + 1.0) / 2.0;
                        int scrollOffset = (int) (maxScroll * wave);

                        guiGraphics.drawString(this.font, entry.villager().nameTag, drawX - scrollOffset, drawY + 4, 0xFFFFFFFF);

                        guiGraphics.disableScissor();
                    }
                }

                if (TradeTrackerConfig.showDistance) {
                    int textWidth = this.font.width(distanceText);
                    int rightAlignedX = btn.getX() + btn.getWidth() - textWidth - 5;
                    guiGraphics.drawString(this.font, distanceText, rightAlignedX, drawY + 4, 0xFFFFFFFF);
                }
            } else {
                VillagerRolodex.TradeInfo trade = entry.trade();
                if (trade == null) continue;

                if (trade.costA.getCount() == trade.localCostA.getCount()) {
                    guiGraphics.renderItem(trade.costA, drawX, drawY);
                    guiGraphics.renderItemDecorations(this.font, trade.costA, drawX, drawY);
                    drawX += 22;
                } else if (trade.costA.getCount() == 1) {
                    guiGraphics.renderItem(trade.costA, drawX, drawY);
                    guiGraphics.renderItemDecorations(this.font, trade.costA, drawX, drawY, String.valueOf(trade.costA.getCount()));
                    guiGraphics.fill(drawX + 7, drawY + 12, drawX + 16, drawY + 13, 0xFFBA370F);
                    drawX += 20;
                    guiGraphics.drawString(this.font, String.valueOf(trade.localCostA.getCount()), drawX, drawY + 9, 0xFFFFFFFF);
                    drawX += 22;
                } else {
                    guiGraphics.renderItem(trade.costA, drawX, drawY);
                    guiGraphics.renderItemDecorations(this.font, trade.costA, drawX, drawY);
                    guiGraphics.fill(drawX + 7, drawY + 12, drawX + 16, drawY + 13, 0xFFBA370F);
                    drawX += 20;
                    guiGraphics.drawString(this.font, String.valueOf(trade.localCostA.getCount()), drawX, drawY + 9, 0xFFFFFFFF);
                    drawX += 22;
                }

                if (!trade.costB.isEmpty()) {
                    guiGraphics.drawString(this.font, "+", drawX, drawY + 4, 0xFFFFFFFF);
                    drawX += 12;
                    guiGraphics.renderItem(trade.costB, drawX, drawY);
                    guiGraphics.renderItemDecorations(this.font, trade.costB, drawX, drawY);
                    drawX += 22;
                }

                guiGraphics.drawString(this.font, "->", drawX, drawY + 4, 0xFFFFFFFF);
                drawX += 18;

                guiGraphics.renderItem(trade.result, drawX, drawY);
                guiGraphics.renderItemDecorations(this.font, trade.result, drawX, drawY);
                drawX += 22;

                maxWidth = btn.getWidth() - (drawX - btn.getX()) - 5;
                nameTextWidth = this.font.width(trade.niceResultName);
                if (!(nameTextWidth > maxWidth)) {
                    guiGraphics.drawString(this.font, trade.niceResultName, drawX, drawY + 4, 0xFF55FF55);
                } else {
                    if (!groupByVillager && TradeTrackerConfig.showDistance) {
                        maxWidth -= this.font.width(distanceText) + 5;
                    }
                    guiGraphics.enableScissor(drawX, drawY - 2, drawX + maxWidth, drawY + 12);

                    long time = Util.getMillis();
                    int maxScroll = nameTextWidth - maxWidth;

                    double wave = (Math.sin((double) time / TradeTrackerConfig.scrollSpeed) + 1.0) / 2.0;
                    int scrollOffset = (int) (maxScroll * wave);

                    guiGraphics.drawString(this.font, trade.niceResultName, drawX - scrollOffset, drawY + 4, 0xFF55FF55);

                    guiGraphics.disableScissor();
                }

                if (!groupByVillager && TradeTrackerConfig.showDistance) {
                    int textWidth = this.font.width(distanceText);
                    int rightAlignedX = btn.getX() + btn.getWidth() - textWidth - 5;
                    guiGraphics.drawString(this.font, distanceText, rightAlignedX, drawY + 4, 0xFFFFFFFF);
                }
            }
        }
    }

    @Override
    public void onClose() {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
                if (entity instanceof Villager) {
                    entity.refreshDimensions();
                }
            });
        }
        super.onClose();
    }
}