package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RolodexSearchScreen extends Screen {

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

    private record DisplayTrade(VillagerRolodex.IndexedVillager villager, VillagerRolodex.TradeInfo trade, double distance) {}
    private EditBox searchBox;
    private final List<Button> tradeButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private Button nextButton;
    private Button prevButton;
    private Button sortButton;
    private SortMode sortMode = SortMode.DISTANCE;

    private int tradesPerPage = 5;

    private final List<DisplayTrade> filteredTrades = new ArrayList<>();

    public RolodexSearchScreen() {
        super(Component.literal("Villager Rolodex"));
    }

    @Override
    protected void init() {
        super.init();

        this.tradeButtons.clear();

        int usableHeight = this.height - 85;
        this.tradesPerPage = Math.max(1, usableHeight / 26);

        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 15, 200, 20, Component.literal("Search Trades"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setValue(VillagerRolodex.searchQuery);
        this.searchBox.setResponder(text -> {
            VillagerRolodex.searchQuery = text.trim();
            this.scrollOffset = 0;
            refreshList();
        });

        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // Sort button — sits to the right of the search box
        this.sortButton = Button.builder(Component.literal(sortMode.label), btn -> {
            sortMode = sortMode.next();
            btn.setMessage(Component.literal(sortMode.label));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 + 105, 15, 100, 20).build();
        this.addRenderableWidget(this.sortButton);

        this.prevButton = Button.builder(Component.literal("<"), btn -> {
            if (this.scrollOffset > 0) {
                this.scrollOffset--;
                refreshList();
            }
        }).bounds(this.width / 2 - 175, this.height - 30, 20, 20).build();

        this.nextButton = Button.builder(Component.literal(">"), btn -> {
            if ((this.scrollOffset + 1) * this.tradesPerPage < this.filteredTrades.size()) {
                this.scrollOffset++;
                refreshList();
            }
        }).bounds(this.width / 2 + 155, this.height - 30, 20, 20).build();

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);

        for (int i = 0; i < this.tradesPerPage; i++) {
            int index = i;
            Button btn = Button.builder(Component.empty(), b -> {
                        int actualIndex = (this.scrollOffset * this.tradesPerPage) + index;
                        if (actualIndex < this.filteredTrades.size()) {
                            UUID target = this.filteredTrades.get(actualIndex).villager().uuid;
                            VillagerRolodex.targetedVillager = target;
                            VillagerRolodex.glowExpiration = System.currentTimeMillis() + JasperBotConfig.glowDurationMs;
                            this.onClose();
                        }
                    })
                    .bounds(this.width / 2 - 150, 45 + (i * 26), 300, 24).build();

            this.tradeButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        refreshList();
    }

    private void refreshList() {
        this.filteredTrades.clear();
        String query = VillagerRolodex.searchQuery.toLowerCase();

        Set<UUID> nearbyVillagers = new HashSet<>();
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
                if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
                    nearbyVillagers.add(entity.getUUID());
                }
            });
        }

        Minecraft client = Minecraft.getInstance();
        double playerX = client.player != null ? client.player.getX() : 0;
        double playerZ = client.player != null ? client.player.getZ() : 0;

        for (VillagerRolodex.IndexedVillager villager : VillagerRolodex.CACHE.values()) {
            if (!nearbyVillagers.contains(villager.uuid)) continue;

            double villagerX = (villager.chunkX * 16) + 8;
            double villagerZ = (villager.chunkZ * 16) + 8;
            double distance = Math.sqrt(Math.pow(playerX - villagerX, 2) + Math.pow(playerZ - villagerZ, 2));

            for (VillagerRolodex.TradeInfo trade : villager.trades) {
                if (query.isEmpty() || trade.resultSearchKey.contains(query)) {
                    this.filteredTrades.add(new DisplayTrade(villager, trade, distance));
                }
            }
        }

        // Sort according to selected mode
        switch (sortMode) {
            case DISTANCE  -> this.filteredTrades.sort(Comparator.comparingDouble(DisplayTrade::distance));
            case NAME      -> this.filteredTrades.sort(Comparator.comparing(t -> t.trade().niceResultName.toLowerCase()));
            case PROFESSION -> this.filteredTrades.sort(Comparator.comparing(t -> t.villager().profession.toLowerCase()));
            case COST      -> this.filteredTrades.sort(Comparator.comparingInt(t -> t.trade().localCostA.getCount()));
        }

        for (int i = 0; i < this.tradesPerPage; i++) {
            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            Button btn = this.tradeButtons.get(i);
            btn.active = actualIndex < this.filteredTrades.size();
            btn.visible = actualIndex < this.filteredTrades.size();
        }

        this.prevButton.active = this.scrollOffset > 0;
        this.nextButton.active = (this.scrollOffset + 1) * this.tradesPerPage < this.filteredTrades.size();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        float totalPages = (float) this.filteredTrades.size() / this.tradesPerPage;
        String pageText = this.filteredTrades.isEmpty()
                ? "No cached trades nearby!"
                : "Page " + (this.scrollOffset + 1) + " / " + (int) Math.ceil(totalPages);
        graphics.text(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2, this.height - 25, ARGB.white(1.0F));

        for (int i = 0; i < this.tradesPerPage; i++) {
            Button btn = this.tradeButtons.get(i);
            if (!btn.visible) continue;

            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            if (actualIndex >= this.filteredTrades.size()) continue;

            DisplayTrade entry = this.filteredTrades.get(actualIndex);
            VillagerRolodex.TradeInfo trade = entry.trade();

            int drawX = btn.getX() + 10;
            int drawY = btn.getY() + 4;

            if (trade.costA.getCount() == trade.localCostA.getCount()) {
                graphics.item(trade.costA, drawX, drawY);
                graphics.itemDecorations(this.font, trade.costA, drawX, drawY);
                drawX += 22;
            } else {
                graphics.item(trade.costA, drawX, drawY);
                graphics.itemDecorations(this.font, trade.costA, drawX, drawY);
                graphics.fill(drawX + 7, drawY + 12, drawX + 16, drawY + 13, 0xFFBA370F);
                drawX += 20;
                graphics.text(this.font, String.valueOf(trade.localCostA.getCount()), drawX, drawY + 9, ARGB.white(1.0f));
                drawX += 22;
            }

            if (!trade.costB.isEmpty()) {
                graphics.text(this.font, "+", drawX, drawY + 4, 0xFFAAAAAA);
                drawX += 12;
                graphics.item(trade.costB, drawX, drawY);
                graphics.itemDecorations(this.font, trade.costB, drawX, drawY);
                drawX += 22;
            }

            graphics.text(this.font, "->", drawX, drawY + 4, ARGB.white(1.0F));
            drawX += 18;

            graphics.item(trade.result, drawX, drawY);
            graphics.itemDecorations(this.font, trade.result, drawX, drawY);
            drawX += 22;

            graphics.text(this.font, trade.niceResultName, drawX, drawY + 4, 0xFFFFFF55);

            if (JasperBotConfig.showDistance) {
                String distanceText = String.format("%.0fm", entry.distance());
                int textWidth = this.font.width(distanceText);
                int rightAlignedX = btn.getX() + btn.getWidth() - textWidth - 5;
                graphics.text(this.font, distanceText, rightAlignedX, drawY + 4, ARGB.white(1.0F));
            }
        }
    }

    @Override
    public void onClose() {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
                if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
                    entity.refreshDimensions();
                }
            });
        }
        super.onClose();
    }
}