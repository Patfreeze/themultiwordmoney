package com.amedacier.themultiworldmoney;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AuctionHouse {
    private Player player;
    private File dataFolder;
    private HashMap<Integer, AuctionItem> a_auctionItems = new HashMap<Integer, AuctionItem>();
    private String sFileName;

    public AuctionHouse(Player p, File df, String sWorldGroup) {
        this.player = p;
        this.dataFolder = df;
        this.sFileName = sWorldGroup;
        this.loadFromFile();
    }

    private static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new HashSet<T>();
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public HashMap<Integer, AuctionItem> getAuctionItems(AhItemsType eType) {

        HashMap<Integer, AuctionItem> a_returnItems = new HashMap<>();

        Calendar calendarCurrent = Calendar.getInstance();
        calendarCurrent.setTimeZone(TimeZone.getTimeZone(TheMultiWorldMoney.sTimezone));

        Calendar calendarCurrentItem = Calendar.getInstance();
        calendarCurrentItem.setTimeZone(TimeZone.getTimeZone(TheMultiWorldMoney.sTimezone));

        for(int acItemKey : a_auctionItems.keySet()) {

            switch(eType.name()) {
                case "ALL":
                    // We put all in the new one
                    a_returnItems.put(acItemKey, a_auctionItems.get(acItemKey));
                    break;

                case "ON_SOLD":
                case "EXPIRED":

                    AuctionItem auctionItem = a_auctionItems.get(acItemKey);
                    calendarCurrentItem.setTimeInMillis(Long.parseLong(auctionItem.getDate()));
                    calendarCurrentItem.add(Calendar.DATE,TheMultiWorldMoney.expirationAuctionDay);

                    // CHECK IF IS EXPIRED OR NOT
                    if(calendarCurrentItem.getTimeInMillis() > calendarCurrent.getTimeInMillis()) {
                        // ON_SOLD
                        if(eType == AhItemsType.ON_SOLD) {
                            a_returnItems.put(acItemKey, a_auctionItems.get(acItemKey));
                        }
                    }
                    else {
                        // EXPIRED && THE PLAYER IS THE OWNER
                        if(eType == AhItemsType.EXPIRED && player.getUniqueId() == auctionItem.getPlayerOwner().getUniqueId()) {
                            a_returnItems.put(acItemKey, a_auctionItems.get(acItemKey));
                        }
                    }

                    break;

                default:
                    TheMultiWorldMoney.LOG.warning(eType.name()+" is not implemented in AuctionHouse");
            }

        }


        return a_returnItems;
    }

    private int getTheNextEmptyId() {
        int nextId = a_auctionItems.size();
        int maxIterate = 1000;
        int iCount = 0;
        while(a_auctionItems.get(nextId) != null) {
            if(iCount > maxIterate) {
                nextId = -1;
                break;
            }

            iCount++;
        }

        return nextId;
    }

    public AuctionItem addAuctionItem(AuctionItem auctionItem) {
        int nextId = getTheNextEmptyId();
        if(nextId == -1) {
            return null; // Unable to find a new number
        }
        return a_auctionItems.putIfAbsent(nextId, auctionItem);
    }

    public boolean removeAuctionItemByItem(AuctionItem auctionItem) {

        Set<Integer> keys = getKeysByValue(a_auctionItems, auctionItem);

        if(keys.size() != 1) {
            return false;
        }

        a_auctionItems.remove(keys.toArray()[0]);
        this.saveOnFile();
        return true;
    }

    public AuctionItem getAuctionItemById(String sId) {
        // No id return false
        if(sId.trim().equalsIgnoreCase("")) {
            return null;
        }

        for(int auctionItemKey : a_auctionItems.keySet()) {
            AuctionItem auctionItem = a_auctionItems.get(auctionItemKey);
            if(auctionItem.getId().contentEquals(sId)) {
                return auctionItem;
            }
        }

        return null;
    }

    public boolean removeAuctionItemByItemId(String sId) {

        // No id return false
        if(sId.trim().equalsIgnoreCase("")) {
            return false;
        }

        // init the key to find
        int iKey = -1;

        for(int auctionItemKey : a_auctionItems.keySet()) {
            AuctionItem auctionItem = a_auctionItems.get(auctionItemKey);
            if(auctionItem.getId().contentEquals(sId)) {
                iKey = auctionItemKey;
                break; // for optimisation, we found it
            }
        }

        // Not found return false
        if(iKey == -1) {
            return false;
        }

        a_auctionItems.remove(iKey);
        this.saveOnFile();
        return true;
    }

    private File getFile() {
        // CREATE PLAYER FILE
        FileConfiguration config = null;

        File file = new File(dataFolder+File.separator+"Auction", this.sFileName+".yml");

        if(!file.exists()){
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public void saveOnFile() {
        // Save data group
        FileConfiguration config = null;
        File file = getFile();
        config = YamlConfiguration.loadConfiguration(file);

        HashMap<Integer, AuctionItem> theAuctionItems = getAuctionItems(AhItemsType.ALL);

        // reset if exist
        config.set("itemStacks", null);

        // loop on each item
        for(int auctionItemKey : theAuctionItems.keySet()) {
            AuctionItem auctionItem = theAuctionItems.get(auctionItemKey);
            config.set("itemStacks."+auctionItemKey+".itemStack", auctionItem.getItemStack());
            config.set("itemStacks."+auctionItemKey+".date", auctionItem.getDate());
            config.set("itemStacks."+auctionItemKey+".ownerId", auctionItem.getPlayerOwner().getUniqueId()+"");
            config.set("itemStacks."+auctionItemKey+".price", auctionItem.getPrice());
            config.set("itemStacks."+auctionItemKey+".id", auctionItem.getId());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFromFile() {
        FileConfiguration config = null;
        File file = getFile();
        config = YamlConfiguration.loadConfiguration(file);

        if(config.isConfigurationSection("itemStacks")) {
            for(String itemKey : config.getConfigurationSection("itemStacks").getKeys(false)) {

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(config.getString("itemStacks."+itemKey+".ownerId")));
                ItemStack itemStack = config.getItemStack("itemStacks."+itemKey+".itemStack");
                double price = config.getDouble("itemStacks."+itemKey+".price");
                String date = config.getString("itemStacks."+itemKey+".date");

                AuctionItem auctionItem = new AuctionItem(offlinePlayer, itemStack, price, date);
                addAuctionItem(auctionItem);

            }
        }

    }

}
