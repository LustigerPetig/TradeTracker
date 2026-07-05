package jasper.bot.client.mixin;

import com.mojang.blaze3d.opengl.GlBackend;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlBackend.class)
public class GlBackendMixin {

    // By injecting at the RETURN (the very end) of Mojang's new method,
    // we execute our hints last, permanently overriding their 3.3 cap!
    @Inject(method = "setWindowHints", at = @At("RETURN"))
    private void forceOpenGLVersion(CallbackInfo ci) {

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);

    }
}