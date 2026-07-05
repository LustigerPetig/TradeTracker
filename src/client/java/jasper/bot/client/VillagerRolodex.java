package jasper.bot.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.network.chat.Component;

import java.util.*;

public class VillagerRolodex {
    // Stores Villager UUID -> One massive string containing all their item tooltips
    public static final Map<UUID, String> CACHE = new HashMap<>();

    // The current text typed into the search menu
    public static String searchQuery = "";

    // Temporarily holds the UUID of the villager we just right-clicked
    public static UUID lastInteractedVillager = null;

    // Helper to extract text from a villager's trades (like "Enchanted Book Mending I")
    public static void cacheTrades(UUID villagerId, MerchantOffers offers) {
        StringBuilder allTrades = new StringBuilder();

        for (MerchantOffer offer : offers) {
            ItemStack resultItem = offer.getResult();

            // We grab the full tooltip so we can read enchantments, not just the item name!
            List<Component> tooltip = resultItem.getTooltipLines(
                    Item.TooltipContext.of(Minecraft.getInstance().level),
                    Minecraft.getInstance().player,
                    TooltipFlag.NORMAL
            );

            for (Component line : tooltip) {
                allTrades.append(line.getString().toLowerCase()).append(" ");
            }
        }

        CACHE.put(villagerId, allTrades.toString());
    }

    // Helper to check if a villager should glow
    public static boolean isMatch(UUID villagerId) {
        if (searchQuery.isEmpty()) return false;
        String trades = CACHE.get(villagerId);
        boolean result = trades != null && trades.contains(searchQuery.toLowerCase());
        System.out.println("[Rolodex] isMatch check: query=" + searchQuery + ", cached=" + trades + ", result=" + result);
        return result;
    }
}