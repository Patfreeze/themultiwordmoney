package com.amedacier.themultiworldmoney;

import net.ess3.api.events.UserBalanceUpdateEvent;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;

import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class TheMultiWorldMoney extends JavaPlugin implements Listener {

    static String sTimezone = "America/New_York";

    // 2.3.5
    /*
        - Force EssentialX to hook on event UserBalanceUpdateEvent to update the balance in the good group (player offline)
        With this event, no need anymore the auto-save.
        - Removing the auto-save, useless now
        - Adding SHOP admin/player handled per group of world
        - Adding AuctionHouse to sell/buy stuff player handled per group of world
        - Adding some missing translation
        - Some bugs fix

     */

    // 2.3.4 lastest

    // VAULT
    public static Economy econ = null;
    private static Chat chat = null;
    private static Permission perms = null;

    private static final DecimalFormat df = new DecimalFormat("0.00");

    String sPluginName = "§c[§eTheMultiWorldMoney - TMWM§c] "; // PlugIn Name in Yellow
    public static final String sPluginNameNoColor = "TheMultiWorldMoney";
    String sErrorColor = "§c"; // LightRed
    String sObjectColor = "§a"; // LightGreen
    String sCorrectColor = "§2"; // Green
    String sYellowColor = "§e"; // Yellow
    String sOrangeColor = "§6"; // Orange
    String sResetColor = "§r";

    String sVersion = getDescription().getVersion(); // version in plugin.yml

    String sStartLine = "";
    String arg1 = "";
    String arg2 = "";
    String arg3 = "";

    public static final String barrelShopName = "§9SHOP §5BARREL";
    public static final Logger LOG = Logger.getLogger("Minecraft");

    // COLOR FOR COMMAND LINE
    public static final String ANSI_RESET = "\u001B[0m";
    //public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    //public static final String ANSI_BLUE = "\u001B[34m";

    //public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    //public static final String ANSI_WHITE = "\u001B[37m";

    private File configf, dataFilef, baltopf, langDefaultf, langf;
    private FileConfiguration config;
    private FileConfiguration dataFile;
    private FileConfiguration configBaltop;
    private static FileConfiguration configDefaultLang;
    private static FileConfiguration configLang;
    private HashMap<UUID, Integer> a_sTimerShop = new HashMap<UUID, Integer>();
    private HashMap<Location, Material> a_objShowItemShop = new HashMap<Location, Material>();
    //private HashMap<UUID, Integer> a_AutoSaveHandler = new HashMap<UUID, Integer>();
    private HashMap<UUID, HandleMessage> a_handleChatMessage = new HashMap<UUID, HandleMessage>();
    public static int expirationAuctionDay = 1;

    private static final String CONFIG_SEPARATOR = "###################################################################################";

    /*
    private Long extractLong(String s) {
        String num = s.replaceAll("\\D", "");

        if(num.isEmpty()) {
            return Long.parseLong("0");
        }
        else {
            return Long.parseLong(num)*100; // to include decimal
        }
    }
     */

    /**
     * This will return the translate key otherwise the exact error
     * @param sError
     * @return
     */
    private String getTransactionErrorTranslated(String sError) {
        switch(sError) {
            case "Loan was not permitted!":
                return "loadNotPermitted";

            default:
                return sError;
        }
    }

    private void messageToConsole(String sKeyMessage, String sWordReplace) {

        List<String> a_sWordReplace = new ArrayList<String>();
        a_sWordReplace.add(sWordReplace);

        messageToConsole(sKeyMessage, a_sWordReplace);
    }

    private void messageToConsole(String sKeyMessage) {
        LOG.info(ChatColor.stripColor(sPluginName)+" "+getTranslatedKeys(sKeyMessage));
    }

    private void messageToConsole(String sKeyMessage, List<String> a_sWordReplace) {
        LOG.info(ChatColor.stripColor(sPluginName));

        // We take what we have from the translatedKey
        String sOutput = getTranslatedKeys(sKeyMessage);

        //for supporting colors we need to convert all &1 to §1 - &2 to §2 etc... Maybe later :)

        // Wish a better way to do this :( For each %s we replace for the word to replace
        for(String sReplace : a_sWordReplace) {
            if(sReplace != null) {
                sOutput = sOutput.replaceFirst("%s", sReplace);
            }

        }
        LOG.info(ChatColor.stripColor(sOutput));
    }

    private boolean playerHaveSpaceInventory(Player player, ItemStack itemStackCheck, int iQts) {

        int iEmptySpace = 0;
        int iQtsLeft = iQts; // 64
        consoleLog("iQtsLeft: "+iQtsLeft);
        for(ItemStack itemstack : player.getInventory().getContents()) {
            if(itemstack == null) { // empty slot
                if(iQtsLeft <= 64 && itemStackCheck.getMaxStackSize() != 1) {
                    return true; // we have space. Too bad for max stack == 16
                }
                iQtsLeft = iQtsLeft - 1;
                iEmptySpace++;
            }
            else if(itemStackCheck.isSimilar(itemstack) && itemstack.getMaxStackSize() != 1) {
                // ok we have the same items here, so we can at least add one
                int iRoom = itemstack.getMaxStackSize() - itemstack.getAmount();
                if(iRoom != 0) {
                    iQtsLeft = iQtsLeft - iRoom;
                }
                if(iQtsLeft < 1) {
                    return true; // we stack all items no need to go further
                }
            }
        }

        // check again if we have nothing
        if(iQtsLeft < 1) {
            return true; // we stack all items no need to go further
        }
        consoleLog("iQtsLeft: "+iQtsLeft);

        return false;
    }

    private void sendMessageToPlayer(Player player, String sKeyMessage, String sColor, List<String> a_sWordReplace) {
        player.sendMessage(sPluginName);

        // We take what we have from the translatedKey
        String sOutput = getTranslatedKeys(sKeyMessage);

        //for supporting colors we need to convert all &1 to §1 - &2 to §2 etc... Maybe later :)

        // Wish a better way to do this :( For each %s we replace for the word to replace
        if(a_sWordReplace.size() > 0) {
            for (String sReplace : a_sWordReplace) {
                if (sReplace != null) {
                    sOutput = sOutput.replaceFirst("%s", sReplace);
                }

            }
        }
        player.sendMessage(sColor+sOutput);
    }

    private void sendMessageToPlayer(Player player, String sKeyMessage, String sColor) {
        sendMessageToPlayer(player, sKeyMessage, sColor, new ArrayList<String>());
    }

    private void sendMessageToPlayer(Player player, String sKeyMessage, String sColor, String sWordReplace) {

        List<String> a_sWordReplace = new ArrayList<String>();
        a_sWordReplace.add(sWordReplace);

        sendMessageToPlayer(player, sKeyMessage, sColor, a_sWordReplace);
    }

    private void sendMessageToPlayer(CommandSender sender, String sKeyMessage, String sColor, List<String> a_sWordReplace) {
        if(sender instanceof Player) {
            sendMessageToPlayer((Player) sender, sKeyMessage, sColor, a_sWordReplace);
        }
        else {
            messageToConsole(sKeyMessage, a_sWordReplace);
        }
    }

    private void sendMessageToPlayer(CommandSender sender, String sKeyMessage, String sColor) {
        if(sender instanceof Player) {
            sendMessageToPlayer((Player) sender, sKeyMessage, sColor);
        }
        else {
            messageToConsole(sKeyMessage);
        }
    }

    private void sendMessageToPlayer(CommandSender sender, String sKeyMessage, String sColor, String sWordReplace) {
        if(sender instanceof Player) {
            sendMessageToPlayer((Player) sender, sKeyMessage, sColor, sWordReplace);
        }
        else {
            messageToConsole(sKeyMessage, sWordReplace);
        }
    }

    public static final String capitalize(String str)
    {
        if (str == null || str.length() == 0) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    public static String getTranslatedKeys(String sKeyMessage) {

        //check first if key exist
        if(configLang.isSet(sKeyMessage)) {
            return configLang.getString(sKeyMessage);
        }
        // Otherwise we take the default one
        if(configDefaultLang.isSet(sKeyMessage)) {
            return configDefaultLang.getString(sKeyMessage);
        }
        // Well here we forgot a key return the key itself
        return sKeyMessage;
    }

    private void createFiles() throws InvalidConfigurationException {

        // we create once and never touch it again
        File langENCAfile, langFRCAfile;
        langENCAfile = new File(getDataFolder()+File.separator+"lang", "en-US.yml");
        langFRCAfile = new File(getDataFolder()+File.separator+"lang", "fr-CA.yml");
        langDefaultf = new File(getDataFolder()+File.separator+"lang", "default.yml");
        //langf is create after the config lang below

        if (!langENCAfile.exists()) {
            langENCAfile.getParentFile().mkdirs();
            saveResource("lang"+File.separator+"en-US.yml", false);
        }

        if (langFRCAfile.exists()) {
            langFRCAfile.delete();
        }
        langFRCAfile.getParentFile().mkdirs();
        saveResource("lang"+File.separator+"fr-CA.yml", false);

        if (langDefaultf.exists()) {
            langDefaultf.delete();
        }
        langDefaultf.getParentFile().mkdirs();
        saveResource("lang"+File.separator+"default.yml", false);
        // Exist or not we always save the file

        configf = new File(getDataFolder(), "config.yml");
        dataFilef = new File(getDataFolder(), "data.yml");
        baltopf = new File(getDataFolder(), "baltop.yml");
        ArrayList<String> a_sComments = new ArrayList<String>();

        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);

        }

        if (!dataFilef.exists()) {
            dataFilef.getParentFile().mkdirs();
            saveResource("data.yml", false);
        }
        if (!baltopf.exists()) {
            baltopf.getParentFile().mkdirs();
            saveResource("baltop.yml", false);
        }
        else {
            // Exist so delete and recreate
            baltopf.delete();
            baltopf.getParentFile().mkdirs();
            saveResource("baltop.yml", false);
        }

        config = new YamlConfiguration();
        dataFile = new YamlConfiguration();
        configBaltop = new YamlConfiguration();
        configDefaultLang = new YamlConfiguration();

        // LOADING CONFIG FIRST
        try {
            config.load(configf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if we have field if not update version and field
        boolean isNeedUpdate = false;

        // Added in v1.0.0
        if (!config.isSet("newWorldInDefault")) {
            config.set("newWorldInDefault", true);
        }
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("If true this will add all new world not listed in Default group");
        a_sComments.add("If false this will create a new group with only this world in this group");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("newWorldInDefault", a_sComments);


        // Added in v2.0.0 - Removed in 2.3.1
        /*
        if (!config.isSet("baltopdelay")) {
            config.set("baltopdelay", 20);
            isNeedUpdate = true;
        }
        */
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("This config is now useless. But keep it like this. Just in case ;) .");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("baltopdelay", a_sComments);

        if (!config.getString("version").equalsIgnoreCase(sVersion)) {
            isNeedUpdate = true;
        }

        // Added in v2.2.0 - removed in 2.2.2
        /*
        if(!config.isSet("namedatabase") || !config.isSet("userdatabase") || !config.isSet("passworddatabase")) {
            config.set("namedatabase", "");
            config.set("userdatabase", "");
            config.set("passworddatabase", "");
            isNeedUpdate = true;
        }
        */

        // Unused in v2.2.2 - Not implemented yet
        if (config.isSet("namedatabase") || config.isSet("userdatabase") || config.isSet("passworddatabase")) {
            a_sComments = new ArrayList<String>();
            a_sComments.add(CONFIG_SEPARATOR);
            a_sComments.add("NOT IMPLEMENTED YET...");
            a_sComments.add(CONFIG_SEPARATOR);
            config.setComments("namedatabase", a_sComments);
            isNeedUpdate = true;
        }

        // Added in v2.2.3 - language
        String defaultLang = "en-US";
        if (!config.isSet("language")) {
            config.set("language", defaultLang);
            isNeedUpdate = true;
        }
        else {
            defaultLang = config.getString("language");
        }
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("The default language of the plugin");
        a_sComments.add("if something not found in the file it will take from default.yml");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("language", a_sComments);

        // Added in v2.3.1
        if(!config.isSet("sTimezone")) {
            config.set("sTimezone", sTimezone);
            isNeedUpdate = true;
        }
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("The default TimeZone of the plugin");
        a_sComments.add("Warning: If not working it will take the GMT by default");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("sTimezone", a_sComments);

        // Added in v2.3.5
        if(!config.isSet("bDebugMode")) {
            config.set("bDebugMode", false);
            isNeedUpdate = true;
        }
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("This is used to spam the console for debugging. I think you will not need");
        a_sComments.add("to turn it on. ^_^' ");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("bDebugMode", a_sComments);

        // Removed in v2.3.5 useless now (hook on EssentialX Economy
        // Added in v2.3.3
        if(config.isSet("iAutoUpdatePlayer")) {
            config.set("iAutoUpdatePlayer", null);
            isNeedUpdate = true;
        }

        // Added in v2.3.5
        if(!config.isSet("iAuctionExpirationDay")) {
            config.set("iAuctionExpirationDay", 10);
            isNeedUpdate = true;
        }
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("This is the number of day for all auctionHouse");
        a_sComments.add("must be greater than 0, if not it will be 1 by default");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("iAuctionExpirationDay", a_sComments);

        expirationAuctionDay = config.getInt("iAuctionExpirationDay");

        /*
        a_sComments = new ArrayList<String>();
        a_sComments.add(CONFIG_SEPARATOR);
        a_sComments.add("This will save balance every x minutes. -1 to disable");
        a_sComments.add("Suggestion: If you enable it put minimum 5. If less");
        a_sComments.add("than 5, more player you have, more latency can occur");
        a_sComments.add("This will not affect the logoff or when changing world");
        a_sComments.add(CONFIG_SEPARATOR);
        config.setComments("iAutoUpdatePlayer", a_sComments);
        */

        langf = new File(getDataFolder()+File.separator+"lang", defaultLang+".yml");
        if (!langf.exists()) { // If not exist take the default
            LOG.warning("The file "+defaultLang+".yml not exist, default.yml is loaded...");
            langf = new File(getDataFolder()+File.separator+"lang", "default.yml");
        }
        configLang = new YamlConfiguration();
        try {
            configLang.load(langf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Si nous avons un update à faire du fichier
        if(isNeedUpdate) {
            config.set("version", sVersion);
            a_sComments = new ArrayList<String>();
            a_sComments.add(CONFIG_SEPARATOR);
            a_sComments.add("CONFIG FOR THE MULTIWORLDMONEY");
            a_sComments.add("This files as an auto updater, if a config is missing");
            a_sComments.add("it will add it by it self with a default value");
            a_sComments.add("No worry, your config is kept.");
            a_sComments.add(CONFIG_SEPARATOR);
            config.setComments("version", a_sComments);

            // Save file
            try {
                config.save(configf);
                LOG.info("TheMultiWorldMoney updated is config.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // We reload file
            try {
                config.load(configf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load the timezone from the file now
        sTimezone = config.getString("sTimezone");


        try {
            configDefaultLang.load(langDefaultf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            dataFile.load(dataFilef);
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateDataIfNeeded();

        try {
            configBaltop.load(baltopf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void consoleLog(String sMessage) {

        if(config.getBoolean("bDebugMode")) {
            LOG.info("[TMWM] " + sMessage);
        }
    }

    private void updateDataIfNeeded() {
        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
            if(!dataFile.isSet("groupinfo."+sGroup)) {
                dataFile.set("groupinfo."+sGroup+".startingbalance", 0.0);
            }
            if(!dataFile.isSet("groupinfo."+sGroup+".auctionHouseEnable")) {
                dataFile.set("groupinfo."+sGroup+".auctionHouseEnable", true);
            }
        }

        try {
            dataFile.save(dataFilef);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //same function in TabCompleter
    private boolean havePermission(CommandSender sender, String sType) {

        if(sender instanceof Player) {
            return havePermission((Player) sender, sType);
        }
        else if (sender instanceof ConsoleCommandSender) {
            if(sType.equalsIgnoreCase("console")) {
                return true;
            }
            return false;
        }
        return false;

    }

    // Same function in TabCompleter
    public boolean havePermission(Player p, String sType) {

        boolean bAdmin = (p.isOp() || p.hasPermission("themultiworldmoney.admin"));
        boolean bMod = (bAdmin || p.hasPermission("themultiworldmoney.mod"));

        boolean bUsePay = (bMod || p.hasPermission("themultiworldmoney.pay"));

        boolean bUsekilledPlayers = (bMod || p.hasPermission("killedplayers.use"));

        switch(sType) {
            case "create_shop":
                return p.hasPermission("themultiworldmoney.createshop");
            case "normal": // Mean always all players
                return true;
            case "killedplayers": // OP ADMIN MOD killedplayers.use
                return bUsekilledPlayers;
            case "pay": // OP ADMIN MOD PAY
                return bUsePay;
            case "mod": // OP ADMIN MOD
                return bMod;
            case "admin": // OP ADMIN
                return bAdmin;
            default:
                consoleLog(sType+" is not defined in permission");
                return false;
        }
    }

    private void makeBackup() {
        // Backup
        File logFirstStartf = new File(getDataFolder(), "backupVault.yml");
        baltopf = new File(getDataFolder(), "baltop.yml");

        // Backup Data and create the first baltop
        if (!logFirstStartf.exists()) {
            logFirstStartf.getParentFile().mkdirs();

            // Make all player's data in a file
            try {
                logFirstStartf.createNewFile();
                if (!baltopf.exists()) {
                    baltopf.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileConfiguration logFirstStart = YamlConfiguration.loadConfiguration(logFirstStartf);
            configBaltop = YamlConfiguration.loadConfiguration(baltopf);

            configBaltop.set("default", "");
            for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {

                // Don't know why, but can have some ghost offlinePlayer. :/
                if(player == null || player.getName() == null) {
                    continue;
                }

                logFirstStart.set("Player."+player.getName()+".getUUID", player.getUniqueId().toString());
                logFirstStart.set("Player."+player.getName()+".amountVault", (double) econ.getBalance(player));

                for(String bank : econ.getBanks()) {
                    logFirstStart.set("Player."+player.getName()+"."+bank+".amountVault", (double) econ.bankBalance(bank).balance);
                }
                logFirstStart.set("Player."+player.getName()+".amountVault", (double) econ.getBalance(player));

                // Save current money group world
                double playerBalance = econ.getBalance(player);
                saveMoneyPlayerInGroup(player, "default", playerBalance, true);
                configBaltop.set("default."+player.getName(), playerBalance);
            }
            configBaltop.set("lastcall", returnDateHour());

            try {
                logFirstStart.save(logFirstStartf);
                configBaltop.save(baltopf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            // file exist and baltop was cleaned make async and read all players file?
            Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {

                        // Don't know why, but can have some ghost offlinePlayer. :/
                        if(player == null || player.getName() == null) {
                            continue;
                        }

                        // open
                        FileConfiguration config = null;
                        File file = getFileMoneyPlayerPerGroup(player, true);
                        config = YamlConfiguration.loadConfiguration(file);

                        double dAmount = 0.0;

                        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                            dAmount = config.getDouble("Player."+sGroup);
                            configBaltop.set(sGroup+"."+player.getName(), econ.format(dAmount));
                        }

                    }
                    configBaltop.set("lastcall", returnDateHour());

                    try {
                        configBaltop.save(baltopf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private String getBalTopList(String sGroup, int iPage) {

        try {
            configBaltop.load(baltopf);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        // first check if this group exist
        if(!configBaltop.isConfigurationSection(sGroup)) {
            return sErrorColor+"- The group '"+sGroup+"' doesn't exist -";
        }

        // Page 1 sera 0 | 2 sera 1
        iPage = iPage-1;
        if(iPage < 0) {
            iPage = 0;
        }

        String sReturn = "";
        int iCount=1;

        // Get the list of the groups (return 10 match) based on iPage
        ArrayList<String> myList = new ArrayList<>();

        double dTotal = 0.0;
        for(String key : configBaltop.getConfigurationSection(sGroup).getKeys(false)) {

            String num = configBaltop.getString(sGroup+"."+key).replaceAll("\\D", "");

            double playerAmount = 0.0;
            if(!num.isEmpty()) {
                playerAmount = Long.parseLong(num);
            }
            dTotal += playerAmount;

            myList.add(key+": "+econ.format(playerAmount));
        }
        myList = reorderArray(myList);

        for(String playerTop : myList) {
            if(iCount >= (iPage*10+1) && iCount <= (iPage*10+10)) {
                sReturn = sReturn+iCount+". "+playerTop+"\n";
            }
            iCount++;
        }

        // If we have nothing
        if(sReturn.equalsIgnoreCase("")) {
            sReturn = "- No data for this page -";
        }
        else {
            // Affiche total de tous
            String moneyString = econ.format(dTotal);
            sReturn = sErrorColor+"Total: "+moneyString+"\n§r"+getTranslatedKeys("lastUpdate")+": "+configBaltop.getString("lastcall")+"\n"+sReturn;

            sReturn = sReturn + sObjectColor+"Next page\n/tmwm baltop "+sGroup+" "+(iPage+2)+"\n";
        }

        return sReturn;

    }

    private ArrayList<String> reorderArray(ArrayList<String> strings) {

        Collections.sort(strings, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return extractLong(o2) > extractLong(o1) ? 1 : -1;
            }

            Long extractLong(String s) {

                String[] a_s = s.split(":");
                String num = "0";
                if(a_s.length > 1) {
                    num = a_s[1].replaceAll("\\D", "");
                }

                if(num.isEmpty()) {
                    return Long.parseLong("0");
                }
                else {
                    return Long.parseLong(num)*100; // to include decimal
                }
            }
        });

        return strings;
    }


    @Override
    public void onEnable(){

        TheMultiWorldMoney _this = this;

        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
            public void run(){

                if (!setupEconomy() ) {
                    Bukkit.getServer().getPluginManager().disablePlugin(_this);
                    return;
                }

                getCommand("themultiworldmoney").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));
                getCommand("tmwm").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));

                getCommand("payto").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));

                getCommand("killedplayers").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));

                getCommand("auction").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));
                getCommand("ac").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));
                getCommand("ah").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));
                getCommand("ach").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));
                getCommand("hdv").setTabCompleter(new TheMultiWorldMoneyTabCompleter(getDataFolder()));

                setupPermissions();
                setupChat();

                Bukkit.getPluginManager().registerEvents(_this,_this);

                // Create file
                try {
                    createFiles();
                } catch (InvalidConfigurationException e) {
                    e.printStackTrace();
                }

                // LOG INFO
                LOG.info(ANSI_GREEN+"------------------------------------------"+ANSI_RESET);
                LOG.info(" ");
                LOG.info(ANSI_CYAN+"THE MULTIWORLD MONEY"+ANSI_RESET+ANSI_YELLOW+" by Patfreeze"+ANSI_RESET);
                LOG.info(ANSI_CYAN+"Version "+ANSI_RESET+ANSI_YELLOW+" "+sVersion+ANSI_RESET);
                LOG.info(" ");
                LOG.info(ANSI_GREEN+"-------------------------------------------"+ANSI_RESET);

                // Now check if version in config file is OK
                // Seens this must be update it self this check will be useless
                if(!config.isString("version")) {
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(" ");
                    LOG.info(ANSI_YELLOW+"YOU NEED TO DELETE/BACKUP CONFIG.YML IN THEMULTIWORLDMONEY FOLDER"+ANSI_RESET);
                    LOG.info(ANSI_YELLOW+"After created file compare it with the last one"+ANSI_RESET);
                    LOG.info(" ");
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                }
                else if(!sVersion.matches(config.getString("version"))) {
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(" ");
                    LOG.info(ANSI_YELLOW+"YOU NEED TO DELETE/BACKUP CONFIG.YML IN THEMULTIWORLDMONEY FOLDER OLD VERSION "+config.getString("version")+ANSI_RESET);
                    LOG.info(ANSI_YELLOW+"After created file compare it with the last one"+ANSI_RESET);
                    LOG.info(" ");
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                    LOG.info(ANSI_RED+"ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT ALERT "+ANSI_RESET);
                }

                // Backup before player login
                // This will prevent if errors occurs get a backup for vault
                makeBackup();

                // Load from file
                reload();

            }
        }, 2); // Wait a certain time because of Vault
    }

    private void reload() {
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
            public void run(){
                loadMoneyReload();
            }
        }, 2);
    }

    /*
    private void cancelPlayerAutoSave(Player player) {
        if(a_AutoSaveHandler.get(player.getUniqueId()) != null) {
            Bukkit.getScheduler().cancelTask(a_AutoSaveHandler.get(player.getUniqueId()));
        }
    }
     */

    /*
    private void handleChangingWorldAutoSave(Player player) {
        cancelPlayerAutoSave(player);
        int iTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                addAutoSave(player);
            }
        }, 20*5);
        // Need this if player change find and return back
        a_AutoSaveHandler.put(player.getUniqueId(), iTask);
    }
     */

    private int getInvAmountForItems(Player player, ItemStack itemStack, int iMax) {

        int iCount = iMax; // 64
        for(ItemStack is : player.getInventory().getContents()) {

            if(iCount == 0) {
                break; // We have reach our limit
            }

            if(is == null) {
                continue;
            }

            // clone of the purpose
            ItemStack itemCheck = is.clone();
            itemCheck.setAmount(1);
            ItemStack shopItemCheck = itemStack.clone();
            shopItemCheck.setAmount(1);

            if(itemCheck.equals(shopItemCheck)) {
                // Item is the same (Enchantment and all is metadata)
                int iItemAmount = is.getAmount(); // 10
                if(iItemAmount < iCount) {
                    is.setAmount(0);
                    iCount = iCount - iItemAmount; // 2
                }
                else {
                    is.setAmount(iItemAmount-iCount);
                    iCount = 0;
                }
            }
        }

        return iCount;
    }

    public void showAllArmorStandInvisible(Player player, int iLimit) {
        Predicate<Entity> filter = new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                System.out.println("entity.getType(): "+entity.getType());
                System.out.println("Good type?: "+(entity.getType() == EntityType.ARMOR_STAND));
                return entity.getType() == EntityType.ARMOR_STAND;
            }
        };
        Collection<Entity> a_Entity = player.getWorld().getNearbyEntities(player.getLocation().add(0, 0, 0), iLimit, iLimit, iLimit, filter);
        for (Iterator<Entity> it = a_Entity.iterator(); it.hasNext(); ) {
            ArmorStand ent = (ArmorStand) it.next();
            ent.setInvulnerable(true);
            ent.setVisible(true);
        }
    }

    public void playDenyShopSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6F, 0.7F);
    }

    @Override
    public void onDisable(){

        // Cancel all task
        /*
        for(UUID playerId : a_AutoSaveHandler.keySet()) {
            Bukkit.getScheduler().cancelTask(a_AutoSaveHandler.get(playerId));
        }
         */

        // try to save players money
        for(Player player : Bukkit.getOnlinePlayers()) {
            saveMoneyPlayerPerGroup(player, player.getWorld().getName());
        }
        LOG.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }


    @EventHandler
    // onClickBlock onClickSign
    public void clickBlock(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        // Protection if no block clicked just ignore
        if(e.getClickedBlock() == null) {
            return;
        }
        if(e.getClickedBlock() != null && e.getClickedBlock().getType().name().contains("_SIGN")) {

            Sign signShop = (Sign) e.getClickedBlock().getState();
            if(ChatColor.stripColor(signShop.getLine(0)).equalsIgnoreCase("[tmwm]") && ChatColor.stripColor(signShop.getLine(1)).equalsIgnoreCase("shop")) {
                if(ChatColor.stripColor(signShop.getLine(2)).trim().isEmpty() || ChatColor.stripColor(signShop.getLine(3)).trim().isEmpty()) {
                    // not a real sign just ignore
                   return;
                }
                // Left click
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    if (player.getGameMode() == GameMode.CREATIVE && player.getInventory().getItemInMainHand().getType() != Material.STICK) {
                        e.setCancelled(true);
                        sendMessageToPlayer(player, getTranslatedKeys("destroyShopStick"), sErrorColor);
                    } else if (player.getGameMode() == GameMode.CREATIVE && player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                        sendMessageToPlayer(player, getTranslatedKeys("destroyShop"), sErrorColor);
                        ShopAdmin shopAdmin = new ShopAdmin(player, getDataFolder(), signShop.getLocation(), true);
                        shopAdmin.deleteShop();
                        return; // Nothing more to do
                    }
                }
                openShop(player, e.getClickedBlock().getLocation());
            }
            return;
        }

        // Protection Barrel TODO: remove if sign do the job
        /*
        if(e.getClickedBlock().getType() == Material.BARREL) {

            Barrel barrel = (Barrel) e.getClickedBlock().getState();

            // TODO: Check if we have a sign stick on this with the word [tmwm] shop

            //guard if not a shop ignore
            if(barrel.getCustomName() == null || !barrel.getCustomName().equalsIgnoreCase(barrelShopName)) {
                return;
            }

            // Left click
            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                if(player.getGameMode() == GameMode.CREATIVE && player.getInventory().getItemInMainHand().getType() != Material.STICK) {
                    e.setCancelled(true);
                    sendMessageToPlayer(player, getTranslatedKeys("destroyShopStick"), sErrorColor);
                }
                else if(player.getGameMode() == GameMode.CREATIVE && player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                    sendMessageToPlayer(player, getTranslatedKeys("destroyShop"), sErrorColor);
                    ShopAdmin shopAdmin = new ShopAdmin(player, getDataFolder(), barrel.getLocation(), true);
                    shopAdmin.deleteShop();
                    a_objShowItemShop.put(barrel.getLocation(), null);
                }

                if(player.getGameMode() == GameMode.SURVIVAL) {
                    e.setCancelled(true);

                    openShop(player, barrel.getLocation());
                }
            }

            if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
                openShop(player, barrel.getLocation());
            }
        }
        */
    }

    @EventHandler
    public void onPlayerBreak(BlockBreakEvent event) {
        if (event.getPlayer() != null) {
            Player player = event.getPlayer();
            // TODO: If is a shop need a way to cancel event
        }
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if(event.getPlayer() != null) {
            Player player = event.getPlayer();

            //TODO: To get Buy and Sell we need the sign like so
            // Line One:    [tmwm]
            // Line two:    * (item in hand) or DIAMOND
            // Line three:  B 200 : S 100

            if(event.getBlockPlaced().getState() instanceof Barrel) {
                Barrel barrel = (Barrel) event.getBlockPlaced().getState();
                if (event.getBlockPlaced().getType() == Material.BARREL && barrel.getCustomName() != null && barrel.getCustomName().equalsIgnoreCase(barrelShopName)) {
                    createShop(player, event.getBlockPlaced().getLocation());
                }
            }
        }
    }

    private void clearSign(Block block) {

        if (block.getType().name().contains("_SIGN")) {

            Sign sign = (Sign) block.getState();
            sign.setGlowingText(false);

            if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[tmwm]")) {
                sign.setLine(0, " ");
                sign.setLine(1, " ");

                sign.setLine(2, " ");
                sign.setLine(3, " ");

                sign.update();
            }

        }
    }

    /**
     * This is call when sign is change
     * @param e
     */
    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if (e.getPlayer().hasPermission("sign.color")) {

            if (ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[tmwm]")) {

                Player player = e.getPlayer();

                String secondLine = ChatColor.stripColor(e.getLine(1)).toLowerCase();

                // Check the 2e line if not part of our list tell the player misspell
                switch (secondLine) {
                    case "shop":
                        // Good we have this but do we have permission
                        if(!havePermission(player, "create_shop")) {
                            sendMessageToPlayer(player, "havePermission", sErrorColor, "themultiworldmoney.createshop");
                            clearSign(e.getBlock());
                            return;
                        }
                        break;

                    default:
                        // Unknown this second line
                        e.getPlayer().sendMessage(sPluginName + sOrangeColor + "Second line was wrong, must be :" + sResetColor + " shop");
                        return;
                }

                if (secondLine.equalsIgnoreCase("shop")) {
                    // createShop
                    createShop(player, e.getBlock().getLocation());
                }
            }
        }
    }

    @EventHandler
    private void onChatMessage(AsyncPlayerChatEvent e) {

        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if we need to cancel and put text in something
        if(a_handleChatMessage.containsKey(playerId) && a_handleChatMessage.get(playerId) != null) {
            // check if its
            HandleMessage handleMessage = a_handleChatMessage.get(playerId);
            if(handleMessage.getInputPlayer().isEmpty()) {
                e.setCancelled(true);
                if(handleMessage.setInputPlayer(e.getMessage())) {
                    sendMessageToPlayer(player, "✓ '"+e.getMessage().trim()+"' "+getTranslatedKeys("saved"), sCorrectColor);
                }else {
                    sendMessageToPlayer(player, "✖ '"+e.getMessage().trim()+"' "+getTranslatedKeys("somethingWrongHappen")+" :(", sErrorColor);
                }
            }

            ShopAdmin shopAdmin = handleMessage.getShopAdmin();

            // No matter what we clear the handleChatMessage
            a_handleChatMessage.remove(playerId);
        }

        //e.getPlayer();
        //consoleLog(e.getMessage());
    }

    private void playNote(Player player, Instrument instrument, Note note, int iTickTime) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                player.playNote(player.getLocation(), instrument, note);
            }
        }, iTickTime);
    }

    private void playTransactionCompleted(Player player) {
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.A), 1);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.B), 3);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.C), 5);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.D), 7);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.E), 10);
    }

    private void playTransactionRemove(Player player) {
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.E), 1);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.D), 3);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.C), 5);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.B), 7);
        playNote(player, Instrument.XYLOPHONE, Note.natural(1, Note.Tone.A), 10);
    }

    private void refreshChest54(Player player, Runnable runnable) {
        GuiCMD guiCMD = new GuiCMD(player, "refresh54", player.getLocation());
        guiCMD.render(runnable, 1, this);
    }

    //onClick chest
    @EventHandler
    private void inventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();

        // Only our plugin in
        if (e.getView().getTitle().toLowerCase().startsWith(sPluginNameNoColor.toLowerCase())) {
            e.setCancelled(true);
            if ((e.getCurrentItem() == null) || (e.getCurrentItem().getType().equals(Material.AIR))) {
                return;
            }

            // Delay executing task
            UUID idPlayer = player.getUniqueId();
            int iTask = 0;
            if(a_sTimerShop.containsKey(idPlayer)) {
                iTask = a_sTimerShop.get(idPlayer);
                consoleLog("containsKey task: "+iTask);
                if(iTask != 0) {
                    consoleLog("task is not 0");
                    // check if task is completed
                    if(iTask%4 == 0) { // 25% chance to send the pleaseWait (less message to console)
                        sendMessageToPlayer(player, "pleaseWait", sOrangeColor);
                    }
                    return;
                }
            }
            iTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
                public void run() {
                    // Clear the task
                    a_sTimerShop.put(idPlayer, 0);
                    consoleLog("task completed we put 0");
                }
            }, 10); // 1 second = 20 ticks

            a_sTimerShop.put(idPlayer, iTask);
            consoleLog(idPlayer+" new task: "+iTask);

            // The Type inventory
            String[] a_sSplitName = e.getView().getTitle().split(":");

            String[] a_sSplitLocation = a_sSplitName[2].split("l");
            Location location = new Location(player.getWorld(), Integer.parseInt(a_sSplitLocation[0]),Integer.parseInt(a_sSplitLocation[1]),Integer.parseInt(a_sSplitLocation[2]));

            consoleLog("SlotId: "+e.getSlot());
            consoleLog("getTYpe: "+e.getClickedInventory().getType());
            ShopAdmin shopAdmin = null;
            AuctionHouse auctionHouse = null;
            int qts = 0;
            GuiCMD guiCMD;

            switch (a_sSplitName[1].toLowerCase()) {

                case "ahexpired":
                    auctionHouse = new AuctionHouse(player, getDataFolder(), getGroupNameByWorld(player.getWorld().getName()));

                    if(e.getClickedInventory().getType() == InventoryType.CHEST) {
                        switch (e.getSlot()) {

                            case 45: // Go back
                                player.closeInventory();
                                AuctionHouse finalAuctionHouse = auctionHouse;
                                refreshChest54(player, new Runnable() {
                                    @Override
                                    public void run() {
                                        GuiCMD guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(finalAuctionHouse, 1);
                                    }
                                });
                                return;

                            case 49: // close inventory
                                player.closeInventory();
                                return;

                            default:
                                // This is for slot between 0 and 35 inclusively otherwise nothing to do
                                if(e.getSlot() >=0 && e.getSlot() <= 35 && !e.getClickedInventory().getItem(e.getSlot()).getItemMeta().getDisplayName().equalsIgnoreCase(" ")) {

                                    ItemStack itemStackAh = e.getClickedInventory().getItem(e.getSlot()).clone();
                                    ItemMeta meta = itemStackAh.getItemMeta();
                                    String sKey = ChatColor.stripColor(meta.getLore().get(0));
                                    meta.setLore(null);
                                    itemStackAh.setItemMeta(meta);

                                    // No room in inventory
                                    if(!playerHaveSpaceInventory(player, itemStackAh, qts)) {
                                        playDenyShopSound(player);
                                        sendMessageToPlayer(player, "nospace", sErrorColor);
                                        return;
                                    }
                                    consoleLog("The sKey: "+sKey);
                                    if(auctionHouse.removeAuctionItemByItemId(sKey)) {
                                        player.getInventory().addItem(itemStackAh);
                                        player.closeInventory();

                                        // re-open the GUI at page 1
                                        guiCMD = new GuiCMD(player, "ahExpired", player.getLocation());
                                        guiCMD.render(auctionHouse, 1);
                                        return;
                                    }
                                }
                                break;
                        }
                    }
                    break;

                case "confirmauction":
                    auctionHouse = new AuctionHouse(player, getDataFolder(), getGroupNameByWorld(player.getWorld().getName()));

                    if(e.getClickedInventory().getType() == InventoryType.CHEST) {
                        switch (e.getSlot()) {
                            // Confirm
                            case 0:
                            case 1:
                            case 2:
                                ItemStack itemStackAh = e.getClickedInventory().getItem(4).clone();
                                ItemMeta meta = itemStackAh.getItemMeta();
                                String sKey = ChatColor.stripColor(meta.getLore().get(0));
                                meta.setLore(null);
                                itemStackAh.setItemMeta(meta);

                                // No room in inventory
                                if(!playerHaveSpaceInventory(player, itemStackAh, qts)) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "nospace", sErrorColor);
                                    return;
                                }

                                AuctionItem auctionItem = auctionHouse.getAuctionItemById(sKey);

                                if(auctionItem == null) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "itemSold", sErrorColor);

                                    player.closeInventory();
                                    AuctionHouse finalAuctionHouse3 = auctionHouse;
                                    refreshChest54(player, new Runnable() {
                                        @Override
                                        public void run() {
                                            GuiCMD guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                            guiCMD.render(finalAuctionHouse3, 1);
                                        }
                                    });

                                    return;
                                }

                                // We need to validate if the player have the money (or if is own item)
                                if(player.getUniqueId() != auctionItem.getPlayerOwner().getUniqueId() && econ.getBalance(player) < auctionItem.getPrice()) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "nomoney", sErrorColor);
                                    return;
                                }

                                // If we remove items because we are the owner, do not make any transaction
                                if(player.getUniqueId() == auctionItem.getPlayerOwner().getUniqueId()) {
                                    if(auctionHouse.removeAuctionItemByItemId(sKey)) {
                                        player.getInventory().addItem(itemStackAh);
                                        player.closeInventory();
                                        playTransactionRemove(player);

                                        // re-open the GUI at page 1
                                        guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(auctionHouse, 1);
                                    }
                                    return;
                                }

                                 // If transaction success remove from ah then give to player
                                EconomyResponse r = econ.withdrawPlayer(player, auctionItem.getPrice());
                                if(r.transactionSuccess()) {

                                    // transaction completed... Give items to player
                                    if(auctionHouse.removeAuctionItemByItemId(sKey)) {
                                        player.getInventory().addItem(itemStackAh);
                                        player.closeInventory();

                                        playTransactionCompleted(player);
                                        sendNewBalancePlayer(player);

                                        // Give the balance to the group of the seller one
                                        File file = getFileMoneyPlayerPerGroup(auctionItem.getPlayerOwner(), false);
                                        FileConfiguration configP = YamlConfiguration.loadConfiguration(file);
                                        handleGroupTransactionAh(
                                            auctionItem.getPlayerOwner(),
                                            configP,
                                            getGroupNameByWorld(player.getWorld().getName()),
                                            auctionItem.getPrice(),
                                            auctionItem.getItemStack().getType().name()
                                        );

                                        // re-open the GUI at page 1
                                        guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(auctionHouse, 1);
                                        return;
                                    }
                                    else {
                                        // Item was sold before this transaction, put back player money
                                        r = econ.depositPlayer(player, auctionItem.getPrice());
                                        if(r.transactionSuccess()) {
                                            playDenyShopSound(player);
                                            sendMessageToPlayer(player, "itemSold", sErrorColor);
                                        }
                                        else {
                                            playDenyShopSound(player);
                                            sendMessageToPlayer(player, "transactionFailed", sErrorColor);
                                            LOG.info(String.format("An error occurred: %s", r.errorMessage));
                                        }
                                    }
                                } else {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "transactionFailed", sErrorColor);
                                    LOG.info(String.format("An error occurred: %s", r.errorMessage));
                                    return;
                                }
                                break;

                            // Cancel
                            case 6:
                            case 7:
                            case 8:
                                player.closeInventory();
                                AuctionHouse finalAuctionHouse3 = auctionHouse;
                                refreshChest54(player, new Runnable() {
                                    @Override
                                    public void run() {
                                        GuiCMD guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(finalAuctionHouse3, 1);
                                    }
                                });
                                break;
                        }
                    }

                    break;

                case "auctionhouse":
                    auctionHouse = new AuctionHouse(player, getDataFolder(), getGroupNameByWorld(player.getWorld().getName()));

                    if(e.getClickedInventory().getType() == InventoryType.CHEST) {
                        switch (e.getSlot()) {

                            case 46: // expired items
                                player.closeInventory();

                                AuctionHouse finalAuctionHouse1 = auctionHouse;
                                refreshChest54(player, new Runnable() {
                                    @Override
                                    public void run() {
                                        GuiCMD guiCMD = new GuiCMD(player, "ahExpired", player.getLocation());
                                        guiCMD.render(finalAuctionHouse1, 1);
                                    }
                                });
                                return;

                            case 49: // refresh ah
                                player.closeInventory();
                                AuctionHouse finalAuctionHouse2 = auctionHouse;
                                refreshChest54(player, new Runnable() {
                                    @Override
                                    public void run() {
                                        GuiCMD guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(finalAuctionHouse2, 1);
                                    }
                                });

                                return;

                            case 52:
                            case 53:

                                int iPage = 1;

                                ItemStack itemStackAh = e.getClickedInventory().getItem(e.getSlot()).clone();
                                ItemMeta meta = itemStackAh.getItemMeta();
                                iPage = Integer.parseInt(ChatColor.stripColor(meta.getLore().get(1)));

                                player.closeInventory();
                                AuctionHouse finalAuctionHouse4 = auctionHouse;
                                int finalIPage = iPage;
                                refreshChest54(player, new Runnable() {
                                    @Override
                                    public void run() {
                                        GuiCMD guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(finalAuctionHouse4, finalIPage);
                                    }
                                });


                                return;

                            default:
                                // This is for slot between 0 and 35 inclusively otherwise nothing to do
                                if(e.getSlot() >=0 && e.getSlot() <= 35 && !e.getClickedInventory().getItem(e.getSlot()).getItemMeta().getDisplayName().equalsIgnoreCase(" ")) {

                                    itemStackAh = e.getClickedInventory().getItem(e.getSlot()).clone();
                                    meta = itemStackAh.getItemMeta();
                                    String sKey = ChatColor.stripColor(meta.getLore().get(0));
                                    meta.setLore(null);
                                    itemStackAh.setItemMeta(meta);


                                    // No room in inventory
                                    if(!playerHaveSpaceInventory(player, itemStackAh, qts)) {
                                        playDenyShopSound(player);
                                        sendMessageToPlayer(player, "nospace", sErrorColor);
                                        return;
                                    }

                                    AuctionItem auctionItem = auctionHouse.getAuctionItemById(sKey);

                                    if(auctionItem == null) {
                                        playDenyShopSound(player);
                                        sendMessageToPlayer(player, "itemSold", sErrorColor);
                                        return;
                                    }

                                    // We need to validate if the player have the money (or if is own item)
                                    if(player.getUniqueId() != auctionItem.getPlayerOwner().getUniqueId() &&econ.getBalance(player) < auctionItem.getPrice()) {
                                        playDenyShopSound(player);
                                        sendMessageToPlayer(player, "nomoney", sErrorColor);
                                        return;
                                    }

                                    // Show confirm ah transaction
                                    guiCMD = new GuiCMD(player, "confirmAuction", player.getLocation());
                                    guiCMD.render(auctionItem);
                                    return;

                                    // If transaction success remove from ah then give to player

                                    /*
                                    if(auctionHouse.removeAuctionItemByItemId(sKey)) {
                                        player.getInventory().addItem(itemStackAh);
                                        player.closeInventory();

                                        // re-open the GUI at page 1
                                        guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                                        guiCMD.render(auctionHouse, 1);
                                        return;
                                    }
                                     */

                                }
                                break;
                        }
                    }
                    break;

                case "addorremoveitems":
                    shopAdmin = new ShopAdmin(player, getDataFolder(), location, true);
                    if(shopAdmin.isShopOwner(player)) {
                        a_objShowItemShop.put(location, shopAdmin.getItemStack().getType());

                        if(e.getClickedInventory().getType() == InventoryType.CHEST) {
                            switch(e.getSlot()) {

                                case 0: // Return
                                    player.closeInventory(); // We need to close the other one
                                    guiCMD = new GuiCMD(player, "adminShop", location);
                                    guiCMD.render(shopAdmin);
                                    return;

                                case 18: // Close interface
                                    player.closeInventory();
                                    return;

                                case 11: // Add or Remove - 1
                                case 12: // Add or Remove - 8
                                case 13: // Add or Remove - 16
                                case 14: // Add or Remove - 32
                                case 15: // add or Remove - 64

                                    // convert slot by stack
                                    int amount = 1;
                                    switch(e.getSlot()) {
                                        case 11:
                                            amount = 1;
                                            break;
                                        case 12:
                                            amount = 8;
                                            break;
                                        case 13:
                                            amount = 16;
                                            break;
                                        case 14:
                                            amount = 32;
                                            break;
                                        case 15:
                                            amount = 64;
                                            break;

                                        default:
                                            LOG.warning(e.getSlot()+" is not implemented for adding/removing items");
                                    }

                                    boolean isAmountNegative = false;
                                    if(e.isLeftClick()) {
                                        isAmountNegative = true;
                                    }
                                    else if(e.isRightClick()) {
                                        // nothing to handle here
                                    }
                                    else {
                                        LOG.warning("Not left no right... WTF? What a Terrible Failed!");
                                    }

                                    int iShop = shopAdmin.getQuantity();
                                    int qtsLeft = 0;
                                    if(isAmountNegative) {
                                        // need to check if we have to good qts if we remove, because we throw at player

                                        // if in the shop we have less than amount
                                        if(iShop < amount) {
                                            amount = iShop;
                                        }

                                        if(amount > 0) {
                                            ItemStack itemStackToGive = shopAdmin.getItemStack();
                                            itemStackToGive.setAmount(amount);
                                            player.getWorld().dropItem(player.getLocation(), itemStackToGive);
                                            qtsLeft = iShop - amount;
                                        }
                                        else {
                                            // Shop is empty
                                            sendMessageToPlayer(player, "outOfOrder", sErrorColor);
                                        }
                                    }
                                    else {
                                        // need to check if player have the amount on him if we add stuff
                                        amount = amount - getInvAmountForItems(player, shopAdmin.getItemStack(), amount);
                                        if(amount > 0) {
                                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8F, 0.7F);
                                        } else {
                                            playDenyShopSound(player);
                                            sendMessageToPlayer(player, "invFailedItem", sErrorColor);
                                        }
                                        qtsLeft = iShop + amount;

                                    }

                                    // Just in case we put to 0
                                    if(qtsLeft < 0) {
                                        qtsLeft = 0;
                                    }

                                    shopAdmin.setQuantity(qtsLeft);
                                    break;

                                default:
                                    // Nothing to do
                                    break;
                            }

                            // ReRender gui
                            guiCMD = new GuiCMD(player, "addOrRemoveItems", location);
                            guiCMD.render(shopAdmin);
                        }
                    }

                    break;

                case "adminshop":
                    shopAdmin = new ShopAdmin(player, getDataFolder(), location, true);
                    a_objShowItemShop.put(location, shopAdmin.getItemStack().getType());

                    // Change item when click on Player Inventory
                    if(e.getClickedInventory().getType() == InventoryType.PLAYER) {

                        consoleLog("IdClicked: "+e.getSlot());
                        if(e.getClickedInventory().getItem(e.getSlot()) != null) {
                            consoleLog("item: " + e.getClickedInventory().getItem(e.getSlot()).getType().name());
                        }
                        else {
                            consoleLog("item: null");
                        }

                        if(havePermission(player, "admin")) {
                            // change item? Or Sell?
                        }

                        // ReRender gui
                        guiCMD = new GuiCMD(player, "adminShop", location);
                        guiCMD.render(shopAdmin);
                    }
                    else if(e.getClickedInventory().getType() == InventoryType.CHEST) {

                        EconomyResponse r = null;

                        switch(e.getSlot()) {
                            case 4: // CHANGE ITEM
                                if(shopAdmin.isShopOwner(player)) {

                                    // Check if the shop is OP (ok to change) but if not, need to empty shop before change item
                                    if(!shopAdmin.hasInfinity() && shopAdmin.getQuantity() > 0) {
                                        player.closeInventory();
                                        sendMessageToPlayer(player, "shopNotEmpty", sErrorColor);
                                        return;
                                    }

                                    // Open another Gui to change item from the Player inventory
                                    guiCMD = new GuiCMD(player, "changeShopItem", location);
                                    guiCMD.render(shopAdmin);
                                    return;
                                }
                                break;

                            case 9: // Change Buying price
                                if(shopAdmin.isShopOwner(player)) {

                                    HandleMessage handleMessage = new HandleMessage(player, "buying_price", "", shopAdmin);
                                    a_handleChatMessage.put(idPlayer, handleMessage);
                                    sendMessageToPlayer(player, getTranslatedKeys("putPriceTchat"), sOrangeColor);
                                    player.closeInventory(); // We need to close the other one
                                    return;
                                }
                                break;

                            case 17: // Change Selling price
                                if(shopAdmin.isShopOwner(player)) {
                                    HandleMessage handleMessage = new HandleMessage(player, "selling_price", "", shopAdmin);
                                    a_handleChatMessage.put(idPlayer, handleMessage);
                                    sendMessageToPlayer(player, getTranslatedKeys("putPriceTchat"), sOrangeColor);
                                    player.closeInventory(); // We need to close the other one
                                    return;
                                }
                                break;

                            case 18:
                            case 19:
                            case 20:
                            case 21:
                                /////////////////////////////////////////
                                //1 8 32 64 - BUYING FROM SHOP
                                /////////////////////////////////////////
                                switch(e.getSlot()) {
                                    case 18:
                                        qts = 1;
                                        break;
                                    case 19:
                                        qts = 8;
                                        break;
                                    case 20:
                                        qts = 32;
                                        break;
                                    case 21:
                                        qts = 64;
                                        break;
                                    default:
                                        consoleLog("Error "+e.getSlot()+" not implemented in adminshop:click");
                                        return;
                                }

                                if(qts > shopAdmin.getItemStack().getMaxStackSize()) {
                                    qts = shopAdmin.getItemStack().getMaxStackSize();
                                }

                                consoleLog("Buying "+qts+" from shop and is "+(shopAdmin.hasInfinity() ? "op" : "not-op"));

                                // if not op, we need to check if this shop has enough items
                                if(!shopAdmin.hasInfinity() && shopAdmin.getQuantity() < qts) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "outOfOrder", sErrorColor);
                                    return;
                                }

                                // the price
                                double price = qts * shopAdmin.getBuyPrice();

                                // No room in inventory
                                if(!playerHaveSpaceInventory(player, shopAdmin.getItemStack().clone(), qts)) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "nospace", sErrorColor);
                                    return;
                                }

                                // check if player have this amount in is pocket
                                if(econ.getBalance(player) < price) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "nomoney", sErrorColor);
                                    return;
                                }

                                // Player haves the amount so make transaction and if ok give items to player and remove from shop if not op
                                r = econ.withdrawPlayer(player,price);
                                if(r.transactionSuccess()) {

                                    // transaction completed... Give items to player
                                    ItemStack itemStackToPlayer = shopAdmin.getItemStack().clone();
                                    itemStackToPlayer.setAmount(qts);
                                    player.getInventory().addItem(itemStackToPlayer);

                                    if(!shopAdmin.hasInfinity()) {
                                        // Shop is op : just take money/items from player
                                        shopAdmin.setQuantity(shopAdmin.getQuantity()-qts);
                                        consoleLog("ShopBalance+price "+shopAdmin.getBalance()+"+"+price);
                                        shopAdmin.setBalance(shopAdmin.getBalance()+price);

                                    }
                                    playTransactionCompleted(player);
                                    sendNewBalancePlayer(player);

                                } else {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "transactionFailed", sErrorColor);
                                    LOG.info(String.format("An error occured: %s", r.errorMessage));
                                    return;
                                }

                                // Player buying stuff - Need to check if is OP (so no worries about qts/money)
                                // otherwise we need to check the qts of the shop
                                break;

                            case 23:
                            case 24:
                            case 25:
                            case 26:
                                /////////////////////////////////////////
                                // 1 8 32 64 - SELLING FROM SHOP
                                /////////////////////////////////////////
                                switch(e.getSlot()) {
                                    case 23:
                                        qts = 1;
                                        break;
                                    case 24:
                                        qts = 8;
                                        break;
                                    case 25:
                                        qts = 32;
                                        break;
                                    case 26:
                                        qts = 64;
                                        break;
                                    default:
                                        consoleLog("Error "+e.getSlot()+" not implemented in adminshop:click");
                                        return;
                                }

                                // if not op, we need to check if this shop has enough money
                                if(!shopAdmin.hasInfinity() && shopAdmin.getBalance() < qts*shopAdmin.getSellPrice()) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "outOfMoney", sErrorColor);
                                    return;
                                }

                                int iQtsPlayer = getAmountOfItemsInventoryPlayer(player, shopAdmin.getItemStack().clone(), qts);

                                if(iQtsPlayer == 0) {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "noItemInv", sErrorColor);
                                    return;
                                }
                                if(iQtsPlayer <= qts) {
                                    qts = iQtsPlayer;
                                }

                                // the price
                                double priceSell = qts * shopAdmin.getSellPrice();

                                // Player haves the amount so make transaction and if ok give items to player and remove from shop if not op
                                r = econ.depositPlayer(player, priceSell);
                                if(r.transactionSuccess()) {

                                    // transaction completed... Remove items to player
                                    if(removeItemsFromInventoryPlayer(player, shopAdmin.getItemStack().clone(), qts) > 0) {
                                        List<String> a_sWordReplace = new ArrayList<>();
                                        a_sWordReplace.add(sCorrectColor+qts+" "+shopAdmin.getItemStack().getType().name()+sResetColor);
                                        sendMessageToPlayer(player, "soldItem", sResetColor, a_sWordReplace);
                                        //sendMessageToPlayer(player);
                                    }

                                    if(!shopAdmin.hasInfinity()) {
                                        // Shop is op : just take money/items from player
                                        shopAdmin.setQuantity(shopAdmin.getQuantity()+qts);
                                        shopAdmin.setBalance(shopAdmin.getBalance()-priceSell);
                                    }
                                    playTransactionCompleted(player);
                                    sendNewBalancePlayer(player);

                                } else {
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, "transactionFailed", sErrorColor);
                                    LOG.info(String.format("An error occured: %s", r.errorMessage));
                                    return;
                                }

                                // Player selling stuff - Need to check if is OP (so no worries about qts/money)
                                // otherwise we need to check the balance of the shop
                                // check if player have the exact item but if click the 64 and have only 60 sell all.
                                break;

                            // Add balance to this shop
                            case 46:
                            case 47:
                            case 48:

                                double amount = 10.0;
                                switch(e.getSlot()) {
                                    case 46:
                                        // default amount
                                        break;

                                    case 47:
                                        amount = 100.0;
                                        break;

                                    case 48:
                                        amount = 1000.0;
                                        break;

                                    default:
                                        LOG.warning("Slot number "+e.getSlot()+" is not implemented");
                                        sendMessageToPlayer(player, "error", sErrorColor);
                                        player.closeInventory();
                                        return;
                                }

                                ////////////////////////////////////////////
                                // left click (remove)
                                // right click (add)
                                ////////////////////////////////////////////
                                boolean isAdding = true;
                                if(e.isLeftClick()) {
                                    // check if the amount in the shop < amount ask if so just adjust amount
                                    if(shopAdmin.getBalance() < amount) {
                                        amount = shopAdmin.getBalance();
                                        if(amount == 0) {
                                            player.closeInventory();
                                            playDenyShopSound(player);
                                            sendMessageToPlayer(player, "balanceEmpty", sErrorColor);
                                            return;
                                        }
                                    }
                                    r = econ.depositPlayer(player, amount);

                                    isAdding = false;
                                }
                                else if(e.isRightClick()) {
                                    r = econ.withdrawPlayer(player, amount);
                                }
                                else {
                                    LOG.warning("Not left not right... WTF? What a Terrible Failed!");
                                    sendMessageToPlayer(player, "error", sErrorColor);
                                    player.closeInventory();
                                    return;
                                }

                                // Transaction was success add or remove from the shop
                                if(r.transactionSuccess()) {
                                    // Success so add or remove the amount
                                    if(!isAdding) {
                                        amount = 0 - amount;
                                    }
                                    shopAdmin.setBalance(shopAdmin.getBalance()+amount);
                                    playTransactionCompleted(player);
                                    sendNewBalancePlayer(player);
                                } else {
                                    player.closeInventory();
                                    playDenyShopSound(player);
                                    sendMessageToPlayer(player, getTransactionErrorTranslated(r.errorMessage), sErrorColor);
                                    return;
                                }

                                break;

                            case 49: // Close interface
                                player.closeInventory();
                                return;

                            case 50: // ADD REMOVE ITEMS
                                player.closeInventory(); // We need to close the other one

                                if(shopAdmin.isShopOwner(player)) {
                                    guiCMD = new GuiCMD(player, "addOrRemoveItems", location);
                                    guiCMD.render(shopAdmin);
                                }
                                return; // We must return we reRender gui adminShop at the bottom

                            case 53: // FLAG OP SHOP
                                if(havePermission(player, "admin")) {
                                    shopAdmin.setInfinity(!shopAdmin.hasInfinity());
                                }
                                break;

                            default:
                                // Nothing to do
                                break;
                        }

                        // ReRender gui
                        guiCMD = new GuiCMD(player, "adminShop", location);
                        guiCMD.render(shopAdmin);
                    }
                   break;


                case "changeshopitem":
                    // Only Admin can change item from a shop
                    shopAdmin = new ShopAdmin(player, getDataFolder(), location, true);
                    if(shopAdmin.isShopOwner(player)) {

                        a_objShowItemShop.put(location, shopAdmin.getItemStack().getType());

                        // Inventory click
                        if(e.getClickedInventory().getType() == InventoryType.CHEST) {
                            switch(e.getSlot()) {
                                case 0: // Go back;
                                    player.closeInventory();
                                    guiCMD = new GuiCMD(player, "adminShop", location);
                                    guiCMD.render(shopAdmin);
                                    return;

                                case 8: // Close
                                    player.closeInventory();
                                    return;
                            }
                        }
                        else if(e.getClickedInventory().getType() == InventoryType.PLAYER) {
                            // Inventory player
                            // If not null change item from the shop

                            ItemStack itemStackClicked = e.getClickedInventory().getItem(e.getSlot());
                            if(itemStackClicked != null) {
                                // we clone the item and change the qts
                                ItemStack itemStackClone = itemStackClicked.clone();
                                itemStackClone.setAmount(1);
                                shopAdmin.setItemStack(itemStackClone);

                                // ReRender gui
                                guiCMD = new GuiCMD(player, "changeShopItem", location);
                                guiCMD.render(shopAdmin);

                            }

                        }
                    }
                    break;

                default:
                    LOG.warning(a_sSplitName[1].toLowerCase()+" is not implemented");
            }
        }
    }

    private int getAmountOfItemsInventoryPlayer(Player player, ItemStack itemStackClone, int iQts) {

        int iQtsInventory = 0;

        for(ItemStack itemstack : player.getInventory().getContents()) {
            if(itemstack != null && itemStackClone.isSimilar(itemstack)) { // no empty slot
                iQtsInventory = iQtsInventory + itemstack.getAmount();
            }
        }

        return iQtsInventory;
    }

    private int removeItemsFromInventoryPlayer(Player player, ItemStack itemStackClone, int iQts) {

        int iQtsLeft = iQts;
        int iQtsSold = 0;

        for(ItemStack itemstack : player.getInventory().getContents()) {
            if(iQtsLeft > 0 && itemstack != null && itemStackClone.isSimilar(itemstack)) { // empty slot
                if (itemstack.getAmount() > iQtsLeft){
                    iQtsSold = iQtsSold + iQtsLeft;
                    itemstack.setAmount(itemstack.getAmount() - iQtsLeft);
                    return iQtsSold;
                }
                else if(itemstack.getAmount() <= iQtsLeft) {
                    iQtsLeft = iQtsLeft - itemstack.getAmount();
                    iQtsSold = iQtsSold + itemstack.getAmount();
                    itemstack.setAmount(0);
                    //player.getInventory().remove(itemstack); // This remove all matching not what I want
                }
            }
        }

        return iQtsSold;
    }


    private void sendNewBalancePlayer(Player player) {
        sendMessageToPlayer(player, "Balance: "+sObjectColor+econ.format(econ.getBalance(player)), sOrangeColor);
    }

    @EventHandler
    private void onUserBalanceUpdate(UserBalanceUpdateEvent event) {
        // This is used when transaction was made by players
        // TODO: Also keep the last 100 transactions made?
        consoleLog("saveMoneyPlayerPerGroup... ");

        String sGroupName = getGroupNameByWorld(event.getPlayer().getWorld().getName());
        saveMoneyPlayerInGroup(event.getPlayer(),sGroupName, Double.parseDouble(event.getNewBalance()+""), false);

        // Take the world from the player online

        consoleLog("==============================================");
        consoleLog("player: "+event.getPlayer().getName());
        consoleLog("world: "+event.getPlayer().getWorld().getName());
        consoleLog("Cause: "+event.getCause().name());
        consoleLog("oldBalance: "+event.getOldBalance());
        consoleLog("NewBalance: "+event.getNewBalance());
        consoleLog("==============================================");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player player = (Player) e.getPlayer();

        // Cancel task autoSave
        //handleChangingWorldAutoSave(player);

        // We check if world is in a group
        LOG.warning("checkGroupWorld...");
        checkGroupWorld(player);

        // Save player money from last world
        LOG.warning("saveMoneyPlayerPerGroup...");
        saveMoneyPlayerPerGroup(player, e.getFrom().getName());

        // Check if we are in the same group otherwise clear and load
        LOG.warning("isWorldInSameGroup...");
        if(!isWorldInSameGroup(e.getFrom().getName(), player.getWorld().getName())) {
            ////////////////////////////////////////
            //Not same world we do nothing more
            ////////////////////////////////////////

            // Load player money from group World
            LOG.warning("loadMoneyPlayerPerWorld...");
            loadMoneyPlayerPerWorld(player, player.getWorld().getName());

        }
    }

    /**
     * Return true if the 2 worlds is in the same group
     * @param world1
     * @param world2
     * @return
     */
    private boolean isWorldInSameGroup(String world1, String world2) {

        String sGroupWorld1 = null;
        String sGroupWorld2 = null;

        // Check if this new world are listed or not
        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
            for(String sWorld : dataFile.getStringList("group."+sGroup)) {
                if(sWorld.equalsIgnoreCase(world1.toLowerCase())) {
                    // We found the group world1
                    sGroupWorld1 = sGroup;
                }
                if(sWorld.equalsIgnoreCase(world2.toLowerCase())) {
                    // We found the group world1
                    sGroupWorld2 = sGroup;
                }

                // if we have our two group exit the loop
                if(sGroupWorld1 != null && sGroupWorld2 != null) {
                    break; // optimization
                }
            }

            // if we have our two group exit the loop
            if(sGroupWorld1 != null && sGroupWorld2 != null) {
                break; // optimization
            }
        }

        //messageToConsole(sGroupWorld1+" == "+sGroupWorld2+" ? "+sGroupWorld1.equalsIgnoreCase(sGroupWorld2));
        return sGroupWorld1 != null && sGroupWorld1.equalsIgnoreCase(sGroupWorld2);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {

        OfflinePlayer playerOff = e.getPlayer();
        Player player = e.getPlayer();

        // First we check if world is in a group
        checkGroupWorld(player);

        // Load player money from group World
        loadMoneyPlayerPerWorld(playerOff, ""+player.getWorld().getName());

        // Resave the baltop
        savePlayerBalTopFile(player);

        // if we have times in auto-save create it
        //addAutoSave(player);

    }

    /*
    private void addAutoSave(Player player) {

        int iAutoUpdatePlayer = config.getInt("iAutoUpdatePlayer");

        if(iAutoUpdatePlayer>0) {
            int iFistSaveIn = (new Random()).nextInt(3)+iAutoUpdatePlayer;
            iFistSaveIn = iFistSaveIn * 20 * 60;

            int iTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                public void run()
                {
                    saveMoneyPlayerPerGroup(player, "");
                }
            }, iFistSaveIn, iAutoUpdatePlayer*20*60);
            a_AutoSaveHandler.put(player.getUniqueId(), iTask);
        }
    }
    */

    private void savePlayerBalTopFile(Player player) {

        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player, true);
        config = YamlConfiguration.loadConfiguration(file);

        double dAmount = 0.0;

        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
            dAmount = config.getDouble("Player."+sGroup);
            configBaltop.set(sGroup+"."+player.getName(), econ.format(dAmount));
        }

        try {
            configBaltop.save(baltopf);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        // remove any a_AutoSaveHandler
        //cancelPlayerAutoSave(player);
        //a_AutoSaveHandler.remove(player.getUniqueId());

        // Save current money group world
        saveMoneyPlayerPerGroup(player, player.getWorld().getName());
    }

    public boolean clearMoneyPlayer(Player player) {

        // we need to check if we have a negative value here
        EconomyResponse r;
        if(econ.getBalance(player) <= 0) {
            // We give the money back to 0
            r = econ.depositPlayer(player, Math.abs(econ.getBalance(player)));
        }
        else {
            // We remove the money back to 0
            r = econ.withdrawPlayer(player, econ.getBalance(player));
        }

        if(r.transactionSuccess()) {
            return true;
        } else {
            LOG.info(String.format("An error occurred: %s", r.errorMessage));
            return false;
        }
    }

    public boolean clearMoneyPlayer(OfflinePlayer player) {

        // we need to check if we have a negative value here
        EconomyResponse r;
        if(econ.getBalance(player) <= 0) {
            // We give the money back to 0
            r = econ.depositPlayer(player, Math.abs(econ.getBalance(player)));
        }
        else {
            // We remove the money back to 0
            r = econ.withdrawPlayer(player, econ.getBalance(player));
        }
        if(r.transactionSuccess()) {
            return true;
        } else {
            LOG.info(String.format("An error occurred: %s", r.errorMessage));
            return false;
        }

    }

    private void loadMoneyReload() {
        // on reload all player online must load from the file. To bad for losing money
        for(Player player : Bukkit.getOnlinePlayers()) {

            // Cancel auto-save if enabled
            //cancelPlayerAutoSave(player);
            //a_AutoSaveHandler.remove(player.getUniqueId());

            loadMoneyPlayerPerWorld(player, player.getWorld().getName());
            //addAutoSave(player);
        }
    }

    public void loadMoneyPlayerPerWorld(OfflinePlayer player, String sWorld) {
        String sGroup = getGroupNameByWorld(sWorld);
        loadMoneyPlayerPerGroup(player, sGroup);
    }

    public void loadMoneyPlayerPerGroup(OfflinePlayer player, String sGroup) {

        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player, false);
        config = YamlConfiguration.loadConfiguration(file);

        double dAmount = 0.0;
        if(!config.isSet("Player."+sGroup)){

            // Group not set, we take amount from config
            dAmount = 0.0;
            if(dataFile.isSet("groupinfo."+sGroup+".startingbalance")) {
                dAmount = dataFile.getDouble("groupinfo."+sGroup+".startingbalance");
            }
            config.set("Player."+sGroup, dAmount);

        }
        else {
            dAmount = config.getDouble("Player."+sGroup);
        }

        if(!config.isSet("Auction."+sGroup+".maxItem")) {
            config.set("Auction."+sGroup+".maxItem", 3);
        }

        // New param so we update every time we load a new group
        config.set("Player.currentGroup", sGroup);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // clear money before loading amount
        if(clearMoneyPlayer(player)) {
            // Check if it's a negative value
            EconomyResponse r;
            if(dAmount < 0.0) {
                // Less we remove
                r = econ.withdrawPlayer(player, Math.abs(dAmount));
                //messageToConsole("loadMoneyPlayerPerGroup withdrawPlayer: "+dAmount);
            }
            else {
                // More we give the money from Group
                r = econ.depositPlayer(player, dAmount);
                //messageToConsole("loadMoneyPlayerPerGroup depositPlayer: "+dAmount);
            }
            if(r.transactionSuccess()) {} else {
                LOG.info(String.format("An error occured: %s", r.errorMessage));
            }
        }
        else {
            LOG.info("unable to clear money from player '"+player.getName()+"'");
        }
    }

    /** Copy in the save function below **/
    /** Copy in the save function below **/
    /** Copy in the save function below **/
    /** Copy in the save function below **/
    private File getFileMoneyPlayerPerGroup(Player player) {
        // CREATE PLAYER FILE
        FileConfiguration config = null;
        File file = new File(getDataFolder()+File.separator+"Player", player.getUniqueId()+".yml");

        if(!file.exists()){
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // we take the default from the config amount
            double dAmount = 0.0;
            if(dataFile.isSet("groupinfo.default.startingbalance")) {
                dAmount = dataFile.getDouble("groupinfo.default.startingbalance");
            }

            config = YamlConfiguration.loadConfiguration(file);
            config.set("Player.Name", player.getName());
            config.set("Player.LastConnection", ""+returnDateHour());
            config.set("Player.default", dAmount);

            // clear money before loading amount
            clearMoneyPlayer(player);

            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    /** Copy in the save function upper **/
    /** Copy in the save function upper **/
    /** Copy in the save function upper **/
    /** Copy in the save function upper **/
    private File getFileMoneyPlayerPerGroup(OfflinePlayer player, boolean bIgnoreLoad) {
        // CREATE PLAYER FILE
        FileConfiguration config = null;
        File file = new File(getDataFolder()+File.separator+"Player", player.getUniqueId()+".yml");

        if(!file.exists()){
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // we take the default from the config amount
            double dAmount = 0.0;
            if(dataFile.isSet("groupinfo.default.startingbalance")) {
                dAmount = dataFile.getDouble("groupinfo.default.startingbalance");
            }

            config = YamlConfiguration.loadConfiguration(file);
            config.set("Player.Name", player.getName());
            config.set("Player.LastConnection", ""+returnDateHour());
            config.set("Player.default", dAmount);

            // clear money before loading amount
            if(!bIgnoreLoad) {
                clearMoneyPlayer(player);
            }

            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public void saveMoneyPlayerPerGroup(Player player, String sWorld) {

        // find the current world (if empty mean we schedule it)
        if(sWorld.isEmpty()) {
            sWorld = player.getWorld().getName();
        }

        // Find our group
        String sGroupFrom = getGroupNameByWorld(sWorld);

        // Save data group
        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player);

        config = YamlConfiguration.loadConfiguration(file);
        double dAmount = (double) econ.getBalance(player);
        config.set("Player."+sGroupFrom, dAmount);
        config.set("Player.LastConnection", returnDateHour());

        // Save bal in the baltop files
        configBaltop.set(sGroupFrom+"."+player.getName(), econ.format(dAmount));

        try {
            config.save(file);
            configBaltop.save(baltopf);
            //messageToConsole("saveMoneyPlayerPerGroup Player."+sGroupFrom+": "+ dAmount);
            //messageToConsole("a_AutoSaveHandler: "+a_AutoSaveHandler.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAuctionLimitForGroup(OfflinePlayer player, String sGroup, int dAmount) {
        // Save data group
        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player, true);

        config = YamlConfiguration.loadConfiguration(file);
        config.set("Auction."+sGroup+".maxItem", dAmount);

        try {
            config.save(file);
            configBaltop.save(baltopf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMoneyPlayerInGroup(OfflinePlayer player, String sGroup, Double dAmount, boolean bIgnoreLoad) {

        // Save data group
        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player, bIgnoreLoad);

        config = YamlConfiguration.loadConfiguration(file);
        config.set("Player."+sGroup, (double) dAmount);

        // Save bal in the baltop files
        configBaltop.set(sGroup+"."+player.getName(), econ.format((double) dAmount));

        try {
            config.save(file);
            configBaltop.save(baltopf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getGroupNameByWorld(String sWorldName) {

        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
            for(String sWorld : dataFile.getStringList("group."+sGroup)) {
                if(sWorld.equalsIgnoreCase(sWorldName.toLowerCase())) {
                    // WE ARE IN
                    return sGroup;
                }
            }
        }

        // Pass here we are not able to find our group so create it
        return addAutomaticNewWorld(sWorldName);

    }

    public boolean addNewGroup(CommandSender sender, String sGroup, String sAmount) {

        // Due to case sensitive in file we lower case groups
        sGroup = sGroup.toLowerCase();

        dataFile.getConfigurationSection("group").createSection(sGroup);

        // Check if the string is an amount
        Double iAmount = 0.0;
        try {
            iAmount = Double.parseDouble(sAmount);
        }
        catch(NumberFormatException e) {
            sendMessageToPlayer(sender, "notAnAmount", sErrorColor, sAmount);
            return false;
        }
        catch(NullPointerException e) {
            sendMessageToPlayer(sender, "notAnAmount", sErrorColor, sAmount);
            return false;
        }

        // Pass all test, update value
        dataFile.set("groupinfo."+sGroup+".startingbalance", iAmount);

        try {
            dataFile.save(dataFilef);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setGroupAmount(CommandSender sender, String sGroup, String sAmount) {

        // CHECK if group exist first
        boolean bGroupExist = false;
        for(String sGroupCheck : dataFile.getConfigurationSection("group").getKeys(false)){
            if(sGroupCheck.toLowerCase().equalsIgnoreCase(sGroup)) {
                bGroupExist = true;
                break;
            }
        }

        if(!bGroupExist) {
            List<String> a_sReplace = new ArrayList<String>();
           a_sReplace.add(sGroup);
          sendMessageToPlayer(sender, "noGroupFound", sErrorColor, a_sReplace);
          return false;
        }

        // Check if the string is an amount
        Double iAmount = 0.0;
        try {
            iAmount = Double.parseDouble(sAmount);
        }
        catch(NumberFormatException e) {
            sendMessageToPlayer(sender, "notAnAmount", sErrorColor, sAmount);
            return false;
        }
        catch(NullPointerException e) {
            sendMessageToPlayer(sender, "notAnAmount", sErrorColor, sAmount);
            return false;
        }

        // Pass all test, update value
        dataFile.set("groupinfo."+sGroup+".startingbalance", iAmount);
        try {
            dataFile.save(dataFilef);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteGroup(String sGroup, CommandSender sender) {

        List<String> a_sGroupDefault = dataFile.getStringList("group.default");
        List<String> a_sGroupDeleted = dataFile.getStringList("group."+sGroup);

        // Move all world to default
        for(String sWorld : a_sGroupDeleted) {
            a_sGroupDefault.add(sWorld);
        }
        dataFile.set("group.default", a_sGroupDefault);
        dataFile.set("group."+sGroup, null);

        try {
            dataFile.save(dataFilef);
            sendMessageToPlayer(sender, "deletedGroup", sObjectColor, sErrorColor+sGroup+sObjectColor);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean moveWorld2Group(String sWorldMove, String sGroupTo, CommandSender sender) {

        boolean bGroupExist = false;
        boolean bWorldExist = false;
        String sGoodGroup = "";
        String sWrongGroup = "";
        String sGoodWorld = sWorldMove;

        // First we need to loop and found if group exist
        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){

            if(sGroup.equalsIgnoreCase(sGroupTo)) {
                sGoodGroup = sGroup;
                bGroupExist = true;
            }

            // Now loop on World
            for(String sWorld : dataFile.getStringList("group."+sGroup)) {
                if(sWorld.equalsIgnoreCase(sWorldMove)) {
                    bWorldExist = true;
                    sGoodWorld = sWorld;
                    sWrongGroup = sGroup;
                }
            }
        }

        if(!bGroupExist) {
            sendMessageToPlayer(sender, "groupNotExist", sErrorColor, sGroupTo);
            return false;
        }

        if(bWorldExist) {
            //We need to move it
            List<String> a_sGroupDefault = dataFile.getStringList("group."+sWrongGroup);
            List<String> a_sGroupNew = dataFile.getStringList("group."+sGoodGroup);
            List<String> a_sGroupClean = new ArrayList<String>();

            a_sGroupNew.add(sGoodWorld);
            for(String sWorldListed : a_sGroupDefault) {
                if(!sWorldListed.equalsIgnoreCase(sGoodWorld.toLowerCase())) {
                    a_sGroupClean.add(sWorldListed);
                }
            }

            // Change old Group
            dataFile.set("group."+sWrongGroup, a_sGroupClean);
            dataFile.set("group."+sGoodGroup, a_sGroupNew);

        }
        else {

            if (Bukkit.getWorld(sGoodWorld) != null) {
                // Just add it to group
                List<String> a_sGroupDefault = dataFile.getStringList("group."+sGoodGroup);
                a_sGroupDefault.add(sGoodWorld);
                dataFile.set("group."+sGoodGroup, a_sGroupDefault);
            }
            else {
                sendMessageToPlayer(sender, "worldNotExist", sErrorColor, sGoodWorld);
                return false;
            }
        }

        try {
            dataFile.save(dataFilef);
            List<String> a_sReplace = new ArrayList<String>();
            a_sReplace.add(sErrorColor+sWorldMove+sObjectColor);
            a_sReplace.add(sErrorColor+sGroupTo+sObjectColor);
            sendMessageToPlayer(sender, "worldMoved", sErrorColor, a_sReplace);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String addAutomaticNewWorld(String sWorld) {

        String sGroup = "";
        if(config.getBoolean("newWorldInDefault")) {
            List<String> a_sGroupDefault = dataFile.getStringList("group.default");
            a_sGroupDefault.add(sWorld);
            dataFile.set("group.default", a_sGroupDefault);
            LOG.info(ANSI_YELLOW+"[TheMultiWorldMoney]"+ANSI_RESET+" added {"+ANSI_CYAN+sWorld+ANSI_RESET+"} to "+ANSI_CYAN+"group.default"+ANSI_RESET);
            sGroup = "default";
        }
        else {
            List<String> aNewGroup = new ArrayList<>();
            aNewGroup.add(sWorld);
            dataFile.set("group."+sWorld+"Group", aNewGroup);
            LOG.info(ANSI_YELLOW+"[TheMultiWorldMoney]"+ANSI_RESET+" added {"+ANSI_CYAN+sWorld+ANSI_RESET+"} to "+ANSI_CYAN+"group."+sWorld+"Group"+ANSI_RESET);
            sGroup = sWorld+"Group";
        }

        try {
            dataFile.save(dataFilef);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sGroup;
    }

    public void checkGroupWorld(Player player) {

        // Current World
        World world = player.getWorld();
        boolean bInGroup = false;

        // Check if this new world are listed or not
        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
            for(String sWorld : dataFile.getStringList("group."+sGroup)) {
                if(sWorld.toLowerCase().equalsIgnoreCase(world.getName().toLowerCase())) {
                    // WE ARE IN
                    bInGroup = true;
                }
            }
        }

        // Our world is not listed so check if we create a new group or place it in default one
        if(!bInGroup) {
            addAutomaticNewWorld(world.getName());
        }

    }

    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            LOG.severe(String.format(ANSI_RED+"[%s] - Disabled due to no Vault plugin!"+ANSI_RESET, getDescription().getName()));
            return false;
        }

        // First we check if there is already a service
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            // Bad luck is not working
            LOG.severe(String.format(ANSI_RED+"[%s] - Disabled due to no EssentialX Economy plugin!"+ANSI_RESET, getDescription().getName()));
            return false;
        }

        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if(rsp != null) {
            chat = rsp.getProvider();
            return true;
        }
        return false;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if(rsp != null) {
            perms = rsp.getProvider();
            return true;
        }
        return false;
    }

    private void helpMenuPlayer(CommandSender sender) {
        sender.sendMessage(sPluginName);
        if(sender.isOp()) {
            sStartLine = "/tmwm player";
            sender.sendMessage(sErrorColor+sStartLine+" [PLAYERNAME] list\n"+sCorrectColor+"  \\-- "+getTranslatedKeys("helpList"));
            sender.sendMessage("\n"+sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] deposit [AMOUNT]\n"+sCorrectColor+"  \\-- "+getTranslatedKeys("helpDeposit"));
            sender.sendMessage("\n"+sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] withdraw [AMOUNT]\n"+sCorrectColor+"  \\-- "+getTranslatedKeys("helpWithdraw"));
            sender.sendMessage("\n"+sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] set [AMOUNT]\n"+sCorrectColor+"  \\-- "+getTranslatedKeys("helpSet"));
            sender.sendMessage("\n"+sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] set_auction_limit [AMOUNT]\n"+sCorrectColor+"  \\-- "+getTranslatedKeys("helpSetAuctionLimit"));
            sender.sendMessage(".");
            sStartLine = "";
        }
        else {
            sendMessageToPlayer(sender, "opPermission", sErrorColor);
        }
    }

    private void helpMenuPay(CommandSender sender) {
        sender.sendMessage(sPluginName);
        if(havePermission(sender, "pay")) {
            sStartLine = "/tmwm pay";
            sender.sendMessage(sErrorColor+sStartLine+" [PLAYERNAME] [AMOUNT]");
            sender.sendMessage("§7also /payto [PLAYERNAME] [AMOUNT]");
        }
        else {
            sendMessageToPlayer(sender, "havePermission", sErrorColor, "pay");
        }
    }

    private void payPlayer(Player playerDonator, String playerName, String amount) {

        // Check if playerName is online
        Player playerReceive = Bukkit.getPlayer(playerName);
        if(playerReceive == null) {
            sendMessageToPlayer(playerDonator, "notOnline", sErrorColor, playerName);
            return;
        }

        // Check if both is in the same world (config for distance?)
        if(!playerDonator.getWorld().getName().equalsIgnoreCase(playerReceive.getWorld().getName().toLowerCase())) {
            sendMessageToPlayer(playerDonator, "notInSameWorld", sErrorColor, playerName);
            return;
        }

        // Convert string amount in double
        Double dAmount = Double.parseDouble(amount);
        if(dAmount < 0.01) {
            sendMessageToPlayer(playerDonator, "notEnoughAmount", sErrorColor);
            return;
        }

        // Check if this player have this amount in his balance
        double iPlayerDonatorBal = econ.getBalance(playerDonator);
        if(dAmount > iPlayerDonatorBal) {
            sendMessageToPlayer(playerDonator, "notEnoughMoney", sErrorColor);
            return;
        }

        // Remove from the player the amount
        boolean bWithdrawOK = true;
        EconomyResponse r = econ.withdrawPlayer(playerDonator,dAmount);
        if(r.transactionSuccess()) {} else {
            sendMessageToPlayer(playerDonator, "transactionFailed", sErrorColor);
            LOG.info(String.format("An error occured: %s", r.errorMessage));
            bWithdrawOK = false;
        }

        // Give the amount to the new player
        if(bWithdrawOK) {
            r = econ.depositPlayer(playerReceive, dAmount);
            if (r.transactionSuccess()) {
            } else {
                sendMessageToPlayer(playerDonator, "transactionFailed", sErrorColor);
                LOG.info(String.format("An error occured: %s", r.errorMessage));
            }
        }

        // Send Message to both if Eco was successful if ECO was not send its own message
        List<String> a_sReplace = new ArrayList<>();
        a_sReplace.add("§a-"+econ.format(dAmount).replaceAll("[^\\d.]", "")+"§r");
        a_sReplace.add("§a"+playerName+"§r");

        // Donator message
        sendMessageToPlayer(playerDonator, "paidTo", "",a_sReplace);

        a_sReplace.clear();
        a_sReplace.add("§a"+playerDonator.getName()+"§r");
        a_sReplace.add("§a"+econ.format(dAmount).replaceAll("[^\\d.]", "")+" §r");

        // Receiver message
        sendMessageToPlayer(playerReceive, "paidFrom", "", a_sReplace);
    }

    // Ratio killed vs killer
    @EventHandler
    public void getKiller(EntityDeathEvent event) {

        LivingEntity playerDie = event.getEntity();
        if(!(playerDie instanceof Player)) {
            return;
        }

        EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if ((entityDamageEvent != null) && !entityDamageEvent.isCancelled() && (entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();

            if (damager instanceof Projectile) {
                LivingEntity shooter = (LivingEntity) ((Projectile) damager).getShooter();
                if (shooter != null) {
                    if(shooter instanceof Player) {
                        String sGroup = getGroupNameByWorld(shooter.getWorld().getName());
                        setKilledPlayers((Player) shooter, sGroup);

                        // If die by another player
                        setDiedByPlayers((Player) playerDie, sGroup);


                    }
                    return;
                }
            }

            if(damager instanceof Player) {
                String sGroup = getGroupNameByWorld(damager.getWorld().getName());
                setKilledPlayers((Player) damager, sGroup);

                // If die by another player
                setDiedByPlayers((Player) playerDie, sGroup);
            }

            return;
        }

        return;
    }

    private void setDiedByPlayers(Player player, String sGroup) {
        // Save data group
        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player);

        config = YamlConfiguration.loadConfiguration(file);
        int iKills = 1;
        if(config.isSet("Player.diedByPlayers."+sGroup)) {
            iKills = config.getInt("Player.diedByPlayers."+sGroup) + 1;
        }
        config.set("Player.diedByPlayers."+sGroup, iKills);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setKilledPlayers(Player player, String sGroup) {
        // Save data group
        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player);

        config = YamlConfiguration.loadConfiguration(file);
        int iKills = 1;
        if(config.isSet("Player.killedPlayers."+sGroup)) {
            iKills = config.getInt("Player.killedPlayers."+sGroup) + 1;
        }
        config.set("Player.killedPlayers."+sGroup, iKills);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getKilledPlayers(Player player, String groupWorld) {
        // Get the file player
        File playerFile = getFileMoneyPlayerPerGroup(player);
        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        // make the name lowercase
        String groupWorldlow = groupWorld.toLowerCase();

        int killedCount = 0;
        int diedCount = 0;
        double rateCount = 0.0;

        // Check if the groupWorld exist if not return 0
        if(playerConfig.isSet("Player.killedPlayers."+groupWorldlow)) {
            killedCount = playerConfig.getInt("Player.killedPlayers."+groupWorldlow);
        }

        if(playerConfig.isSet("Player.diedByPlayers."+groupWorldlow)) {
            diedCount = playerConfig.getInt("Player.diedByPlayers."+groupWorldlow);
        }

        if(diedCount != 0) {
            rateCount = killedCount / (double) diedCount;
        }

        List<String> a_sReplace = new ArrayList<String>();
        a_sReplace.add(groupWorld.toUpperCase()+sResetColor);
        a_sReplace.add(sYellowColor+killedCount+sResetColor);
        a_sReplace.add(sYellowColor+diedCount+sResetColor);

        sendMessageToPlayer(player, "killVsDeath", sYellowColor, a_sReplace);
        if(diedCount != 0) {
            sendMessageToPlayer(player, "yourRate", "", sYellowColor+df.format(rateCount)+sResetColor);
        }
        else {
            sendMessageToPlayer(player, "yourRate", "", "PERFECT!!");
        }
    }

    private void sendMessageToConsoleCannotUse(CommandSender sender) {
        sendMessageToPlayer(sender, "You cannot use this command in the console", "");
    }


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        boolean bConsoleOrOP = (!(sender instanceof Player) || sender.isOp());

        boolean bConsole = !(sender instanceof Player);

        Player player = null;
        if(!bConsole) {
            player = (Player) sender;
        }

        boolean bThisModCommand = command.getName().equalsIgnoreCase("themultiworldmoney") || command.getName().equalsIgnoreCase("tmwm");

        switch(command.getName().toLowerCase()) {

            case "auction":
            case "ah":
            case "ach":
            case "ac":
            case "hdv":

                // first we check is enabled here by default is true if false just send message
                String sGroupWorld = getGroupNameByWorld(player.getWorld().getName());
                if(dataFile.isSet("groupinfo."+sGroupWorld+".auctionHouseEnable") && !dataFile.getBoolean("groupinfo."+sGroupWorld+".auctionHouseEnable")) {
                    sendMessageToPlayer(player, "auctionNotActivated", sOrangeColor);
                    return true;
                }

                AuctionHouse auctionHouse = new AuctionHouse(player, getDataFolder(), getGroupNameByWorld(player.getWorld().getName()));
                String commandAc = "";
                if(args.length > 0) {
                    commandAc = args[0];
                }

                switch(commandAc.toLowerCase()) {

                    case "expiration":

                        int iPage = 1;
                        if(args.length > 1) {
                            try {
                                iPage = Integer.parseInt(args[1]);
                                if(iPage < 1) {
                                    iPage = 1;
                                }
                            }
                            catch(NumberFormatException e) {
                                iPage = 1;
                            }
                        }

                        GuiCMD guiCMD = new GuiCMD(player, "ahExpired", player.getLocation());
                        guiCMD.render(auctionHouse, iPage);
                        return true;

                    case "sell":

                        File file = getFileMoneyPlayerPerGroup(player, false);
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                        // we check if we have reach the limit
                        int iLimit = 3;
                        if(config.isSet("Auction."+sGroupWorld+".maxItem")) {
                            iLimit = config.getInt("Auction."+sGroupWorld+".maxItem");
                        }
                        if(iLimit != -1 && auctionHouse.getItemsCountByPlayer(player) >= iLimit) {
                            ArrayList<String> a_sWorldReplace = new ArrayList<>();
                            a_sWorldReplace.add(sErrorColor+iLimit+sOrangeColor+" item"+(iLimit>1?"s":""));
                            sendMessageToPlayer(player,"auctionLimit", sOrangeColor, a_sWorldReplace);
                            return true;
                        }

                        if(args.length > 1) {
                            try {
                                double price = Double.parseDouble(args[1]);

                                int iQts = 1;
                                if(args.length > 2) {
                                    try {
                                        iQts = Integer.parseInt(args[2]);
                                    }
                                    catch(NumberFormatException e) {
                                        iQts = 1;
                                    }
                                }

                                ItemStack itemStackSell = player.getInventory().getItemInMainHand();
                                if(itemStackSell == null || itemStackSell.getType().isAir()) {
                                    sendMessageToPlayer(player, "nothingInMainHand", sErrorColor);
                                    return true;
                                }
                                int maxQts = itemStackSell.getAmount();
                                if(iQts > maxQts) {
                                    iQts = maxQts;
                                }

                                itemStackSell = player.getInventory().getItemInMainHand().clone();
                                itemStackSell.setAmount(iQts);
                                player.getInventory().getItemInMainHand().setAmount(maxQts-iQts);

                                Calendar calendar = Calendar.getInstance();
                                calendar.setTimeZone(TimeZone.getTimeZone(sTimezone));

                                AuctionItem auctionItem = new AuctionItem(player,itemStackSell, price, calendar.getTimeInMillis()+"");
                                auctionHouse.addAuctionItem(auctionItem);
                                auctionHouse.saveOnFile();
                                sendMessageToPlayer(player, "itemAdded", sCorrectColor);
                                return true;
                            }
                            catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }

                        // If we reach here we got an error
                        sendMessageToPlayer(player, "goodWayToDo", sErrorColor);
                        sendMessageToPlayer(player, "/auction sell [price] [qts]", sOrangeColor);
                        break;

                    case "":
                    case "display":
                    default:

                        iPage = 1;
                        if(args.length > 0) {
                            try {
                                iPage = Integer.parseInt(args[0]);
                                if(iPage < 1) {
                                    iPage = 1;
                                }
                            }
                            catch(NumberFormatException e) {
                                iPage = 1;
                            }
                        }

                        guiCMD = new GuiCMD(player, "auctionHouse", player.getLocation());
                        guiCMD.render(auctionHouse, iPage);
                }
                return true;

            case "shop":
                player.performCommand("tmwm create_shop");
                return true;

            case "killedplayers":
                String worldGroup = "";
                if(args.length > 0) {
                    worldGroup = args[0];
                }
                else {
                    // Get the current group of world
                    worldGroup = getGroupNameByWorld(player.getWorld().getName());
                }
                getKilledPlayers(player,worldGroup);
                return true;

            case "payto":

                if(bConsole) {
                    sendMessageToConsoleCannotUse(sender);
                    return true;
                }

                if(args.length > 1 && havePermission(sender, "pay")) {
                    // call function
                    payPlayer(player, args[0], args[1]);
                }
                else {
                    helpMenuPay(sender);
                }

                return true;

            case "themultiworldmoney":
            case "tmwm":

                if(bThisModCommand) {

                    String arg0 = "";
                    if(args.length > 0) {
                        arg0 = args[0].toLowerCase();
                    }

                    switch(arg0) {
                        case "":
                        case "help":
                            sender.sendMessage(sPluginName);
                            sender.sendMessage(sObjectColor+command.getUsage());
                            return true;

                        case "show_armor_stand":
                            if(bConsole) {
                                sendMessageToConsoleCannotUse(sender);
                                return true;
                            }
                            if(havePermission(sender, "admin")) {
                                showAllArmorStandInvisible(player, 10);
                            }

                            return true;

                        case "pay":

                            if(bConsole) {
                                sendMessageToConsoleCannotUse(sender);
                                return true;
                            }

                            if(havePermission(sender, "pay")) {
                                // do we have all args
                                if(args.length < 3) {
                                    helpMenuPay(sender);
                                    return true;
                                }

                                // call function
                                payPlayer(player, args[1], args[2]);
                            }
                            else {
                                helpMenuPay(sender);
                            }

                            return true;

                        case "baltop":
                            if(args.length > 1) {
                                arg1 = args[1].toLowerCase();

                                int arg2 = 1;
                                if(args.length > 2) {
                                    arg2 = Integer.parseInt(args[2]);
                                    if(arg2 == 0) {
                                        arg2 = 1;
                                    }
                                }
                                sender.sendMessage(sPluginName);
                                sender.sendMessage(sObjectColor+"-- "+arg1.toUpperCase()+" --");

                                sender.sendMessage(getBalTopList(arg1, arg2));
                            }
                            else {
                                sender.sendMessage(sPluginName);
                                sender.sendMessage(sObjectColor+command.getUsage());
                            }


                            return true;

                        case "create_shop":

                            // Not a console command
                            if(bConsole) {
                                sendMessageToConsoleCannotUse(sender);
                                return true;
                            }

                            if(!havePermission(player, "create_shop")) {
                                sendMessageToPlayer(player, "havePermission", sErrorColor, "themultiworldmoney.createshop");
                                return true;
                            }

                            // create the admin shop based on item in hand (player can also create shop)
                            List<String> a_sWordReplace = new ArrayList<>();
                            a_sWordReplace.add(sErrorColor+"[TMWM]"+sOrangeColor);
                            a_sWordReplace.add(sErrorColor+"shop"+sOrangeColor);
                            sendMessageToPlayer(sender, "placeSign", sOrangeColor, a_sWordReplace);
                            //getBarrelShop(player);
                            return true;

                        case "player": // all command for player

                            if(!(havePermission(sender, "admin") || havePermission(sender, "console"))) {
                                // SEND MESSAGE NO OP
                                sendMessageToPlayer(sender, "opPermission", sErrorColor);
                                return true;
                            }

                            arg1 = "";
                            sStartLine = "/tmwm player";
                            if(args.length > 1) {
                                arg1 = args[1].toLowerCase();
                            }

                            // Param for player
                            switch(arg1) { // This supposes to be playerName
                                case "":
                                case "help":
                                    // HERE WILL BE A SMALL MENU
                                    helpMenuPlayer(sender);
                                    return true;
                            }

                            boolean bGetOne = false;
                            for(OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                if(
                                        offlinePlayer != null &&
                                        offlinePlayer.getName() != null &&
                                        offlinePlayer.getName().equalsIgnoreCase(arg1)
                                ) {
                                    // Found player Return what we are looking for
                                    String offlinePlayerName = offlinePlayer.getName();

                                    File file = getFileMoneyPlayerPerGroup(offlinePlayer, false);
                                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                                    // SubMenu after player name

                                    arg2 = "";
                                    sStartLine = "/tmwm player";
                                    if(args.length > 2) {
                                        arg2 = args[2].toLowerCase();
                                    }

                                    switch(arg2) {
                                        case "":
                                        case "help":
                                            helpMenuPlayer(sender);
                                            return true;

                                        case "list":
                                            sender.sendMessage(sPluginName);
                                            for(String sKey : config.getConfigurationSection("Player").getKeys(false)) {
                                                sender.sendMessage(sCorrectColor+sKey+": "+sYellowColor+config.getString("Player."+sKey));
                                            }
                                            return true;

                                        default:
                                            // We suppose to get a group so check if exist in player file
                                            arg3 = "";
                                            Double arg4 = 0.0;
                                            Double dTotal = 0.0;
                                            boolean bPass = false;
                                            if(args.length > 4) {
                                                arg3 = args[3].toLowerCase();
                                                arg4 = Double.parseDouble(args[4]);
                                            }

                                            handleGroupTransaction(sender,offlinePlayer, config, args[2], arg3, arg4);
                                            return true;
                                    }
                                }
                            }

                            // No player found
                            if(!bGetOne) {
                                sendMessageToPlayer(sender, "payPlayerNotFound", sErrorColor, arg1);
                            }

                            return true;

                        case "group":

                            arg1 = "";
                            sStartLine = "/tmwm group";
                            if(args.length > 1) {
                                arg1 = args[1].toLowerCase();
                            }

                            // Param for group
                            switch(arg1) {
                                case "":
                                case "help":
                                    // HERE WILL BE A SMALL MENU
                                    sender.sendMessage(sPluginName);
                                    sender.sendMessage(sObjectColor+sStartLine+" help "+sCorrectColor+"-- This Help Dah!");
                                    sender.sendMessage(sObjectColor+sStartLine+" list "+sCorrectColor+"-- List of groups & worlds");
                                    if(havePermission(sender, "admin") || havePermission(sender, "console")) {
                                        sender.sendMessage(sErrorColor+sStartLine+" add [GROUPNAME] "+sCorrectColor+"-- Add groups");
                                        sender.sendMessage(sErrorColor+sStartLine+" move [WORLDNAME] [GROUPNAME]"+sCorrectColor+"-- Move World to new group");
                                        sender.sendMessage(sErrorColor+sStartLine+" delete [GROUPNAME]"+sCorrectColor+"-- Delete group Worlds will in default");
                                    }
                                    return true;

                                case "list":
                                    sender.sendMessage(sPluginName);
                                    for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                                        sender.sendMessage(sObjectColor+"----| "+sGroup+" |--------------------------");
                                        for(String sWorld : dataFile.getStringList("group."+sGroup)) {
                                            sender.sendMessage(sCorrectColor+"  - "+sWorld);
                                        }
                                    }
                                    return true;

                                case "add":
                                    if(havePermission(sender, "admin") || havePermission(sender, "console")) {
                                        // Add new group
                                        if(args.length > 3) {
                                            if(addNewGroup(sender, args[2], args[3])) {

                                                List<String> a_sReplace = new ArrayList<String>();
                                                a_sReplace.add(sErrorColor+args[2]+sObjectColor);
                                                a_sReplace.add(sErrorColor+args[3]);

                                                sendMessageToPlayer(sender, "groupAdded", sObjectColor, a_sReplace);
                                            }
                                        }
                                        else if(args.length > 2) {
                                            if(addNewGroup(sender, args[2], "0.0")) {

                                                List<String> a_sReplace = new ArrayList<String>();
                                                a_sReplace.add(sErrorColor+args[2]+sObjectColor);
                                                a_sReplace.add(sErrorColor+"0.00");

                                                sendMessageToPlayer(sender, "groupAdded", sObjectColor, a_sReplace);
                                            }
                                        }
                                        else {
                                            sender.sendMessage(sPluginName);
                                            sender.sendMessage(sObjectColor+sStartLine+" add [GROUPNAME] [STARTING AMOUNT]");
                                        }
                                    }
                                    else {
                                        sender.sendMessage(sPluginName);
                                        sender.sendMessage(sErrorColor+"Need OP permission");
                                    }
                                    return true;

                                case "setamount":
                                    if(havePermission(sender, "admin") || havePermission(sender, "console")) {
                                        // Add new group
                                        if(args.length > 3) {
                                            if(setGroupAmount(sender, args[2], args[3])) {

                                                List<String> a_sReplace = new ArrayList<String>();
                                                a_sReplace.add(sErrorColor+args[2]+sObjectColor);
                                                a_sReplace.add(sErrorColor+args[3]);

                                                sendMessageToPlayer(sender, "groupSetAmount", sObjectColor, a_sReplace);
                                            }
                                        }
                                        else {
                                            sender.sendMessage(sPluginName);
                                            sender.sendMessage(sObjectColor+sStartLine+" setamount [GROUPNAME] [STARTING_AMOUNT]");
                                        }
                                    }
                                    else {
                                        sendMessageToPlayer(sender, "opPermission", sErrorColor);
                                    }
                                    return true;

                                case "move":
                                    if(havePermission(sender, "admin") || havePermission(sender, "console")) {
                                        // Move world to new group
                                        if(args.length > 3) {

                                            String sWorld = args[2];
                                            String sGroup = args[3];

                                            moveWorld2Group(sWorld, sGroup, sender);

                                        }
                                        else {
                                            sender.sendMessage(sPluginName);
                                            sender.sendMessage(sObjectColor+sStartLine+" move [WORLDNAME] [GROUPNAME]");
                                        }
                                    }
                                    else {
                                        sendMessageToPlayer(sender, "opPermission", sErrorColor);
                                    }
                                    return true;

                                case "delete":
                                    if(havePermission(sender, "admin") || havePermission(sender, "console")) {
                                        // Delete world to new group
                                        if(args.length > 2) {
                                            String sGroup = args[2];
                                            deleteGroup(sGroup, sender);
                                        }
                                        else {
                                            sender.sendMessage(sPluginName);
                                            sender.sendMessage(sObjectColor+sStartLine+" delete [GROUPNAME]");
                                        }
                                    }
                                    else {
                                        sendMessageToPlayer(sender, "opPermission", sErrorColor);
                                    }
                                    return true;
                            }

                        default:
                            return false;
                    }
                }

                return false;
        }
        return false;
    }

    /*
    private void getBarrelShop(Player player) {
        // This will give a special barrel to place
        ItemStack itemStack = new ItemStack(Material.BARREL, 1);
        ItemMeta im = itemStack.getItemMeta();
        im.setDisplayName(barrelShopName);
        im.setCustomModelData(1);

        itemStack.setItemMeta(im);
        player.getWorld().dropItem(player.getLocation().add(0,1,0), itemStack);
    }
    */

    private void createShop(Player player, Location locationBarrel) {

        // if file already exist load from it
        String sLocation = locationBarrel.getBlockX()+"_"+locationBarrel.getBlockY()+"_"+locationBarrel.getBlockZ();
        File file = new File(getDataFolder()+File.separator+"Shop", locationBarrel.getWorld().getName()+"_"+sLocation+".yml");

        boolean bLoadFromFile = false;
        if(file.exists()){
            bLoadFromFile = true;
        }

        ShopAdmin shopAdmin = new ShopAdmin(player, getDataFolder(), locationBarrel, bLoadFromFile);
        a_objShowItemShop.put(locationBarrel, shopAdmin.getItemStack().getType());
        if(!bLoadFromFile) {
            shopAdmin.saveOnFile();
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                consoleLog("createShop: "+shopAdmin.getItemStack().getType().name());
                shopAdmin.updateSign(shopAdmin.getItemStack());
            }
        }, 10);

        GuiCMD guiCMD = new GuiCMD(player, "adminShop", locationBarrel);
        guiCMD.render(shopAdmin);
    }

    private void openShop(Player player, Location locationBarrel) {
        player.playNote(player.getLocation(), Instrument.BELL, Note.natural(1, Note.Tone.C));
        ShopAdmin shopAdmin = new ShopAdmin(player, getDataFolder(), locationBarrel, true);
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            @Override
            public void run() {
                consoleLog("openShop: "+shopAdmin.getItemStack().getType().name());
                shopAdmin.updateSign(shopAdmin.getItemStack());
            }
        },10);
        a_objShowItemShop.put(locationBarrel, shopAdmin.getItemStack().getType());
        GuiCMD guiCMD = new GuiCMD(player, "adminShop",locationBarrel);
        guiCMD.render(shopAdmin);
    }

    private void handleGroupTransactionAh(OfflinePlayer offlinePlayer, FileConfiguration config, String sGroupName, Double dAmount, String itemName) {

        if(!sGroupName.contentEquals("[moneyGroup]") && !config.isDouble("Player."+sGroupName)) {
            return;
        }

        if(offlinePlayer == null || offlinePlayer.getName() == null) {
            return;
        }
        String offlinePlayerName = offlinePlayer.getName();

        if(sGroupName.contentEquals("[moneyGroup]")) {

            // we check current Group, if player was offline for a while it will be null
            if(!config.isSet("Player.currentGroup")) {
                LOG.warning("Unable to find the last group of world. "+offlinePlayerName+" is away for a while?");
                return;
            }
            sGroupName = config.getString("Player.currentGroup");
        }

        Double dCurrent = config.getDouble("Player."+sGroupName);

        Double dTotal = 0.0;
        //boolean bPass = false;

        String sType = "deposit";
        switch(sType) {

            case "withdraw":
                //dTotal = dCurrent - dAmount;
                //bPass = true;

            case "deposit":

                //if(!bPass) {
                    dTotal = dCurrent + dAmount;
                //}
                saveMoneyPlayerInGroup(offlinePlayer, sGroupName, dTotal, false);
                if(offlinePlayer.isOnline()) {
                    loadMoneyPlayerPerGroup(offlinePlayer, getGroupNameByWorld(((Player) offlinePlayer).getWorld().getName()));
                }

                String totalDisplay = econ.format(dTotal).replace("$", "");

                List<String> a_sReplace = new ArrayList<String>();
                a_sReplace.add(sYellowColor+totalDisplay+sCorrectColor);
                a_sReplace.add(sErrorColor+itemName+sCorrectColor);
                a_sReplace.add(sErrorColor+sGroupName);

                if(offlinePlayer.isOnline()) {
                    sendMessageToPlayer((Player) offlinePlayer, "newTransactionIn", sErrorColor, a_sReplace);
                }
                return;

            default:
                return;
        }
    }

    private void handleGroupTransaction(CommandSender sender, OfflinePlayer offlinePlayer, FileConfiguration config, String sGroupName, String sType, Double dAmount) {


        if(!sGroupName.contentEquals("[moneyGroup]") && !config.isDouble("Player."+sGroupName)) {
            sendMessageToPlayer(sender, "payGroupNotFound", sErrorColor, arg2);
            return;
        }

        if(offlinePlayer == null || offlinePlayer.getName() == null) {
            return;
        }
        String offlinePlayerName = offlinePlayer.getName();

        if(sGroupName.contentEquals("[moneyGroup]")) {

            // we check current Group, if player was offline for a while it will be null
            if(!config.isSet("Player.currentGroup")) {
                LOG.info("Unable to find the last group of world. "+offlinePlayerName+" is away for a while?");
                return;
            }
            sGroupName = config.getString("Player.currentGroup");
        }

        Double dCurrent = config.getDouble("Player."+sGroupName);

        Double dTotal = 0.0;
        boolean bPass = false;

        switch(sType) {

            case "withdraw":
                dTotal = dCurrent - dAmount;
                bPass = true;

            case "deposit":

                if(!bPass) {
                    dTotal = dCurrent + dAmount;
                }
                saveMoneyPlayerInGroup(offlinePlayer, sGroupName, dTotal, false);
                if(offlinePlayer.isOnline()) {
                    loadMoneyPlayerPerGroup(offlinePlayer, getGroupNameByWorld(((Player) offlinePlayer).getWorld().getName()));
                }

                String totalDisplay = econ.format(dTotal).replace("$", "");

                List<String> a_sReplace = new ArrayList<String>();
                a_sReplace.add(offlinePlayerName+sCorrectColor);
                a_sReplace.add(sErrorColor+totalDisplay+sCorrectColor);
                a_sReplace.add(sErrorColor+sGroupName);

                sendMessageToPlayer(sender, "haveNow", sErrorColor, a_sReplace);
                a_sReplace.clear();
                return;

            case "set":
                saveMoneyPlayerInGroup(offlinePlayer, sGroupName, dAmount, false);
                if(offlinePlayer.isOnline()) {
                    loadMoneyPlayerPerGroup(offlinePlayer, getGroupNameByWorld(((Player) offlinePlayer).getWorld().getName()));
                }

                a_sReplace = new ArrayList<>();
                a_sReplace.add(offlinePlayerName+sCorrectColor);
                a_sReplace.add(sErrorColor+dAmount+sCorrectColor);
                a_sReplace.add(sErrorColor+sGroupName);

                sendMessageToPlayer(sender, "haveNow", sErrorColor, a_sReplace);
                return;

            case "set_auction_limit":
                saveAuctionLimitForGroup(offlinePlayer, sGroupName, dAmount.intValue());
                a_sReplace = new ArrayList<>();
                a_sReplace.add(sCorrectColor+dAmount.intValue());
                sendMessageToPlayer(sender, "auctionLimitChange", sErrorColor, a_sReplace);
                return;

            case "":
            case "help":
            default:
                helpMenuPlayer(sender);
                return;
        }
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPermissions() {
        return perms;
    }

    public static Chat getChat() {
        return chat;
    }

    public static String returnDateHour() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(sTimezone));
        String sMonth = String.format("%02d", calendar.get(Calendar.MONTH));
        String sDay = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
        String sHour = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        String sMinute = String.format("%02d", calendar.get(Calendar.MINUTE));

        String sDate = ""+calendar.get(Calendar.YEAR)+"-"+sMonth+"-"+sDay+" "+sHour+":"+sMinute;
        return sDate;

    }
}

