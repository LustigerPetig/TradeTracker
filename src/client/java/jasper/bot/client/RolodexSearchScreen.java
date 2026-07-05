package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.npc.villager.Villager;
import org.lwjgl.glfw.GLFW;

public class RolodexSearchScreen extends Screen {
    private EditBox searchBox;

    public RolodexSearchScreen() {
        super(Component.literal("Villager Rolodex"));
    }

    @Override
    protected void init() {
        super.init();

        // Create the box
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 200, 20, Component.literal("Search Trades"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setValue(VillagerRolodex.searchQuery);

        this.searchBox.setResponder(text -> {
            VillagerRolodex.searchQuery = text.trim();
        });

        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 1. Let the EditBox handle typing first
        if (this.searchBox.keyPressed(event)) {
            return true;
        }

        // 2. Check for Enter key to close the screen
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    // ADDED THIS: This handles the logic when the screen closes
    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(
                this.font,
                "Villager Rolodex Search",
                this.width / 2 - this.font.width("Villager Rolodex Search") / 2,
                this.height / 2 - 25,
                ARGB.white(1.0F)
        );

        graphics.text(
                this.font,
                "Press ENTER to search",
                this.width / 2 - this.font.width("Press ENTER to search") / 2,
                this.height / 2 + 15,
                0xFFAAAAAA
        );
    }
}