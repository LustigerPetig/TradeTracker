package jasper.bot.client.mixin;

import jasper.bot.client.VillagerRolodex;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Target the parent class where containerTick is physically written!
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    // Add @Unique so the compiler knows this is a safe, custom variable
    @Unique
    private boolean rolodexCached = false;

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void onContainerTick(CallbackInfo ci) {

        // Ensure the screen currently ticking is actually the Villager menu
        if ((Object) this instanceof MerchantScreen screen) {

            if (this.rolodexCached) return;

            MerchantMenu menu = screen.getMenu();

            if (VillagerRolodex.lastInteractedVillager != null && menu != null && !menu.getOffers().isEmpty()) {
                System.out.println("[Rolodex] Caching trades for " + VillagerRolodex.lastInteractedVillager + ", offers: " + menu.getOffers().size());
                VillagerRolodex.cacheTrades(VillagerRolodex.lastInteractedVillager, menu.getOffers());

                // Flip the unique flag so we don't spam the cache 20 times a second
                this.rolodexCached = true;
            }
        }
    }
}