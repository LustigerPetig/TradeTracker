package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet; // ADDED
import java.util.List;
import java.util.Set; // ADDED
import java.util.UUID;

public class RolodexSearchScreen extends Screen {

    private record DisplayTrade(VillagerRolodex.IndexedVillager villager, VillagerRolodex.TradeInfo trade) {}
    private EditBox searchBox;
    private final List<Button> tradeButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private Button nextButton;
    private Button prevButton;

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
                            VillagerRolodex.glowExpiration = System.currentTimeMillis() + 10000;
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

        // 1. Scan client memory for any villager entities that are currently rendering
        Set<UUID> nearbyVillagers = new HashSet<>();
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
                if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
                    nearbyVillagers.add(entity.getUUID());
                }
            });
        }

        // 2. Filter your structural database by checking if the villager's UUID is in render distance
        for (VillagerRolodex.IndexedVillager villager : VillagerRolodex.CACHE.values()) {

            // NEW: Skip this villager's trades completely if they aren't loaded around the player
            if (!nearbyVillagers.contains(villager.uuid)) {
                continue;
            }

            for (VillagerRolodex.TradeInfo trade : villager.trades) {
                if (query.isEmpty() || trade.resultSearchKey.contains(query)) {
                    this.filteredTrades.add(new DisplayTrade(villager, trade));
                }
            }
        }

        for (int i = 0; i < this.tradesPerPage; i++) {
            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            Button btn = this.tradeButtons.get(i);

            if (actualIndex < this.filteredTrades.size()) {
                btn.active = true;
                btn.visible = true;
            } else {
                btn.active = false;
                btn.visible = false;
            }
        }

        this.prevButton.active = this.scrollOffset > 0;
        this.nextButton.active = (this.scrollOffset + 1) * this.tradesPerPage < this.filteredTrades.size();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        float totalPages = (float) this.filteredTrades.size() / this.tradesPerPage;

        String pageText = "Page " + (this.scrollOffset + 1) + " / " + (int) Math.ceil(totalPages);
        // CHANGED: Informative message updating the context to nearby matches
        if (this.filteredTrades.isEmpty()) pageText = "No cached trades nearby!";
        graphics.text(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2, this.height - 25, ARGB.white(1.0F));

        for (int i = 0; i < this.tradesPerPage; i++) {
            Button btn = this.tradeButtons.get(i);
            if (btn.visible) {
                int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
                if (actualIndex < this.filteredTrades.size()) {
                    DisplayTrade entry = this.filteredTrades.get(actualIndex);
                    VillagerRolodex.TradeInfo trade = entry.trade();

                    int drawX = btn.getX() + 10;
                    int drawY = btn.getY() + 4;

                    graphics.item(trade.costA, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.costA, drawX, drawY);
                    drawX += 22;

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
                }
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