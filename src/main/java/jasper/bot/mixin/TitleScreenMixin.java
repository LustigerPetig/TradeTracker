package jasper.bot.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The @Mixin annotation tells Fabric which vanilla class we want to modify.
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    // We inject our code at the "RETURN" (the very end) of the render method.
    // This ensures our text is drawn ON TOP of the background and vanilla buttons.
    @Inject(at = @At("RETURN"), method = "render")
    private void addCustomSplashText(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        // We get the default text renderer
        var textRenderer = MinecraftClient.getInstance().textRenderer;

        // Draw the text!
        // Parameters: Text Renderer, String to draw, X position, Y position, Color (Hex format)
        context.drawTextWithShadow(
                textRenderer,
                "Fabric Mod Loaded Successfully!",
                10, // X coordinate (10 pixels from the left)
                10, // Y coordinate (10 pixels from the top)
                0xFFFFFF // White color
        );
    }
}