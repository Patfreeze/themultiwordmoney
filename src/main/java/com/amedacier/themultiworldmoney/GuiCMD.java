package com.amedacier.themultiworldmoney;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

import java.util.ArrayList;

public class GuiCMD {

    Player player;
    String sType;
    Inventory inventoryBoxShop;
    Location location;

    private static final String sColorGold = "§6";
    private static final String sColorRed = "§4";
    private static final String sColorGreen = "§2";
    private static final String sColorObfuscate = "§0§k";
    private Material materialDefault = Material.GLASS_PANE;

    private static final DecimalFormat df = new DecimalFormat("0.00");

    /*
        // DecimalFormat, default is RoundingMode.HALF_EVEN
        df.setRoundingMode(RoundingMode.DOWN);
        println("\ndouble (RoundingMode.DOWN) : " + df.format(input));  //3.14

        df.setRoundingMode(RoundingMode.UP);
        println("double (RoundingMode.UP)  : " + df.format(input));    //3.15
     */

    public GuiCMD(Player player, String sType, Location loc) {
        this.player = player;
        this.sType = sType;
        this.location = loc;
        String sLocation = loc.getBlockX()+"l"+loc.getBlockY()+"l"+loc.getBlockZ();
        inventoryBoxShop = Bukkit.getServer().createInventory(player, this.getSize(), TheMultiWorldMoney.sPluginNameNoColor+":"+sType+":"+sLocation);
    }

    private int getSize() {
        // Multiple of 9 so : 9, 18, 27,  36, 45, max 54
        switch(this.sType) {

            case "ahExpired":
            case "auctionHouse":
            case "adminShop":
            case "refresh54":
                return 54;

            case "addOrRemoveItems":
                return 27;

            case "confirmAuction":
            case "changeShopItem":
            default:
               return 9;
        }

    }

    private void placeVoidItemsMaterial(Material material) {
        ArrayList<String> lore = new ArrayList<String>();
        for(int i=0; i<this.getSize(); i++) {
            placeItem(i, material, " ", lore, 1, false);
        }
    }

    private void placeVoidItems() {
        ArrayList<String> lore = new ArrayList<String>();
        for(int i=0; i<this.getSize(); i++) {
            placeItem(i, materialDefault, " ", lore, 1, false);
        }
    }

    private void placeItem(int position, ItemStack itemStack, String displayName, ArrayList<String> lore) {

        ItemMeta metaRef = itemStack.getItemMeta();

        // Set Lore to Item
        metaRef.setLore(lore);
        metaRef.setDisplayName(displayName);

        itemStack.setItemMeta(metaRef);
        inventoryBoxShop.setItem(position, itemStack);
    }

