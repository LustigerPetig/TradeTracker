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

    private final List<VillagerRolodex.TradeInfo> filteredTrades = new ArrayList<>();

    public RolodexSearchScreen() {
        super(Component.literal("Villager Rolodex"));
    }

    @Override
    protected void init() {
        super.init();

        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 20, 200, 20, Component.literal("Search Trades"));
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
            if ((this.scrollOffset + 1) * 5 < this.filteredTrades.size()) {
                this.scrollOffset++;
                refreshList();
            }
        }).bounds(this.width / 2 + 155, this.height - 30, 20, 20).build();

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);

        for (int i = 0; i < 5; i++) {
            int index = i;
            // Blank button! We will draw over it later.
            Button btn = Button.builder(Component.empty(), b -> {
                        int actualIndex = (this.scrollOffset * 5) + index;
                        if (actualIndex < this.filteredTrades.size()) {
                            UUID target = this.filteredTrades.get(actualIndex).villagerId;
                            VillagerRolodex.targetedVillager = target;
                            VillagerRolodex.glowExpiration = System.currentTimeMillis() + 10000;
                            this.onClose();
                        }
                    })
                    // Taller buttons (24px) so 16x16 items fit beautifully
                    .bounds(this.width / 2 - 150, 60 + (i * 28), 300, 24).build();

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

        for (int i = 0; i < 5; i++) {
            int actualIndex = (this.scrollOffset * 5) + i;
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
        this.nextButton.active = (this.scrollOffset + 1) * 5 < this.filteredTrades.size();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 1. Let the EditBox handle typing first
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

    // THE MAGIC HAPPENS HERE
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // 1. Draw Page text
        String pageText = "Page " + (this.scrollOffset + 1);
        if (this.filteredTrades.isEmpty()) pageText = "No cached trades found!";
        graphics.text(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2, this.height - 25, ARGB.white(1.0F));

        // 2. Custom Item Rendering over the blank buttons!
        for (int i = 0; i < 5; i++) {
            Button btn = this.tradeButtons.get(i);
            if (btn.visible) {
                int actualIndex = (this.scrollOffset * 5) + i;
                if (actualIndex < this.filteredTrades.size()) {
                    VillagerRolodex.TradeInfo trade = this.filteredTrades.get(actualIndex);

                    // Start drawing inside the button bounds
                    int drawX = btn.getX() + 10;
                    int drawY = btn.getY() + 4; // Padding to center the 16x16 item vertically

                    // Cost A (e.g. Emeralds)
                    graphics.item(trade.costA, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.costA, drawX, drawY);
                    drawX += 22;

                    // Cost B (e.g. Book)
                    if (!trade.costB.isEmpty()) {
                        graphics.text(this.font, "+", drawX, drawY + 4, 0xFFAAAAAA);
                        drawX += 12;
                        graphics.item(trade.costB, drawX, drawY);
                        graphics.itemDecorations(this.font, trade.costB, drawX, drawY);
                        drawX += 22;
                    }

                    // Arrow
                    graphics.text(this.font, "->", drawX, drawY + 4, ARGB.white(1.0F));
                    drawX += 18;

                    // Result (e.g. Enchanted Book)
                    graphics.item(trade.result, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.result, drawX, drawY);
                    drawX += 22;

                    // Result Text (e.g. "Enchanted Book (Mending I)")
                    graphics.text(this.font, trade.niceResultName, drawX, drawY + 4, 0xFFFFFF55); // Nice yellow color
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