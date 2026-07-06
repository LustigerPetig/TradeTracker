package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.*;

public class VillagerRolodex {


    // 1. Die TradeInfo braucht keine UUID mehr, sie ist jetzt "dumm" und hält nur die Item-Daten
    public static class TradeInfo {
        public final ItemStack costA;
        public final ItemStack costB;
        public final ItemStack result;
        public final String niceResultName;
        public final String resultSearchKey;

        public TradeInfo(ItemStack costA, ItemStack costB, ItemStack result, String niceResultName, String resultSearchKey) {
            this.costA = costA;
            this.costB = costB;
            this.result = result;
            this.niceResultName = niceResultName;
            this.resultSearchKey = resultSearchKey.toLowerCase();
        }
    }

    // 2. Deine neue Villager-Klasse, die alles zusammenhält
    public static class IndexedVillager {
        public final UUID uuid;
        public String profession;
        public int level;
        public int chunkX;
        public int chunkZ;
        public List<TradeInfo> trades;

        public IndexedVillager(UUID uuid, String profession, int level, int chunkX, int chunkZ, List<TradeInfo> trades) {
            this.uuid = uuid;
            this.profession = profession;
            this.level = level;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.trades = trades;
        }
    }

    // 3. Der Cache mapped nun von UUID auf das komplette Villager-Objekt
    public static final Map<UUID, IndexedVillager> CACHE = new HashMap<>();
    public static Villager lastInteractedVillagerEntity = null;
    public static String searchQuery = "";
    public static UUID lastInteractedVillager = null;
    public static UUID targetedVillager = null;
    public static long glowExpiration = 0;

    // 4. Die Methode nimmt jetzt auch die Metadaten des Villagers entgegen
    public static void cacheVillager(UUID villagerId, String profession, int level, int chunkX, int chunkZ, MerchantOffers offers) {
        List<TradeInfo> trades = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            ItemStack costA = offer.getBaseCostA().copy();
            ItemStack costB = offer.getCostB().copy();
            ItemStack result = offer.getResult().copy();

            String niceName = result.getHoverName().getString();

            List<Component> tooltips = result.getTooltipLines(
                    Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    TooltipFlag.NORMAL
            );

            if (niceName.contains("Enchanted Book") && tooltips.size() > 1) {
                niceName += " (" + tooltips.get(1).getString() + ")";
            }

            StringBuilder searchKey = new StringBuilder();
            for (Component line : tooltips) {
                searchKey.append(line.getString()).append(" ");
            }

            trades.add(new TradeInfo(costA, costB, result, niceName, searchKey.toString()));
        }

        // Wir erstellen das neue Villager-Objekt und legen es im Cache ab
        IndexedVillager indexedVillager = new IndexedVillager(villagerId, profession, level, chunkX, chunkZ, trades);
        CACHE.put(villagerId, indexedVillager);

        RolodexStorage.save();
    }

    // 5. Angepasste isMatch-Methode für die neue Datenstruktur
    public static boolean isMatch(UUID villagerId) {
        if (targetedVillager != null) {
            if (System.currentTimeMillis() < glowExpiration) {
                return villagerId.equals(targetedVillager);
            } else {
                targetedVillager = null;
            }
        }

        if (searchQuery.isEmpty()) return false;

        // Wir holen uns jetzt den IndexedVillager aus dem Cache, nicht mehr direkt die Liste
        IndexedVillager villager = CACHE.get(villagerId);
        if (villager == null) return false;

        // String query = searchQuery.toLowerCase();
        // for (TradeInfo trade : villager.trades) {  // <-- Hier greifen wir jetzt über villager.trades zu
        //    if (trade.resultSearchKey.contains(query)) {
        //        return true;
        //    }
        //}
        return false;
    }
}