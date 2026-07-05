package jasper.bot.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(at = @At("RETURN"), method = "extractRenderState")
    private void addCustomSplashText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {

        var font = Minecraft.getInstance().font;

        graphics.text(
                font,
                "Jasper stinkt!",
                10,
                10,
                ARGB.white(1.0F)
        );
    }
}