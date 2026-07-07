package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.*;

public class VillagerRolodex {


    public static class TradeInfo {
        public final ItemStack costA;
        public final ItemStack localCostA;
        public final ItemStack costB;
        public final ItemStack result;
        public final String niceResultName;
        public final String resultSearchKey;

        public TradeInfo(ItemStack costA,ItemStack localCostA, ItemStack costB, ItemStack result, String niceResultName, String resultSearchKey) {
            this.costA = costA;
            this.localCostA = localCostA;
            this.costB = costB;
            this.result = result;
            this.niceResultName = niceResultName;
            this.resultSearchKey = resultSearchKey.toLowerCase();
        }
    }


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


    public static final Map<UUID, IndexedVillager> CACHE = new HashMap<>();
    public static Villager lastInteractedVillagerEntity = null;
    public static String searchQuery = "";
    public static UUID lastInteractedVillager = null;
    public static UUID targetedVillager = null;
    public static long glowExpiration = 0;


    public static void cacheVillager(UUID villagerId, String profession, int level, int chunkX, int chunkZ, MerchantOffers offers) {
        List<TradeInfo> trades = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            ItemStack costA = offer.getBaseCostA().copy();
            ItemStack localCostA = offer.getCostA().copy();
            ItemStack costB = offer.getCostB().copy();
            ItemStack result = offer.getResult().copy();

            String niceName = result.getHoverName().getString();

            List<Component> tooltips = result.getTooltipLines(
                    Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    TooltipFlag.NORMAL
            );
            if (BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals("minecraft:enchanted_book") && tooltips.size() > 1){
                niceName += " (" + tooltips.get(1).getString() + ")";
            }

            StringBuilder searchKey = new StringBuilder();
            for (Component line : tooltips) {
                searchKey.append(line.getString()).append(" ");
            }

            trades.add(new TradeInfo(costA,localCostA, costB, result, niceName, searchKey.toString()));
        }

        // Create Villager Object and save to cache
        IndexedVillager indexedVillager = new IndexedVillager(villagerId, profession, level, chunkX, chunkZ, trades);
        CACHE.put(villagerId, indexedVillager);

        RolodexStorage.save();
    }

    public static boolean isTargetedOrSearched(UUID villagerId) {
        // 1. Check if this villager is manually targeted by a timer
        if (targetedVillager != null) {
            if (System.currentTimeMillis() < glowExpiration) {
                if (villagerId.equals(targetedVillager)) {
                    return true;
                }
            } else {
                targetedVillager = null; // Timer expired!
            }
        }

        // 2. If no search query is active, stop checking
        if (searchQuery.isEmpty()) return false;

        IndexedVillager villager = CACHE.get(villagerId);
        if (villager == null) return false;

        // 3. Check if any of the cached trades match the active search query
        String query = searchQuery.toLowerCase();
        for (TradeInfo trade : villager.trades) {
            if (trade.resultSearchKey.contains(query)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMatch(UUID villagerId) {
        // GATEKEEPER: If the user selected Bounding Box ESP, return false here
        // so your old Vanilla outline Mixin ignores this villager completely.
        if (JasperBotConfig.glowStyle != JasperBotConfig.GlowStyle.VANILLA) {
            return false;
        }

        // Otherwise, use our core calculation logic
        return isTargetedOrSearched(villagerId);
    }
}