    private void placeItem(int position, Material material, String displayName, ArrayList<String> lore, int amount, boolean bEnchanted) {

        ItemStack ref1 = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta metaref1 = ref1.getItemMeta();

        ref1 = new ItemStack(material);
        ref1.setAmount(amount);

        // Set Lore to Item
        metaref1.setLore(lore);
        metaref1.setDisplayName(displayName);

        ref1.setItemMeta(metaref1);
        if(bEnchanted) {
            ref1.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 5);
        }
        inventoryBoxShop.setItem(position, ref1);
    }

    private void getAddOrRemoveItems(ShopAdmin shopAdmin) {
        ArrayList<String> lore = new ArrayList<>();
        lore.add(sColorRed+TheMultiWorldMoney.getTranslatedKeys("removeLeftClick"));
        lore.add(sColorGreen+TheMultiWorldMoney.getTranslatedKeys("addRightClick"));

        placeItem(0, Material.BAMBOO, TheMultiWorldMoney.getTranslatedKeys("goBack"), null, 1, false);

        placeItem(11, shopAdmin.getItemStack().getType(), "", lore, 1, false);
        placeItem(12, shopAdmin.getItemStack().getType(), "", lore, 8, false);
        placeItem(13, shopAdmin.getItemStack().getType(), "", lore, 16, false);
        placeItem(14, shopAdmin.getItemStack().getType(), "", lore, 32, false);
        placeItem(15, shopAdmin.getItemStack().getType(), "", lore, 64, false);

        lore = new ArrayList<>();
        placeItem(18, Material.STRUCTURE_VOID, TheMultiWorldMoney.getTranslatedKeys("close"), lore, 1, false);

        lore = new ArrayList<>();
        lore.add("QTS: "+shopAdmin.getQuantity());
        placeItem(26, Material.CHEST, sColorGreen+TheMultiWorldMoney.getTranslatedKeys("info"), lore, 1, false);
    }

    private void getChangeShopItem(ShopAdmin shopAdmin) {
        ArrayList<String> lore = new ArrayList<>();

        // Go Back
        placeItem(0, Material.BAMBOO, TheMultiWorldMoney.getTranslatedKeys("goBack"), null, 1, false);

        // Close
        lore = new ArrayList<>();
        placeItem(8, Material.STRUCTURE_VOID, TheMultiWorldMoney.getTranslatedKeys("close"), lore, 1, false);

        lore = new ArrayList<>();
        lore.add(sColorGreen+TheMultiWorldMoney.getTranslatedKeys("clickItemYourInventory"));
        ItemStack itemStackShow = shopAdmin.getItemStack().clone();

        placeItem(4, itemStackShow, "", lore);
    }

    private void getAhExpired(AuctionHouse auctionHouse, int iPage) {
        // Init Lore for any Item (reset on each item)
        ArrayList<String> lore;

        // SEPARATION
        ItemStack itemStackSeparator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack itemStackSeparator2 = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        for(int i=36; i<=44; i++) {
            placeItem(i, itemStackSeparator, " ", null);
            placeItem(i+9, itemStackSeparator2, " ", null);
        }

        /////////////////////////////////////////////////////////
        // SECTION ITEMS / max 45 per page 0 to 44
        /////////////////////////////////////////////////////////

        //separate items per page
        HashMap<Integer, AuctionItem> a_auctionItems = auctionHouse.getAuctionItems(AhItemsType.EXPIRED);

        int iMaxPerPage = 36;
        int iMaxPage = 1;
        if(a_auctionItems.size() > iMaxPerPage) {
            iMaxPage = (int) Math.ceil(a_auctionItems.size() / 36.0);
        }

        int iSmallestItem = (iPage*iMaxPerPage)-iMaxPerPage;
        int iLargestItem = iPage*iMaxPerPage;

        int iPos = 0;
        int iItemCount = 0;
        for(int auctionItemKey : a_auctionItems.keySet()) {

            if(iItemCount >= iSmallestItem && iItemCount < iLargestItem) {
                lore = new ArrayList<>();
                AuctionItem auctionItem = a_auctionItems.get(auctionItemKey);

                // First line an id key string to remove item
                lore.add(sColorObfuscate+auctionItem.getId());
                lore.add(sColorGold+TheMultiWorldMoney.getTranslatedKeys("price")+": " + TheMultiWorldMoney.econ.format(auctionItem.getPrice()));
                lore.add(TheMultiWorldMoney.getTranslatedKeys("owner")+": " + auctionItem.getPlayerOwner().getName());
                lore.add(sColorRed+TheMultiWorldMoney.getTranslatedKeys("expiration")+": " + auctionItem.getExpirationDate()); // Calculate time remain
                placeItem(iPos, auctionItem.getItemStack(), "", lore);
                iPos++;
            }
            iItemCount++;

        }

        /////////////////////////////////////////////////////////
        // SECTION MENU BOTTOM
        /////////////////////////////////////////////////////////

        ItemStack itemStackMenu;

        // Go back
        lore = new ArrayList<>();
        placeItem(45, Material.BAMBOO, TheMultiWorldMoney.getTranslatedKeys("goBack"), null, 1, false);

        // Close
        lore = new ArrayList<>();
        placeItem(49, Material.STRUCTURE_VOID, TheMultiWorldMoney.getTranslatedKeys("close"), lore, 1, false);

        // Pages <- 1 ->
        lore = new ArrayList<>();
        lore.add(TheMultiWorldMoney.getTranslatedKeys("clickLeft"));
        itemStackMenu = new ItemStack(Material.PAPER);
        if(iPage != 1) {
            placeItem(52, itemStackMenu, "§lPage " + (iPage - 1), lore);
        }

        if(iPage < iMaxPage) {
            placeItem(53, itemStackMenu, "§lPage " + (iPage+1), lore);
        }

    }

    private void getAuctionHouse(AuctionHouse auctionHouse, int iPage) {
        // Init Lore for any Item (reset on each item)
        ArrayList<String> lore;

        // SEPARATION
        ItemStack itemStackSeparator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack itemStackSeparator2 = new ItemStack(Material.BROWN_STAINED_GLASS_PANE);
        for(int i=36; i<=44; i++) {
            placeItem(i, itemStackSeparator, " ", null);
            placeItem(i+9, itemStackSeparator2, " ", null);
        }

        /////////////////////////////////////////////////////////
        // SECTION ITEMS / max 45 per page 0 to 44
        /////////////////////////////////////////////////////////
        
        //separate items per page
        HashMap<Integer, AuctionItem> a_auctionItems = auctionHouse.getAuctionItems(AhItemsType.ON_SOLD);

        int iMaxPerPage = 36;
        int iMaxPage = 1;
        if(a_auctionItems.size() > iMaxPerPage) {
            iMaxPage = (int) Math.ceil(a_auctionItems.size() / 36.0);
        }

        int iSmallestItem = (iPage*iMaxPerPage)-iMaxPerPage;
        int iLargestItem = iPage*iMaxPerPage;

        System.out.println("SizeItems: "+a_auctionItems.size());

        System.out.println("iSmallestItem: "+iSmallestItem);
        System.out.println("iLargestItem: "+iLargestItem);


        int iPos = 0;
        int iItemCount = 0;
        for(int auctionItemKey : a_auctionItems.keySet()) {

            if(iItemCount >= iSmallestItem && iItemCount < iLargestItem) {
                lore = new ArrayList<>();
                AuctionItem auctionItem = a_auctionItems.get(auctionItemKey);

                // First line an id key string to remove item
                lore.add(sColorObfuscate+auctionItem.getId());
                lore.add(sColorGold+TheMultiWorldMoney.getTranslatedKeys("price")+": " + TheMultiWorldMoney.econ.format(auctionItem.getPrice()));
                lore.add(TheMultiWorldMoney.getTranslatedKeys("owner")+": " + auctionItem.getPlayerOwner().getName());
                lore.add(sColorRed+TheMultiWorldMoney.getTranslatedKeys("expiration")+": " + auctionItem.getExpirationDate()); // Calculate time remain
                placeItem(iPos, auctionItem.getItemStack(), "", lore);
                iPos++;
            }
            iItemCount++;

        }

        /////////////////////////////////////////////////////////
        // SECTION MENU BOTTOM
        /////////////////////////////////////////////////////////

        // Info Player
        lore = new ArrayList<>();
        lore.add(sColorGreen+"§lG:§r "+sColorGreen+TheMultiWorldMoney.capitalize(auctionHouse.getGroupName()));
        lore.add(sColorGold+"§lBalance:§r "+sColorGold+TheMultiWorldMoney.econ.format(TheMultiWorldMoney.econ.getBalance(player)));
        ItemStack itemStackMenu = new ItemStack(Material.GOLD_NUGGET);
        placeItem(45, itemStackMenu, "§lINFO", lore);

        // Item Expired
        lore = new ArrayList<>();
        lore.add(TheMultiWorldMoney.getTranslatedKeys("clickLeft"));
        int iExpiredItem = auctionHouse.getAuctionItems(AhItemsType.EXPIRED).size();
        placeItem(46, Material.ENDER_CHEST, sColorRed+"§l"+TheMultiWorldMoney.getTranslatedKeys("expiredItems")+"§r§c ("+iExpiredItem+")", lore, iExpiredItem == 0 ? 1 : iExpiredItem, true);

        // Refresh
        lore = new ArrayList<>();
        lore.add(sColorGreen+TheMultiWorldMoney.getTranslatedKeys("itemInHand"));
        lore.add("/auction sell "+sColorGold+"[PRICE] "+sColorGreen+"[QTS]");
        lore.add("/ah sell "+sColorGold+"[PRICE] "+sColorGreen+"[QTS]");
        lore.add("/hdv sell "+sColorGold+"[PRICE] "+sColorGreen+"[QTS]");
        itemStackMenu = new ItemStack(Material.SUNFLOWER);
        placeItem(49, itemStackMenu, sColorRed+"§l"+TheMultiWorldMoney.getTranslatedKeys("refresh"), lore);


        // Pages <- 1 ->
        itemStackMenu = new ItemStack(Material.PAPER);
        if(iPage != 1) {
            lore = new ArrayList<>();
            lore.add(TheMultiWorldMoney.getTranslatedKeys("clickLeft"));
            lore.add((iPage - 1)+"");
            placeItem(52, itemStackMenu, "§lPage " + (iPage - 1), lore);
        }

        if(iPage < iMaxPage) {
            lore = new ArrayList<>();
            lore.add(TheMultiWorldMoney.getTranslatedKeys("clickLeft"));
            lore.add((iPage + 1)+"");
            placeItem(53, itemStackMenu, "§lPage " + (iPage+1), lore);
        }

    }

    private void getAdminShop(ShopAdmin shopAdmin) {

        Boolean isAdmin = false;
        if(player.hasPermission("themultiworldmoney.admin") || player.isOp()) {
            isAdmin = true;
        }

        Boolean isOwner = false;
        if(shopAdmin.isShopOwner(player)) {
            isOwner = true;
        }

        // Init Lore for any Item (reset on each item)
        ArrayList<String> lore;

        /////////////////////////////////////////////////////////
        // SECTION SEPARATOR
        /////////////////////////////////////////////////////////
        ItemStack itemStackSeparator = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        for(int i=9; i<=17; i++) {
            placeItem(i, itemStackSeparator, " ", null);
        }
        placeItem(22, itemStackSeparator, "", null);
        placeItem(31, itemStackSeparator, "", null);
        placeItem(40, itemStackSeparator, "", null);
        placeItem(49, itemStackSeparator, "", null);


        /////////////////////////////////////////////////////////
        // SECTION ITEM
        /////////////////////////////////////////////////////////

        // Get the ItemStack from the shopAdmin
        ItemStack itemStackShop = shopAdmin.getItemStack().clone();
        String sDisplayName = "§9"+itemStackShop.getType().name()+" §ax §91";

        // Check if this shop is infinite and if not check if we have any item left
        boolean bOutOfStock = false;
        int iQts = shopAdmin.getQuantity();
        boolean hasInfinity = shopAdmin.hasInfinity();
        if(!hasInfinity && iQts == 0) {
            lore = new ArrayList<>();
            sDisplayName = TheMultiWorldMoney.getTranslatedKeys("outOfOrder");
            lore.add("§4Shop "+sDisplayName);
            bOutOfStock = true;
        }
        else {
            // Else we show the itemStack with lore of selling/buying price
            // NOTE: No need to show enchantement, already added by the game :)
            df.setRoundingMode(RoundingMode.UP);
            lore = new ArrayList<>();
            lore.add("§2Sell: "+df.format(shopAdmin.getSellPrice()));
            lore.add("§cBuy: "+df.format(shopAdmin.getBuyPrice()));
            lore.add("§7Qts: "+(hasInfinity ? "-"  : iQts));
        }

        lore.add("§9"+TheMultiWorldMoney.getTranslatedKeys("owner")+": §a"+shopAdmin.getPlayerOwner().getName());

        boolean bOutOfMoney = false;
        double balanceShop = shopAdmin.getBalance();
        if(!hasInfinity && balanceShop == 0) {
            bOutOfMoney = true;
        }

        if(isOwner) {
            lore.add("§4"+TheMultiWorldMoney.getTranslatedKeys("changeItem"));
        }
        // We place the itemStack in the middle
        placeItem(4, itemStackShop, sDisplayName, lore); // position 4 is the middle one

        /////////////////////////////////////////////////////////
        // SECTION ACTION : BUY
        /////////////////////////////////////////////////////////
        Material materialBuy = Material.LIME_CONCRETE;
        String sOutOfStock = "";
        if(bOutOfStock) {
            materialBuy = Material.STRUCTURE_VOID;
            sOutOfStock = "§4"+TheMultiWorldMoney.getTranslatedKeys("outOfOrder");
        }

        ItemStack itemStackBuy = new ItemStack(materialBuy, 1);
        String sBuy = TheMultiWorldMoney.getTranslatedKeys("buy");

        // POSITION CHEST : QTS
        HashMap<Integer, Integer> a_sBuyItem = new HashMap<>();
        a_sBuyItem.put(18,1);
        a_sBuyItem.put(19,8);
        a_sBuyItem.put(20,32);
        a_sBuyItem.put(21,64);

        int maxQts = itemStackShop.getMaxStackSize();

        for(int iKey : a_sBuyItem.keySet()) {
            int iNumber = a_sBuyItem.get(iKey);

            if(iNumber > maxQts) {
                iNumber = maxQts;
            }

            lore = new ArrayList<>();
            Material sMaterial = Material.STRUCTURE_VOID;

            if(iQts >= iNumber || hasInfinity) {
                sMaterial = itemStackBuy.getType();
                itemStackBuy.setAmount(1);
                lore.add("§2" + sBuy + " "+iNumber+": " + df.format(shopAdmin.getBuyPrice() * iNumber));
            }
            else {
                lore.add(sOutOfStock);
            }
            placeItem(iKey, sMaterial, sBuy+" "+iNumber, lore, iNumber, true);
        }

        /////////////////////////////////////////////////////////
        // SECTION ACTION : SELL
        /////////////////////////////////////////////////////////
        Material materialSell = Material.ORANGE_CONCRETE;
        String sOutOfMoney = "";
        if(bOutOfMoney) {
            sOutOfMoney = sColorRed+TheMultiWorldMoney.getTranslatedKeys("outOfMoney");
            materialSell = Material.STRUCTURE_VOID;
        }
        String sSell = TheMultiWorldMoney.getTranslatedKeys("sell");

        // POSITION CHEST : QTS
        HashMap<Integer, Integer> a_sSellItem = new HashMap<>();
        a_sSellItem.put(23,1);
        a_sSellItem.put(24,8);
        a_sSellItem.put(25,32);
        a_sSellItem.put(26,64);

        double sellPrice = 0;
        for(int iKey : a_sSellItem.keySet()) {
            lore = new ArrayList<>();
            int iNumber = a_sSellItem.get(iKey);
            sellPrice = shopAdmin.getSellPrice()*iNumber;
            lore.add("§2"+sSell+" "+iNumber+": "+df.format(sellPrice));
            if(balanceShop < sellPrice && !hasInfinity) {
                lore.add(sOutOfMoney);
            }
            placeItem(iKey, materialSell, sSell + " "+iNumber, lore, iNumber, true);
        }

        /////////////////////////////////////////////////////////
        // Close Interface
        /////////////////////////////////////////////////////////
        lore = new ArrayList<>();
        placeItem(49, Material.STRUCTURE_VOID, TheMultiWorldMoney.getTranslatedKeys("close"), lore, 1, false);

        /////////////////////////////////////////////////////////
        // Admin stuff here
        /////////////////////////////////////////////////////////
        if(isOwner || isAdmin) {

            // Change Price Buy/sell 9 - 17
            lore = new ArrayList<>();
            lore.add("§4"+TheMultiWorldMoney.getTranslatedKeys("changeBuyingPrice"));
            placeItem(9, Material.GREEN_CANDLE, "$$$", lore, 1, true);

            lore = new ArrayList<>();
            lore.add("§2"+TheMultiWorldMoney.getTranslatedKeys("changeSellingPrice"));
            placeItem(17, Material.ORANGE_CANDLE, "$$$", lore, 1, true);



            // ADD BALANCE AND QTS IF IT'S NOT OP
            if(!shopAdmin.hasInfinity()) {


                lore = new ArrayList<>();
                df.setRoundingMode(RoundingMode.UP);
                sDisplayName = "§9Balance: "+sColorGold+df.format(shopAdmin.getBalance());
                placeItem(45, Material.SUNFLOWER, sDisplayName, lore, 1, true);

                lore = new ArrayList<>();
                lore.add("§4"+TheMultiWorldMoney.getTranslatedKeys("removeLeftClick")+" "+TheMultiWorldMoney.getTranslatedKeys("money"));
                lore.add("§2"+TheMultiWorldMoney.getTranslatedKeys("addRightClick")+" "+TheMultiWorldMoney.getTranslatedKeys("money"));

                sDisplayName = "§9Balance  -+"+sColorGold+"10";
                itemStackShop.setType(Material.GOLD_NUGGET);
                itemStackShop.setAmount(1);
                placeItem(46, itemStackShop, sDisplayName, lore);

                sDisplayName = "§9Balance  -+"+sColorGold+"100";
                itemStackShop.setType(Material.GOLD_INGOT);
                itemStackShop.setAmount(1);
                placeItem(47, itemStackShop, sDisplayName, lore);

                sDisplayName = "§9Balance  -+"+sColorGold+"1000";
                itemStackShop.setType(Material.GOLD_BLOCK);
                itemStackShop.setAmount(1);
                placeItem(48, itemStackShop, sDisplayName, lore);

                lore = new ArrayList<>();
                lore.add("§9qts: "+shopAdmin.getQuantity());
                placeItem(50, Material.CHEST_MINECART, TheMultiWorldMoney.getTranslatedKeys("addOrRemoveItems"), lore, 1, false);
            }


            // FLAG OP INFINITY OR NOT ONLY ADMIN
            if(isAdmin) {
                sDisplayName = "§4OP shop?";
                // Boolean to change the shop as infinite or not
                ItemStack itemStackFlag = new ItemStack(Material.LIGHT_GRAY_BANNER, 1);
                lore = new ArrayList<>();
                lore.add("§3"+TheMultiWorldMoney.getTranslatedKeys("normalShop"));
                lore.add("§2"+TheMultiWorldMoney.getTranslatedKeys("turnOnInfinity"));
                lore.add("§7"+TheMultiWorldMoney.getTranslatedKeys("turnOnInfinityInfo"));
                if(shopAdmin.hasInfinity()) {
                    itemStackFlag.setType(Material.GREEN_BANNER);
                    lore = new ArrayList<>();
                    lore.add("§3"+TheMultiWorldMoney.getTranslatedKeys("OPShop"));
                    lore.add("§4"+TheMultiWorldMoney.getTranslatedKeys("turnOffInfinity"));
                    lore.add("§7"+TheMultiWorldMoney.getTranslatedKeys("turnOffInfinityInfo"));
                }
                placeItem(53, itemStackFlag, sDisplayName, lore); // position 53 the last one
            }

        }
    }

    private void scheduleRefresh(Plugin plugin, Material material, int iTimer) {

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                placeVoidItemsMaterial(material);
            }
        }, iTimer);
    }

    private void getRefresh54(Plugin plugin, Runnable runnable) {

        Material matChange = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

        scheduleRefresh(plugin, matChange, 2);
        scheduleRefresh(plugin, materialDefault, 4);
        scheduleRefresh(plugin, matChange, 6);
        scheduleRefresh(plugin, materialDefault, 8);
        scheduleRefresh(plugin, matChange, 10);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }, 12);

    }

    private void getConfirmAuction(AuctionItem auctionItem) {
        ArrayList<String> lore;

        lore = new ArrayList<>();
        String sConfirm = "§a"+TheMultiWorldMoney.getTranslatedKeys("confirm");
        if(auctionItem.getPlayerOwner().getUniqueId() == player.getUniqueId()) {
            sConfirm = "§a"+TheMultiWorldMoney.getTranslatedKeys("remove");
        }
        placeItem(0, Material.GREEN_STAINED_GLASS_PANE, sConfirm, lore, 1, false);
        placeItem(1, Material.GREEN_STAINED_GLASS_PANE, sConfirm, lore, 1, false);
        placeItem(2, Material.GREEN_STAINED_GLASS_PANE, sConfirm, lore, 1, false);

        // First line an id key string to remove item
        lore = new ArrayList<>();
        lore.add(sColorObfuscate+auctionItem.getId());
        lore.add(sColorGold+TheMultiWorldMoney.getTranslatedKeys("price")+": " + TheMultiWorldMoney.econ.format(auctionItem.getPrice()));
        lore.add(TheMultiWorldMoney.getTranslatedKeys("owner")+": " + auctionItem.getPlayerOwner().getName());
        lore.add(sColorRed+TheMultiWorldMoney.getTranslatedKeys("expiration")+": " + auctionItem.getExpirationDate()); // Calculate time remain
        placeItem(4, auctionItem.getItemStack(), "", lore);

        lore = new ArrayList<>();
        String sCancel = "§c"+TheMultiWorldMoney.getTranslatedKeys("cancel");
        placeItem(6, Material.RED_STAINED_GLASS_PANE, sCancel, lore, 1, false);
        placeItem(7, Material.RED_STAINED_GLASS_PANE, sCancel, lore, 1, false);
        placeItem(8, Material.RED_STAINED_GLASS_PANE, sCancel, lore, 1, false);
    }

    public void render(Object myObject) {
        render(myObject, 0, null);
    }

    public void render(Object myObject, int iPage) {
        render(myObject, iPage, null);
    }

    public void render(Object myObject, int iPage, Plugin plugin) {
        if(player != null) {
            // First we place all void items
            placeVoidItems();

            switch(sType) {
                case "addOrRemoveItems":
                    getAddOrRemoveItems((ShopAdmin) myObject);
                    break;

                case "adminShop":
                    getAdminShop((ShopAdmin) myObject);
                    break;

                case "auctionHouse":
                    getAuctionHouse((AuctionHouse) myObject, iPage);
                    break;

                case "ahExpired":
                    getAhExpired((AuctionHouse) myObject, iPage);
                    break;

                case "changeShopItem":
                    getChangeShopItem((ShopAdmin) myObject);
                    break;

                case "confirmAuction":
                    getConfirmAuction((AuctionItem) myObject);
                    break;

                case "refresh54":
                    if(myObject instanceof Runnable) {
                        getRefresh54(plugin, (Runnable) myObject);
                    }
                    break;
            }

            //Here opens the inventory
            player.openInventory(inventoryBoxShop);
        }
    }

}