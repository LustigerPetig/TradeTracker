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

    // NEW: A dedicated object to hold separated trade data!
    public static class TradeInfo {
        public final UUID villagerId;
        public final String displayString;
        public final String resultSearchKey;

        public TradeInfo(UUID villagerId, String displayString, String resultSearchKey) {
            this.villagerId = villagerId;
            this.displayString = displayString;
            // Force lowercase so our search is never case-sensitive
            this.resultSearchKey = resultSearchKey.toLowerCase();
        }
    }

    // Our cache now stores a List of TradeInfo objects for each UUID
    public static final Map<UUID, List<TradeInfo>> CACHE = new HashMap<>();

    public static String searchQuery = "";
    public static UUID lastInteractedVillager = null;
    public static UUID targetedVillager = null;
    public static long glowExpiration = 0;

    public static void cacheTrades(UUID villagerId, MerchantOffers offers) {
        List<TradeInfo> trades = new ArrayList<>();

        for (MerchantOffer offer : offers) {
            ItemStack costA = offer.getBaseCostA();
            ItemStack costB = offer.getCostB();
            ItemStack result = offer.getResult();

            // 1. Format the Input Cost (What you give)
            String costStr = getCompactName(costA);
            if (!costB.isEmpty()) {
                costStr += " + " + getCompactName(costB);
            }

            // 2. Format the Output Result (What you get)
            String resultStr = getCompactName(result);

            // 3. Combine them for the UI button
            String display = costStr + " -> " + resultStr;

            // 4. Build a hidden search key from ALL tooltip lines of the RESULT ONLY
            StringBuilder searchKey = new StringBuilder();
            List<Component> tooltips = result.getTooltipLines(
                    Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    TooltipFlag.NORMAL
            );
            for (Component line : tooltips) {
                searchKey.append(line.getString()).append(" ");
            }

            trades.add(new TradeInfo(villagerId, display, searchKey.toString()));
        }

        CACHE.put(villagerId, trades);
    }

    // Helper method to make item text look nice (e.g. "24x Emerald" or "1x Enchanted Book (Mending I)")
    private static String getCompactName(ItemStack stack) {
        if (stack.isEmpty()) return "";

        List<Component> tooltip = stack.getTooltipLines(
                Item.TooltipContext.of(Minecraft.getInstance().level),
                Minecraft.getInstance().player,
                TooltipFlag.NORMAL
        );

        if (tooltip.isEmpty()) return stack.getCount() + "x Unknown";

        String name = stack.getCount() + "x " + tooltip.get(0).getString();

        // Magic hack: If it's an Enchanted Book, append the enchantment name from line 2
        if (name.contains("Enchanted Book") && tooltip.size() > 1) {
            name += " (" + tooltip.get(1).getString() + ")";
        }

        return name;
    }

    public static boolean isMatch(UUID villagerId) {
        // 1. If we clicked a specific trade button, ONLY glow that targeted villager
        if (targetedVillager != null) {
            if (System.currentTimeMillis() < glowExpiration) {
                return villagerId.equals(targetedVillager);
            } else {
                targetedVillager = null; // 10 seconds is up, turn off the target lock
            }
        }

        // 2. Normal generic search bar behavior
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