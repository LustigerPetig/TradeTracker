package trade.tracker.client;

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
        public String nameTag;
        public String profession;
        public int level;
        public int cordX;
        public int cordZ;
        public List<TradeInfo> trades;

        public IndexedVillager(UUID uuid,String nameTag, String profession, int level, int cordX, int cordZ, List<TradeInfo> trades) {
            this.uuid = uuid;
            this.nameTag = nameTag;
            this.profession = profession;
            this.level = level;
            this.cordX = cordX;
            this.cordZ = cordZ;
            this.trades = trades;
        }
    }


    public static final Map<UUID, IndexedVillager> CACHE = new HashMap<>();
    public static Villager lastInteractedVillagerEntity = null;
    public static String searchQuery = "";
    public static UUID lastInteractedVillager = null;
    public static UUID targetedVillager = null;
    public static long glowExpiration = 0;


    public static void cacheVillager(UUID villagerId, String nameTag, String profession, int level, int cordX, int cordZ, MerchantOffers offers) {
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

        IndexedVillager indexedVillager = new IndexedVillager(villagerId, nameTag, profession, level, cordX, cordZ, trades);
        CACHE.put(villagerId, indexedVillager);

        RolodexStorage.save();
    }

    public static boolean isTargetedOrSearched(UUID villagerId) {
        if (targetedVillager != null) {
            if (System.currentTimeMillis() < glowExpiration) {
                if (villagerId.equals(targetedVillager)) {
                    return true;
                }
            } else {
                targetedVillager = null;
            }
        }

        if (searchQuery.isEmpty()) return false;

        IndexedVillager villager = CACHE.get(villagerId);
        if (villager == null) return false;
        return false;
    }

    public static boolean isMatch(UUID villagerId) {
        if (TradeTrackerConfig.glowStyle != TradeTrackerConfig.GlowStyle.VANILLA) {
            return false;
        }

        return isTargetedOrSearched(villagerId);
    }
}