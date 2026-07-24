package trade.tracker.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import trade.tracker.client.TradeTrackerConfig;
import trade.tracker.client.VillagerRolodex;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {

    @Shadow public abstract UUID getUUID();

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void forceRolodexGlow(CallbackInfoReturnable<Boolean> cir) {
        if (TradeTrackerConfig.glowStyle != TradeTrackerConfig.GlowStyle.VANILLA) return;
        if (!cir.getReturnValueZ() && VillagerRolodex.isMatch(this.getUUID())) {
            cir.setReturnValue(true);
        }
    }
}