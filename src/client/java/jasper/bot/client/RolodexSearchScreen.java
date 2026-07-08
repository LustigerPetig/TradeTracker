package jasper.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
public class RolodexSearchScreen extends Screen {

    private enum SortMode {
        DISTANCE("Sort: Distance"),
        NAME("Sort: Name"),
        PROFESSION("Sort: Profession"),
        COST("Sort: Cost");

        final String label;
        SortMode(String label) { this.label = label; }
        SortMode next() {
            SortMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private record RowEntry(VillagerRolodex.IndexedVillager villager, VillagerRolodex.TradeInfo trade, double distance, boolean isHeader) {}
    private record VillagerGroup(VillagerRolodex.IndexedVillager villager, List<VillagerRolodex.TradeInfo> trades, double distance) {}

    private EditBox searchBox;
    private final List<Button> tradeButtons = new ArrayList<>();
    private int scrollOffset = 0;
    private Button nextButton;
    private Button prevButton;
    private Button sortButton;
    private Button toggleModeButton;
    private Button filterBookButton; // NEW: Book filter button

    private static SortMode sortMode = SortMode.DISTANCE;
    private static boolean groupByVillager = false;
    private static boolean onlyEnchantedBooks = false; // NEW: Static state memory

    private int tradesPerPage = 5;



    private final List<RowEntry> listEntries = new ArrayList<>();

    public RolodexSearchScreen() {
        super(Component.literal("Villager Rolodex"));
    }

    @Override
    protected void init() {
        super.init();

        this.tradeButtons.clear();

        // Adjusted usable height because we added a second row of controls
        int usableHeight = this.height - 135;
        this.tradesPerPage = Math.max(1, usableHeight / 26);

        // TOP ROW: Wider Search box centered at Y = 28
        this.searchBox = new EditBox(this.font, this.width / 2 - 80, 28, 160, 20, Component.literal("Search Trades"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setValue(VillagerRolodex.searchQuery);
        this.searchBox.setResponder(text -> {
            VillagerRolodex.searchQuery = text.trim();
            this.scrollOffset = 0;
            refreshList();
        });

        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        // SECOND ROW: Three buttons evenly spaced at Y = 52

        // 1. View toggle (Left)
        this.toggleModeButton = Button.builder(Component.literal(groupByVillager ? "View: Villagers" : "View: Trades"), btn -> {
            groupByVillager = !groupByVillager;
            btn.setMessage(Component.literal(groupByVillager ? "View: Villagers" : "View: Trades"));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 - 165, 52, 100, 20).build();
        this.addRenderableWidget(this.toggleModeButton);

        // 2. NEW: Book Filter toggle (Center)
        this.filterBookButton = Button.builder(Component.literal(onlyEnchantedBooks ? "Filter: Books" : "Filter: All"), btn -> {
            onlyEnchantedBooks = !onlyEnchantedBooks;
            btn.setMessage(Component.literal(onlyEnchantedBooks ? "Filter: Books" : "Filter: All"));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 - 55, 52, 110, 20).build();
        this.addRenderableWidget(this.filterBookButton);

        // 3. Sort toggle (Right)
        this.sortButton = Button.builder(Component.literal(sortMode.label), btn -> {
            sortMode = sortMode.next();
            btn.setMessage(Component.literal(sortMode.label));
            this.scrollOffset = 0;
            refreshList();
        }).bounds(this.width / 2 + 65, 52, 100, 20).build();
        this.addRenderableWidget(this.sortButton);

        // Bottom Navigation
        this.prevButton = Button.builder(Component.literal("<"), btn -> {
            if (this.scrollOffset > 0) {
                this.scrollOffset--;
                refreshList();
            }
        }).bounds(this.width / 2 - 165, this.height - 28, 20, 20).build();

        this.nextButton = Button.builder(Component.literal(">"), btn -> {
            if ((this.scrollOffset + 1) * this.tradesPerPage < this.listEntries.size()) {
                this.scrollOffset++;
                refreshList();
            }
        }).bounds(this.width / 2 + 145, this.height - 28, 20, 20).build();

        this.addRenderableWidget(this.prevButton);
        this.addRenderableWidget(this.nextButton);

        for (int i = 0; i < this.tradesPerPage; i++) {
            int index = i;
            Button btn = Button.builder(Component.empty(), b -> {
                        int actualIndex = (this.scrollOffset * this.tradesPerPage) + index;
                        if (actualIndex < this.listEntries.size()) {
                            UUID target = this.listEntries.get(actualIndex).villager().uuid;
                            VillagerRolodex.targetedVillager = target;
                            VillagerRolodex.glowExpiration = System.currentTimeMillis() + JasperBotConfig.glowDurationMs;
                            this.onClose();
                        }
                    })
                    // List pushes down to Y = 85 to make room for the new buttons
                    .bounds(this.width / 2 - 165, 85 + (i * 26), 330, 24).build();

            this.tradeButtons.add(btn);
            this.addRenderableWidget(btn);
        }

        refreshList();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        boolean handled = super.mouseClicked(click, doubled);
        if (this.getFocused() != this.searchBox) {
            if (this.getFocused() instanceof Button clickedButton) {
                clickedButton.setFocused(false);
            }
            this.setFocused(this.searchBox);
        }
        return handled;
    }

    private String formatProfessionName(String profession) {
        String key = profession.replace("minecraft:", "");
        if (key.isEmpty()) return "Unknown";
        return key.substring(0, 1).toUpperCase() + key.substring(1).replace("_", " ");
    }

    private ItemStack getWorkstationIcon(String profession) {
        String key = profession.replace("minecraft:", "").toLowerCase();
        return switch (key) {
            case "armorer" -> new ItemStack(Items.BLAST_FURNACE);
            case "butcher" -> new ItemStack(Items.SMOKER);
            case "cartographer" -> new ItemStack(Items.CARTOGRAPHY_TABLE);
            case "cleric" -> new ItemStack(Items.BREWING_STAND);
            case "farmer" -> new ItemStack(Items.COMPOSTER);
            case "fisherman" -> new ItemStack(Items.BARREL);
            case "fletcher" -> new ItemStack(Items.FLETCHING_TABLE);
            case "leatherworker" -> new ItemStack(Items.CAULDRON);
            case "librarian" -> new ItemStack(Items.LECTERN);
            case "mason" -> new ItemStack(Items.STONECUTTER);
            case "shepherd" -> new ItemStack(Items.LOOM);
            case "toolsmith" -> new ItemStack(Items.SMITHING_TABLE);
            case "weaponsmith" -> new ItemStack(Items.GRINDSTONE);
            case "nitwit", "none" -> new ItemStack(Items.VILLAGER_SPAWN_EGG);
            default -> new ItemStack(Items.EMERALD);
        };
    }

    private void refreshList() {
        this.listEntries.clear();
        String query = VillagerRolodex.searchQuery.toLowerCase();

        Set<UUID> nearbyVillagers = new HashSet<>();
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
                if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
                    nearbyVillagers.add(entity.getUUID());
                }
            });
        }

        Minecraft client = Minecraft.getInstance();
        double playerX = client.player != null ? client.player.getX() : 0;
        double playerZ = client.player != null ? client.player.getZ() : 0;

        if (groupByVillager) {
            List<VillagerGroup> groups = new ArrayList<>();

            for (VillagerRolodex.IndexedVillager villager : VillagerRolodex.CACHE.values()) {
                if (!nearbyVillagers.contains(villager.uuid)) continue;

                double villagerX = villager.cordX;
                double villagerZ = villager.cordZ;
                double distance = Math.sqrt(Math.pow(playerX - villagerX, 2) + Math.pow(playerZ - villagerZ, 2));

                List<VillagerRolodex.TradeInfo> matchingTrades = new ArrayList<>();
                for (VillagerRolodex.TradeInfo trade : villager.trades) {
                    // NEW: If the book filter is on, immediately skip any non-enchanted book result
                    if (onlyEnchantedBooks && !trade.result.is(Items.ENCHANTED_BOOK)) continue;

                    if (query.isEmpty() || trade.resultSearchKey.contains(query)) {
                        matchingTrades.add(trade);
                    }
                }

                if (!matchingTrades.isEmpty()) {
                    groups.add(new VillagerGroup(villager, matchingTrades, distance));
                }
            }

            switch (sortMode) {
                case DISTANCE  -> groups.sort(Comparator.comparingDouble(g -> g.distance));
                case NAME      -> groups.sort(Comparator.comparing(g -> g.trades.get(0).niceResultName.toLowerCase()));
                case PROFESSION -> groups.sort(Comparator.comparing(g -> g.villager.profession.toLowerCase()));
                case COST      -> groups.sort(Comparator.comparingInt(g -> g.trades.get(0).localCostA.getCount()));
            }

            for (VillagerGroup group : groups) {
                this.listEntries.add(new RowEntry(group.villager, null, group.distance, true));
                for (VillagerRolodex.TradeInfo trade : group.trades) {
                    this.listEntries.add(new RowEntry(group.villager, trade, group.distance, false));
                }
            }

        } else {
            List<RowEntry> flatTrades = new ArrayList<>();
            for (VillagerRolodex.IndexedVillager villager : VillagerRolodex.CACHE.values()) {
                if (!nearbyVillagers.contains(villager.uuid)) continue;

                double villagerX = villager.cordX;
                double villagerZ = villager.cordZ;
                double distance = Math.sqrt(Math.pow(playerX - villagerX, 2) + Math.pow(playerZ - villagerZ, 2));

                for (VillagerRolodex.TradeInfo trade : villager.trades) {
                    // NEW: Same filter check for flat mode
                    if (onlyEnchantedBooks && !trade.result.is(Items.ENCHANTED_BOOK)) continue;

                    if (query.isEmpty() || trade.resultSearchKey.contains(query)) {
                        flatTrades.add(new RowEntry(villager, trade, distance, false));
                    }
                }
            }

            switch (sortMode) {
                case DISTANCE  -> flatTrades.sort(Comparator.comparingDouble(RowEntry::distance));
                case NAME      -> flatTrades.sort(Comparator.comparing(t -> t.trade().niceResultName.toLowerCase()));
                case PROFESSION -> flatTrades.sort(Comparator.comparing(t -> t.villager().profession.toLowerCase()));
                case COST      -> flatTrades.sort(Comparator.comparingInt(t -> t.trade().localCostA.getCount()));
            }

            this.listEntries.addAll(flatTrades);
        }

        for (int i = 0; i < this.tradesPerPage; i++) {
            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            Button btn = this.tradeButtons.get(i);

            if (actualIndex < this.listEntries.size()) {
                btn.active = true;
                btn.visible = true;

                RowEntry entry = this.listEntries.get(actualIndex);
                if (groupByVillager && !entry.isHeader()) {
                    btn.setX(this.width / 2 - 145);
                    btn.setWidth(310);
                } else {
                    btn.setX(this.width / 2 - 165);
                    btn.setWidth(330);
                }
            } else {
                btn.active = false;
                btn.visible = false;
            }
        }

        this.prevButton.active = this.scrollOffset > 0;
        this.nextButton.active = (this.scrollOffset + 1) * this.tradesPerPage < this.listEntries.size();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        String titleText = "Trade Tracker";
        graphics.text(this.font, titleText, this.width / 2 - this.font.width(titleText) / 2, 12, ARGB.white(1.0F));

        float totalPages = (float) this.listEntries.size() / this.tradesPerPage;
        String pageText = this.listEntries.isEmpty()
                ? "No cached trades nearby!"
                : "Page " + (this.scrollOffset + 1) + " / " + (int) Math.ceil(totalPages);
        graphics.text(this.font, pageText, this.width / 2 - this.font.width(pageText) / 2, this.height - 22, ARGB.white(1.0F));

        for (int i = 0; i < this.tradesPerPage; i++) {
            Button btn = this.tradeButtons.get(i);
            if (!btn.visible) continue;

            int actualIndex = (this.scrollOffset * this.tradesPerPage) + i;
            if (actualIndex >= this.listEntries.size()) continue;

            RowEntry entry = this.listEntries.get(actualIndex);

            int drawX = btn.getX() + 10;
            int drawY = btn.getY() + 4;
            int maxWidth = 0;
            int  nameTextWidth = 0;
            if (entry.isHeader()) {
                ItemStack workstation = getWorkstationIcon(entry.villager().profession);
                graphics.item(workstation, drawX, drawY);
                drawX += 20;
                String cleanName = formatProfessionName(entry.villager().profession);
                graphics.text(this.font, cleanName, drawX, drawY + 4, 0xFFFFD700);
                if (JasperBotConfig.showLvl) {
                    drawX += this.font.width(cleanName) + 2;
                    String LvlTxt = "(Lvl " + String.valueOf(entry.villager().level) + "/5)";
                    graphics.text(this.font, LvlTxt, drawX, drawY + 4, 0xFFFFD700);
                    drawX += this.font.width(LvlTxt) +5;
                }else{
                    drawX += this.font.width(cleanName) + 5;
                }

                maxWidth =btn.getWidth() - (drawX - btn.getX()) - 5;

                if(!List.of("armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "shepherd","toolsmith", "weaponsmith", "nitwit", "villager").contains(entry.villager.nameTag.toLowerCase())){

                   nameTextWidth = this.font.width(entry.villager.nameTag);
                   if(!(nameTextWidth> maxWidth)){
                   graphics.text(this.font,"'" + entry.villager().nameTag + "'", drawX, drawY + 4, ARGB.white(1.0F));
                }else{
                       if(JasperBotConfig.showDistance){
                           maxWidth -= this.font.width(String.format("%.0fm", entry.distance()) + 5);
                       }
                       graphics.enableScissor(drawX, drawY - 2, drawX + maxWidth, drawY + 12);



                       long time = Util.getMillis();
                       int maxScroll = nameTextWidth - maxWidth;


                       double wave = (Math.sin((double) time / JasperBotConfig.scrollSpeed) + 1.0) / 2.0;
                       int scrollOffset = (int) (maxScroll * wave);


                       graphics.text(this.font, entry.villager().nameTag, drawX - scrollOffset, drawY + 4, ARGB.white(1.0F));

                       graphics.disableScissor();
                       }
                }

                if (JasperBotConfig.showDistance) {
                    String distanceText = String.format("%.0fm", entry.distance());
                    int textWidth = this.font.width(distanceText);
                    int rightAlignedX = btn.getX() + btn.getWidth() - textWidth - 5;
                    graphics.text(this.font, distanceText, rightAlignedX, drawY + 4, ARGB.white(1.0F));
                }
            } else {
                VillagerRolodex.TradeInfo trade = entry.trade();
                if (trade == null) continue;

                if (trade.costA.getCount() == trade.localCostA.getCount()) {
                    graphics.item(trade.costA, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.costA, drawX, drawY);
                    drawX += 22;
                } else if (trade.costA.getCount() == 1) {
                    graphics.item(trade.costA, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.costA, drawX, drawY, String.valueOf(trade.costA.getCount()));
                    graphics.fill(drawX + 7, drawY + 12, drawX + 16, drawY + 13, 0xFFBA370F);
                    drawX += 20;
                    graphics.text(this.font, String.valueOf(trade.localCostA.getCount()), drawX, drawY + 9, ARGB.white(1.0f));
                    drawX += 22;

                }else{
                    graphics.item(trade.costA, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.costA, drawX, drawY);
                    graphics.fill(drawX + 7, drawY + 12, drawX + 16, drawY + 13, 0xFFBA370F);
                    drawX += 20;
                    graphics.text(this.font, String.valueOf(trade.localCostA.getCount()), drawX, drawY + 9, ARGB.white(1.0f));
                    drawX += 22;
                }

                if (!trade.costB.isEmpty()) {
                    graphics.text(this.font, "+", drawX, drawY + 4, ARGB.white(1.0F));
                    drawX += 12;
                    graphics.item(trade.costB, drawX, drawY);
                    graphics.itemDecorations(this.font, trade.costB, drawX, drawY);
                    drawX += 22;
                }

                graphics.text(this.font, "->", drawX, drawY + 4, ARGB.white(1.0F));
                drawX += 18;

                graphics.item(trade.result, drawX, drawY);


                graphics.itemDecorations(this.font, trade.result, drawX, drawY);
                drawX += 22;

                maxWidth =btn.getWidth() - (drawX - btn.getX()) - 5;
                nameTextWidth = this.font.width(trade.niceResultName);
                if(!(nameTextWidth> maxWidth)){
                    graphics.text(this.font, trade.niceResultName, drawX, drawY + 4, 0xFF55FF55);
                }else {
                    if (!groupByVillager && JasperBotConfig.showDistance) {
                        maxWidth -= this.font.width(String.format("%.0fm", entry.distance()) + 5);
                    }
                    graphics.enableScissor(drawX, drawY - 2, drawX + maxWidth, drawY + 12);


                    long time = Util.getMillis();
                    int maxScroll = nameTextWidth - maxWidth;

                    double wave = (Math.sin((double) time / JasperBotConfig.scrollSpeed) + 1.0) / 2.0;
                    int scrollOffset = (int) (maxScroll * wave);

                    graphics.text(this.font, trade.niceResultName, drawX - scrollOffset, drawY + 4, 0xFF55FF55);

                    graphics.disableScissor();
                }

                if (!groupByVillager && JasperBotConfig.showDistance) {
                    String distanceText = String.format("%.0fm", entry.distance());
                    int textWidth = this.font.width(distanceText);
                    int rightAlignedX = btn.getX() + btn.getWidth() - textWidth - 5;
                    graphics.text(this.font, distanceText, rightAlignedX, drawY + 4, ARGB.white(1.0F));
                }
            }
        }
    }

    @Override
    public void onClose() {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
                if (entity instanceof net.minecraft.world.entity.npc.villager.Villager) {
                    entity.refreshDimensions();
                }
            });
        }
        super.onClose();
    }
}