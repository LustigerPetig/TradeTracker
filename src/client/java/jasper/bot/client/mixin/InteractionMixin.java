package jasper.bot.client.mixin;

import jasper.bot.client.VillagerRolodex;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class InteractionMixin {

    @Inject(method = "interact", at = @At("HEAD"))
    private void onInteract(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {

        if (entity instanceof Villager villager) {


            VillagerRolodex.lastInteractedVillager = villager.getUUID();


            VillagerRolodex.lastInteractedVillagerEntity = villager;

        }
    }
}