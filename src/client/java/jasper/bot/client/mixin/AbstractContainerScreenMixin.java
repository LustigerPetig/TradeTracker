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

        // Prüfen, ob wir wirklich im Villager-Handelsfenster sind
        if ((Object) this instanceof MerchantScreen screen) {

            // Wenn wir diesen Villager schon gespeichert haben, brechen wir direkt ab
            if (this.rolodexCached) return;

            MerchantMenu menu = screen.getMenu();

            // Wir holen uns das Villager-Objekt, das wir beim Rechtsklick gespeichert haben
            Villager villager = VillagerRolodex.lastInteractedVillagerEntity;

            // Prüfen, ob der Villager da ist, das Menü existiert und die Trades geladen sind
            if (villager != null && menu != null && !menu.getOffers().isEmpty()) {

                // 1. Alle neuen Metadaten aus dem Entity auslesen
                String profession = villager.getVillagerData().profession().getRegisteredName();
                int level = villager.getVillagerData().level();
                int chunkX = villager.chunkPosition().x();
                int chunkZ = villager.chunkPosition().z();

                System.out.println("[Rolodex] Caching " + profession + " (Lvl " + level + ") at Chunk [" + chunkX + ", " + chunkZ + "]. Offers: " + menu.getOffers().size());

                // 2. Deine neue Methode mit allen Daten aufrufen
                VillagerRolodex.cacheVillager(
                        villager.getUUID(),
                        profession,
                        level,
                        chunkX,
                        chunkZ,
                        menu.getOffers()
                );

                // 3. Flag setzen, damit wir nicht 20x pro Sekunde speichern
                this.rolodexCached = true;
            }
        }
    }
}