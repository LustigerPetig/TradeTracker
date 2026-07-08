package jasper.bot.client.mixin;

import jasper.bot.client.VillagerRolodex;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Unique
    private boolean rolodexCached = false;

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void onContainerTick(CallbackInfo ci) {

        // Check for Trade Window
        if ((Object) this instanceof MerchantScreen screen) {

            // Cancel if Villager already checked
            if (this.rolodexCached) return;

            MerchantMenu menu = screen.getMenu();

            // get villager object
            Villager villager = VillagerRolodex.lastInteractedVillagerEntity;

            // Check Villager
            if (villager != null && menu != null && !menu.getOffers().isEmpty()) {

                // 1. Read metadata from Entity
                String profession = villager.getVillagerData().profession().getRegisteredName();
                int level = villager.getVillagerData().level();
                int cordX = (int) villager.position().x();
                int cordZ = (int) villager. position().z();

                System.out.println("[Rolodex] Caching " + profession + " (Lvl " + level + ") at  [" + cordX + ", " + cordZ + "]. Offers: " + menu.getOffers().size());


                VillagerRolodex.cacheVillager(
                        villager.getUUID(),
                        profession,
                        level,
                        cordX,
                        cordZ,
                        menu.getOffers()
                );

                // 3. Flag setzen, damit wir nicht 20x pro Sekunde speichern
                this.rolodexCached = true;
            }
        }
    }
}