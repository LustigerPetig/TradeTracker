package jasper.bot.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;

public class JasperBotConfigScreen extends Screen {
    private final Screen parent;

    public JasperBotConfigScreen(Screen parent) {
        // We pass the title to the main Screen class
        super(Component.literal("Jasper Bot Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // 1. The Toggle Button
        this.addRenderableWidget(Button.builder(
                getToggleText(),
                button -> {
                    // Flip the boolean
                    JasperBotConfig.forceOpenGL = !JasperBotConfig.forceOpenGL;
                    JasperBotConfig.save(); // Save to hard drive instantly!

                    // Refresh the button text
                    button.setMessage(getToggleText());
                }
        ).bounds(this.width / 2 - 100, this.height / 2 - 20, 200, 20).build());

        // 2. The Back/Done Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> this.onClose()
        ).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    // A helper method so our text is always accurate
    private Component getToggleText() {
        return Component.literal("Force OpenGL 4.6: " + (JasperBotConfig.forceOpenGL ? "ON" : "OFF"));
    }

    // The brand-new 26.2 Rendering Pipeline
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        // This super call renders the dark dirt background!
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Render Title
        String titleString = this.title.getString();
        int titleWidth = this.font.width(titleString);
        graphics.text(
                this.font,
                titleString,
                this.width / 2 - titleWidth / 2,
                20,
                ARGB.white(1.0F)
        );

        // Render Restart Warning
        String warningString = "Requires a game restart to take effect!";
        int warningWidth = this.font.width(warningString);
        graphics.text(
                this.font,
                warningString,
                this.width / 2 - warningWidth / 2,
                this.height / 2 - 45,
                0xFFFF5555 // Raw hex code for a soft red color
        );
    }

    @Override
    public void onClose() {
        // Send the player safely back to ModMenu when they press ESC or Done
        this.minecraft.gui.setScreen(this.parent);
    }
}