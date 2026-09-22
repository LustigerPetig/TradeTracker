package trade.tracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RolodexStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("TradeTracker");
    private static Path configDir;

    /**
     * Call this during mod initialization on each loader to pass its config directory.
     */
    public static void setConfigDir(Path dir) {
        configDir = dir;
    }

    private static String getServerIdSanitized() {
        Minecraft client = Minecraft.getInstance();
        if (client.isSingleplayer()) {
            return "Singleplayer";
        }

        String id = "unknown";
        if (client.getCurrentServer() != null) {
            id = client.getCurrentServer().ip;
        }

        return id.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private static Path getSavePath() {
        Path base = configDir != null ? configDir : Path.of("config");
        return base.resolve("TradeTracker_rolodex")
                .resolve(getServerIdSanitized() + ".dat");
    }

    @SuppressWarnings("unused")
    public static void save() {
        if (VillagerRolodex.CACHE.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        try {
            RegistryAccess regs = client.getConnection().registryAccess();
            CompoundTag root = new CompoundTag();
            ListTag villagersList = new ListTag();

            for (VillagerRolodex.IndexedVillager villager : VillagerRolodex.CACHE.values()) {
                CompoundTag villagerTag = new CompoundTag();

                villagerTag.putString("VillagerID", villager.uuid.toString());
                villagerTag.putString("Nametag", villager.nameTag);
                villagerTag.putString("Profession", villager.profession);
                villagerTag.putInt("Level", villager.level);
                villagerTag.putInt("CordX", villager.cordX);
                villagerTag.putInt("CordZ", villager.cordZ);

                ListTag tradesList = new ListTag();
                for (VillagerRolodex.TradeInfo trade : villager.trades) {
                    CompoundTag tradeTag = new CompoundTag();

                    tradeTag.put("CostA", ItemStack.OPTIONAL_CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.costA).getOrThrow());
                    tradeTag.put("LocalCostA", ItemStack.OPTIONAL_CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.localCostA).getOrThrow());
                    tradeTag.put("CostB", ItemStack.OPTIONAL_CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.costB).getOrThrow());
                    tradeTag.put("Result", ItemStack.OPTIONAL_CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.result).getOrThrow());

                    tradeTag.putString("NiceName", trade.niceResultName);
                    tradeTag.putString("SearchKey", trade.resultSearchKey);
                    tradesList.add(tradeTag);
                }
                villagerTag.put("Trades", tradesList);
                villagersList.add(villagerTag);
            }
            root.put("Villagers", villagersList);

            Path savePath = getSavePath();
            Files.createDirectories(savePath.getParent());
            NbtIo.writeCompressed(root, savePath);

        } catch (Exception e) {
            LOGGER.error("[Rolodex] Failed to save NBT data!", e);
        }
    }

    @SuppressWarnings("unused")
    public static void load() {
        VillagerRolodex.CACHE.clear();
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        Path savePath = getSavePath();
        if (!Files.exists(savePath)) return;

        try {
            RegistryAccess regs = client.getConnection().registryAccess();
            CompoundTag root = NbtIo.readCompressed(savePath, NbtAccounter.unlimitedHeap());

            if (!root.contains("Villagers", Tag.TAG_LIST)) return;
            ListTag villagersList = root.getList("Villagers", Tag.TAG_COMPOUND);

            for (int i = 0; i < villagersList.size(); i++) {
                CompoundTag villagerTag = villagersList.getCompound(i);

                String uuidStr = villagerTag.getString("VillagerID");
                if (uuidStr.isEmpty()) continue;

                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                String nameTag = villagerTag.contains("Nametag") ? villagerTag.getString("Nametag") : "Villager";
                String profession = villagerTag.contains("Profession") ? villagerTag.getString("Profession") : "UNKNOWN";
                int level = villagerTag.contains("Level") ? villagerTag.getInt("Level") : 1;
                int chunkX = villagerTag.contains("ChunkX") ? villagerTag.getInt("ChunkX") : 0;
                int chunkZ = villagerTag.contains("ChunkZ") ? villagerTag.getInt("ChunkZ") : 0;

                List<VillagerRolodex.TradeInfo> trades = new ArrayList<>();
                if (villagerTag.contains("Trades", Tag.TAG_LIST)) {
                    ListTag tradesList = villagerTag.getList("Trades", Tag.TAG_COMPOUND);
                    for (int j = 0; j < tradesList.size(); j++) {
                        CompoundTag tradeTag = tradesList.getCompound(j);

                        ItemStack costA = ItemStack.OPTIONAL_CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tradeTag.getCompound("CostA")).result().orElse(ItemStack.EMPTY);
                        CompoundTag fallbackTag = tradeTag.getCompound("CostA");
                        CompoundTag localCostATag = tradeTag.contains("LocalCostA") ? tradeTag.getCompound("LocalCostA") : fallbackTag;
                        ItemStack localCostA = ItemStack.OPTIONAL_CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), localCostATag).result().orElse(costA);
                        ItemStack costB = ItemStack.OPTIONAL_CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tradeTag.getCompound("CostB")).result().orElse(ItemStack.EMPTY);
                        ItemStack result = ItemStack.OPTIONAL_CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tradeTag.getCompound("Result")).result().orElse(ItemStack.EMPTY);

                        String niceName = tradeTag.contains("NiceName") ? tradeTag.getString("NiceName") : "Unknown";
                        String searchKey = tradeTag.contains("SearchKey") ? tradeTag.getString("SearchKey") : "";

                        trades.add(new VillagerRolodex.TradeInfo(costA, localCostA, costB, result, niceName, searchKey));
                    }
                }
                VillagerRolodex.IndexedVillager indexedVillager = new VillagerRolodex.IndexedVillager(uuid, nameTag, profession, level, chunkX, chunkZ, trades);
                VillagerRolodex.CACHE.put(uuid, indexedVillager);
            }
            LOGGER.info("[Rolodex] Loaded {} villagers for server: {}", VillagerRolodex.CACHE.size(), getServerIdSanitized());

        } catch (Exception e) {
            LOGGER.error("[Rolodex] Failed to load NBT data!", e);
        }
    }
}