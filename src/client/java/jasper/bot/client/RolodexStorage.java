package jasper.bot.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public class RolodexStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger("JasperBot");

    private static String getServerIdSanitized() {
        Minecraft client = Minecraft.getInstance();
        String id = "unknown_world";

        if (client.getCurrentServer() != null) {
            id = client.getCurrentServer().ip;
        } else if (client.getSingleplayerServer() != null) {
            id = client.getSingleplayerServer().getWorldData().getLevelName();
        }

        return id.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private static Path getSavePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("jasperbot_rolodex")
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

            for (Map.Entry<UUID, List<VillagerRolodex.TradeInfo>> entry : VillagerRolodex.CACHE.entrySet()) {
                CompoundTag villagerTag = new CompoundTag();

                villagerTag.putString("VillagerID", entry.getKey().toString());

                ListTag tradesList = new ListTag();
                for (VillagerRolodex.TradeInfo trade : entry.getValue()) {
                    CompoundTag tradeTag = new CompoundTag();

                    tradeTag.put("CostA", ItemStack.CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.costA).getOrThrow());
                    tradeTag.put("CostB", ItemStack.CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.costB).getOrThrow());
                    tradeTag.put("Result", ItemStack.CODEC.encodeStart(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), trade.result).getOrThrow());

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

            Optional<ListTag> villagersOpt = root.getList("Villagers");
            if (villagersOpt.isEmpty()) return;
            ListTag villagersList = villagersOpt.get();

            for (int i = 0; i < villagersList.size(); i++) {
                Optional<CompoundTag> villagerTagOpt = villagersList.getCompound(i);
                if (villagerTagOpt.isEmpty()) continue;
                CompoundTag villagerTag = villagerTagOpt.get();

                Optional<String> uuidStrOpt = villagerTag.getString("VillagerID");
                if (uuidStrOpt.isEmpty() || uuidStrOpt.get().isEmpty()) continue;

                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStrOpt.get());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                List<VillagerRolodex.TradeInfo> trades = new ArrayList<>();
                Optional<ListTag> tradesListOpt = villagerTag.getList("Trades");

                if (tradesListOpt.isPresent()) {
                    ListTag tradesList = tradesListOpt.get();
                    for (int j = 0; j < tradesList.size(); j++) {
                        Optional<CompoundTag> tradeTagOpt = tradesList.getCompound(j);
                        if (tradeTagOpt.isEmpty()) continue;
                        CompoundTag tradeTag = tradeTagOpt.get();

                        ItemStack costA = ItemStack.CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tradeTag.getCompound("CostA").orElse(new CompoundTag())).getOrThrow();
                        ItemStack costB = ItemStack.CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tradeTag.getCompound("CostB").orElse(new CompoundTag())).getOrThrow();
                        ItemStack result = ItemStack.CODEC.parse(regs.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tradeTag.getCompound("Result").orElse(new CompoundTag())).getOrThrow();

                        String niceName = tradeTag.getString("NiceName").orElse("Unknown");
                        String searchKey = tradeTag.getString("SearchKey").orElse("");

                        trades.add(new VillagerRolodex.TradeInfo(uuid, costA, costB, result, niceName, searchKey));
                    }
                }
                VillagerRolodex.CACHE.put(uuid, trades);
            }
            LOGGER.info("[Rolodex] Loaded {} villagers for server: {}", VillagerRolodex.CACHE.size(), getServerIdSanitized());

        } catch (Exception e) {
            LOGGER.error("[Rolodex] Failed to load NBT data!", e);
        }
    }
}