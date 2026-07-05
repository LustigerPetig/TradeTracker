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
import java.util.List;
import java.util.UUID;

public class RolodexSearchScreen extends Screen {

    private EditBox searchBox;
    private final List<Button> tradeButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private Button nextButton;
    private Button prevButton;

    // NEW: This is now a dynamic variable, calculated every time the screen opens or resizes!
    private int tradesPerPage = 5;

    private final List<VillagerRolodex.TradeInfo> filteredTrades = new ArrayList<>();

    public RolodexSearchScreen() {
        super(Component.literal("Villager Rolodex"));
    }

    @Override
    protected void init() {
        super.init();

        // CRITICAL: Clear the old buttons in case the player resizes the window while the menu is open
        this.tradeButtons.clear();

        // --- THE RESPONSIVE MATH ---
        // Top margin (Search bar) takes up ~45 pixels.
        // Bottom margin (Pagination) takes up ~40 pixels.
        // Total usable space = height - 85.
        // Each button is 24px tall + 2px gap = 26px step.
        int usableHeight = this.height - 85;
        this.tradesPerPage = Math.max(1, usableHeight / 26); // Ensure it always fits at least 1!
        // ---------------------------

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

        // Generate exactly as many buttons as the screen can hold
        for (int i = 0; i < this.tradesPerPage; i++) {
            int index = i;
            Button btn = Button.builder(Component.empty(), b -> {
                        int actualIndex = (this.scrollOffset * this.tradesPerPage) + index;
                        if (actualIndex < this.filteredTrades.size()) {
                            UUID target = this.filteredTrades.get(actualIndex).villagerId;
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

        for (List<VillagerRolodex.TradeInfo> villagerTrades : VillagerRolodex.CACHE.values()) {
            for (VillagerRolodex.TradeInfo trade : villagerTrades) {
                if (query.isEmpty() || trade.resultSearchKey.contains(query)) {
                    this.filteredTrades.add(trade);
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
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox.keyPressed(event)) {
            return true;
        }

        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            VillagerRolodex.targetedVillager = null;
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String pageText = "Page " + (this.scrollOffset + 1);
        if (this.filteredTrades.isEmpty()) pageText = "No cached trades found!";
        graphics.text(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2, this.height - 25, ARGB.white(1.0F));

        for (int i = 0; i < this.tradesPerPage; i++) {
            Button btn = this.tradeButtons.get(i);
            if (btn.visible) {
                int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
                if (actualIndex < this.filteredTrades.size()) {
                    VillagerRolodex.TradeInfo trade = this.filteredTrades.get(actualIndex);

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