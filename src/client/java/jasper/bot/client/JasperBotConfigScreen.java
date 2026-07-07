package jasper.bot.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NonNull;

public class JasperBotConfigScreen extends Screen {
    private final Screen parent;
    private EditBox secondsBox; // NEW: The text box widget

    public JasperBotConfigScreen(Screen parent) {
        super(Component.literal("Jasper Bot Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // 1. The Toggle Button (Moved slightly higher)
        this.addRenderableWidget(Button.builder(
                getToggleText(),
                button -> {
                    JasperBotConfig.forceOpenGL = !JasperBotConfig.forceOpenGL;
                    JasperBotConfig.save();
                    button.setMessage(getToggleText());
                }
        ).bounds(this.width / 2 - 100, this.height / 2 - 40, 200, 20).build());

        // 2. The Text Box for Seconds
        this.secondsBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 + 10, 200, 20, Component.literal("Glow Duration"));

        // Load the current config value, convert back to seconds for display
        this.secondsBox.setValue(String.valueOf(JasperBotConfig.glowDurationMs / 1000));

        // Listen to every keystroke
        this.secondsBox.setResponder(text -> {
            try {
                // Try to parse the text into a number
                int seconds = Integer.parseInt(text.trim());

                // Convert to milliseconds and save!
                JasperBotConfig.glowDurationMs = seconds * 1000;
                JasperBotConfig.save();
            } catch (NumberFormatException e) {
                // If they typed letters or deleted everything, ignore it so the game doesn't crash
            }
        });

        this.addRenderableWidget(this.secondsBox);

        // 3. The Back/Done Button
        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> this.onClose()
        ).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    private Component getToggleText() {
        return Component.literal("Force OpenGL 4.6: " + (JasperBotConfig.forceOpenGL ? "ON" : "OFF"));
    }

    // NEW: We must intercept key presses so you can type in the box without triggering game binds
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.secondsBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Render Title
        String titleString = this.title.getString();
        graphics.text(
                this.font,
                titleString,
                this.width / 2 - this.font.width(titleString) / 2,
                20,
                ARGB.white(1.0F)
        );

        // Render Restart Warning (Moved higher)
        String warningString = "Requires a game restart to take effect!";
        graphics.text(
                this.font,
                warningString,
                this.width / 2 - this.font.width(warningString) / 2,
                this.height / 2 - 60,
                0xFFFF5555
        );

        // NEW: Render a label right above the text box
        String boxLabel = "Villager Glow Duration (Seconds):";
        graphics.text(
                this.font,
                boxLabel,
                this.width / 2 - this.font.width(boxLabel) / 2,
                this.height / 2 - 5,
                ARGB.white(1.0F)
        );
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}