package jasper.bot.client.mixin;

import com.mojang.blaze3d.opengl.GlBackend;
import jasper.bot.client.JasperBotConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlBackend.class)
public class GlBackendMixin {

    @Inject(method = "setWindowHints", at = @At("RETURN"))
    private void forceOpenGLVersion(CallbackInfo ci) {

        // Load the config file the exact millisecond the game boots
        JasperBotConfig.load();

        // Only run the F3 hack if the setting is true!
        if (JasperBotConfig.forceOpenGL) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        }
    }
}