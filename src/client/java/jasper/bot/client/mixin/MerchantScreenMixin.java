package jasper.bot.client.mixin;

import jasper.bot.client.VillagerRolodex;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    private boolean rolodexCached = false;

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void onContainerTick(CallbackInfo ci) {
        if (rolodexCached) return;

        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantMenu menu = screen.getMenu();

        if (VillagerRolodex.lastInteractedVillager != null && menu != null && !menu.getOffers().isEmpty()) {
            System.out.println("[Rolodex] Caching trades for " + VillagerRolodex.lastInteractedVillager + ", offers: " + menu.getOffers().size());
            VillagerRolodex.cacheTrades(VillagerRolodex.lastInteractedVillager, menu.getOffers());
            rolodexCached = true;
        }
    }
}