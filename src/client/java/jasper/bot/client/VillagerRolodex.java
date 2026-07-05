package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.*;

public class VillagerRolodex {

    // We now store the physical ItemStacks!
    public static class TradeInfo {
        public final UUID villagerId;
        public final ItemStack costA;
        public final ItemStack costB;
        public final ItemStack result;
        public final String niceResultName; // Text to display next to the result item
        public final String resultSearchKey;

        public TradeInfo(UUID villagerId, ItemStack costA, ItemStack costB, ItemStack result, String niceResultName, String resultSearchKey) {
            this.villagerId = villagerId;
            this.costA = costA;
            this.costB = costB;
            this.result = result;
            this.niceResultName = niceResultName;
            this.resultSearchKey = resultSearchKey.toLowerCase(); // Lowercase for easy searching
        }
    }

    public static final Map<UUID, List<TradeInfo>> CACHE = new HashMap<>();

    public static String searchQuery = "";
    public static UUID lastInteractedVillager = null;
    public static UUID targetedVillager = null;
    public static long glowExpiration = 0;

    public static void cacheTrades(UUID villagerId, MerchantOffers offers) {
        List<TradeInfo> trades = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            // MUST copy the stacks so they don't despawn when the menu closes
            ItemStack costA = offer.getBaseCostA().copy();
            ItemStack costB = offer.getCostB().copy();
            ItemStack result = offer.getResult().copy();

            // Get a clean name for the UI (e.g. "Enchanted Book")
            String niceName = result.getHoverName().getString();

            List<Component> tooltips = result.getTooltipLines(
                    Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    TooltipFlag.NORMAL
            );

            // Magic hack: If it's an Enchanted Book, steal the enchant name from the tooltip!
            if (niceName.contains("Enchanted Book") && tooltips.size() > 1) {
                niceName += " (" + tooltips.get(1).getString() + ")";
            }

            // Build the hidden search key using ALL text on the result item
            StringBuilder searchKey = new StringBuilder();
            for (Component line : tooltips) {
                searchKey.append(line.getString()).append(" ");
            }

            trades.add(new TradeInfo(villagerId, costA, costB, result, niceName, searchKey.toString()));
        }

        CACHE.put(villagerId, trades);
    }

    public static boolean isMatch(UUID villagerId) {
        if (targetedVillager != null) {
            if (System.currentTimeMillis() < glowExpiration) {
                return villagerId.equals(targetedVillager);
            } else {
                targetedVillager = null;
            }
        }

        if (searchQuery.isEmpty()) return false;

        List<TradeInfo> trades = CACHE.get(villagerId);
        if (trades == null) return false;

        String query = searchQuery.toLowerCase();
        for (TradeInfo trade : trades) {
            if (trade.resultSearchKey.contains(query)) {
                return true;
            }
        }
        return false;
    }
}