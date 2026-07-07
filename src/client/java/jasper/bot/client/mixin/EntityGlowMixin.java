package jasper.bot.client.mixin;

import jasper.bot.client.JasperBotConfig;
import jasper.bot.client.VillagerRolodex;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Shadow public abstract UUID getUUID();

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void forceRolodexGlow(CallbackInfoReturnable<Boolean> cir) {
        // Only force the vanilla glow outline when that style is selected
        if (JasperBotConfig.glowStyle != JasperBotConfig.GlowStyle.VANILLA) return;

        // If the entity isn't already glowing naturally, we check our Rolodex
        if (!cir.getReturnValueZ() && VillagerRolodex.isMatch(this.getUUID())) {
            cir.setReturnValue(true); // LIGHT THEM UP!
        }
    }
}