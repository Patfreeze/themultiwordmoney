package com.amedacier.themultiworldmoney;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class TheMultiWorldMoney extends JavaPlugin implements Listener {

    static String sTimezone = "America/New_York";

    // 2.2.0
    /*
    // - Update for 1.18.2
    // - Extra added /killedplayers [group] to get the rate kills players
     */

    // 2.1.1
    /*
    // - Correction bug about bigInteger vs Integer

     */
    // 2.1.0
    /*
    // - Added the command /payto [name] [amount] alias /thepay /thepayto
    // - Added also /tmwm pay [name] [amount]
    //   You will need to remove the permission /pay from your player to not by pass travel money from group
    // - When plugin is reloaded save all player data
     */

    //New in 2.0.0 for 1.18 (on spigot)
    /*
    // Rewritten code to use Maven
    // Config.yml auto-update
    // bug correction on backup vault
    // bug correction on autocomplete
    // - Added /themultiworld baltop [group] [page]
	*/

    // SPIGOT 1.0.5

    // VAULT
    private static Economy econ = null;
    private static Chat chat = null;
    private static Permission perms = null;

    private static final DecimalFormat df = new DecimalFormat("0.00");

    String sPluginName = "§c[§eTheMultiWorldMoney - TMWM§c] "; // PlugIn Name in Yellow
    String sErrorColor = "§c"; // LightRed
    String sObjectColor = "§a"; // LightGreen
    String sCorrectColor = "§2"; // Green
    String sYellowColor = "§e"; // Yellow
    String sResetColor = "§r";

    String sVersion = getDescription().getVersion(); // version in plugin.yml

    String sStartLine = "";
    String arg1 = "";
    String arg2 = "";
    String arg3 = "";

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

    private File configf, dataFilef, baltopf;
    private FileConfiguration config, dataFile, configBaltop;

    private void createFiles() throws InvalidConfigurationException {

        configf = new File(getDataFolder(), "config.yml");
        dataFilef = new File(getDataFolder(), "data.yml");
        baltopf = new File(getDataFolder(), "baltop.yml");

        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            saveResource("config.yml", false);

        }
        // This will always be refresh
        saveResource("confighelp.yml", true);

        if (!dataFilef.exists()) {
            dataFilef.getParentFile().mkdirs();
            saveResource("data.yml", false);
        }
        if (!baltopf.exists()) {
            baltopf.getParentFile().mkdirs();
            saveResource("baltop.yml", false);
        }

        config = new YamlConfiguration();
        dataFile = new YamlConfiguration();
        configBaltop = new YamlConfiguration();


        // LOADING CONFIG FIRST
        try {
            config.load(configf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if we have field if not update version and field
        boolean isNeedUpdate = false;

        // Added in v1.0.0
        if(!config.isSet("newWorldInDefault")) {
            config.set("newWorldInDefault", true);
        }

        // Added in v2.0.0
        if(!config.isSet("baltopdelay")) {
            config.set("baltopdelay", 20);
            isNeedUpdate = true;
        }

        // Added in v2.2.0
        if(!config.isSet("namedatabase") || !config.isSet("userdatabase") || !config.isSet("passworddatabase")) {
            config.set("namedatabase", "");
            config.set("userdatabase", "");
            config.set("passworddatabase", "");
            isNeedUpdate = true;
        }

        // Si nous avons un update a faire du fichier
        if(isNeedUpdate) {
            config.set("version", sVersion);

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

        try {
            dataFile.load(dataFilef);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            configBaltop.load(baltopf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private boolean havePermission(Player p, String sType) {

        boolean bAdmin = (p.isOp() || p.hasPermission("themultiworldmoney.admin"));
        boolean bMod = (bAdmin || p.hasPermission("themultiworldmoney.mod"));

        boolean bUsePay = (bMod || p.hasPermission("themultiworldmoney.pay"));

        boolean bUsekilledPlayers = (bMod || p.hasPermission("killedplayers.use"));

        switch(sType) {
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
                System.console().printf("%s is not define in permission", sType);
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

            List<String> baltopAmount = new ArrayList<String>();
            for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                logFirstStart.set("Player."+player.getName()+".getUUID", player.getUniqueId().toString());
                logFirstStart.set("Player."+player.getName()+".amountVault", (double) econ.getBalance(player));

                for(String bank : econ.getBanks()) {
                    logFirstStart.set("Player."+player.getName()+"."+bank+".amountVault", (double) econ.bankBalance(bank).balance);
                }
                logFirstStart.set("Player."+player.getName()+".amountVault", (double) econ.getBalance(player));

                // Save current money group world
                baltopAmount.add(player.getName()+": "+econ.getBalance(player));
                saveMoneyPlayerInGroup(player, "default", econ.getBalance(player), true);
            }

            List<String> reordered = reorderArray(baltopAmount);
            configBaltop.set("lastcall", 0);
            configBaltop.set("default", "");

            try {
                logFirstStart.save(logFirstStartf);
                configBaltop.save(baltopf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            getBalTopList("default", 1);
        }
    }

    private String getBalTopList(String sGroup, int iPage) {

        // Page 1 sera 0 | 2 sera 1
        iPage = iPage-1;
        if(iPage < 0) {
            iPage = 0;
        }

        // Current times in int
        Calendar cal = Calendar.getInstance();

        // See the last date if we update or not
        if(cal.getTimeInMillis() - configBaltop.getLong("lastcall") > (config.getInt("baltopdelay")*1000)) {

            // On clean les groupes
            for(String sGroupy : dataFile.getConfigurationSection("group").getKeys(false)){
                configBaltop.set(sGroupy, "");
                configBaltop.set("max."+sGroupy, 0.0);
            }

            // Reload all files
            for(OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                FileConfiguration config = null;
                File file = getFileMoneyPlayerPerGroup(offlinePlayer, false);
                config = YamlConfiguration.loadConfiguration(file);

                // loop sur tout les groupes
                for(String sGroupy : dataFile.getConfigurationSection("group").getKeys(false)){

                    //load le groupes comme une liste
                    List<String> myList = configBaltop.getStringList(sGroupy);
                    double max = configBaltop.getDouble("max."+sGroupy);

                    // Load group
                    if(config.isSet("Player."+sGroupy)){
                        max = max + config.getDouble("Player."+sGroupy);
                        String moneyString = NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(config.getDouble("Player."+sGroupy));
                        myList.add(offlinePlayer.getName()+": "+moneyString);
                    }
                    // On re-save le resultat
                    configBaltop.set(sGroupy, myList);
                    configBaltop.set("max."+sGroupy,max);
                }
            }

            // reorder data
            for(String sGroupy : dataFile.getConfigurationSection("group").getKeys(false)){
                List<String> myList = configBaltop.getStringList(sGroupy);
                myList = reorderArray(myList);
                configBaltop.set(sGroupy, myList);
            }

            try {
                configBaltop.set("lastcall", cal.getTimeInMillis());
                configBaltop.save(baltopf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            configBaltop.load(baltopf);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        String sReturn = "";
        int iCount=1;

        // Get the list of the groups (return 10 match) based on iPage
        List<String> showList = configBaltop.getStringList(sGroup);

        for(String playerTop : showList) {
            if(iCount >= (iPage*10+1) && iCount <= (iPage*10+10)) {
                sReturn = sReturn+iCount+". "+playerTop+"\n";
            }
            iCount++;
        }

        // Si nous avons rien
        if(sReturn.equalsIgnoreCase("")) {
            sReturn = "- No data for this page -";
        }
        else {
            // Affiche total de tous
            String moneyString = NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(configBaltop.getDouble("max."+sGroup));
            sReturn = sErrorColor+"Total: "+moneyString+"\n§r"+sReturn;

            sReturn = sReturn + sObjectColor+"Next page\n/tmwm baltop "+sGroup+" "+(iPage+2)+"\n";
        }

        return sReturn;

    }


    private List<String> reorderArray(List<String> strings) {

        //List<String> strings = Arrays.asList("Patfreeze:8098.00","Yoil:8098.01","Drake:898.00","Yaya:98.00","LittlePooky:18098.00","LittleLolo:8098.00");

        Collections.sort(strings, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return extractLong(o2) > extractLong(o1) ? 1 : -1;
            }

            Long extractLong(String s) {
                String num = s.replaceAll("\\D", "");

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
                loadMoneyReload();

            }
        }, 2); // Wait a certain time because of Vault

    }

    @Override
    public void onDisable(){
        // save players money
        for(Player player : Bukkit.getOnlinePlayers()) {
            saveMoneyPlayerPerGroup(player, player.getWorld().getName());
        }
        LOG.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player player = (Player) e.getPlayer();

        // First we check if world is in a group
        checkGroupWorld(player);

        // Save player money from last world
        saveMoneyPlayerPerGroup(player, e.getFrom().getName());

        // Remove all money
        clearMoneyPlayer(player);

        // Load player money from group World
        loadMoneyPlayerPerWorld(player, player.getWorld().getName());

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {

        Player player = e.getPlayer();

        // Remove all money by secure
        clearMoneyPlayer(player);

        // First we check if world is in a group
        checkGroupWorld(player);

        // Load player money from group World
        loadMoneyPlayerPerWorld(player, ""+player.getWorld().getName());

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        // Save current money group world
        saveMoneyPlayerPerGroup(player, player.getWorld().getName());

        // REMOVE ALL MONEY
        clearMoneyPlayer(player);
    }

    public void clearMoneyPlayer(Player player) {
        EconomyResponse r = econ.withdrawPlayer(player, econ.getBalance(player));
        if(r.transactionSuccess()) {} else {
            LOG.info(String.format("An error occured: %s", r.errorMessage));
        }
    }

    public void clearMoneyPlayer(OfflinePlayer player) {
        EconomyResponse r = econ.withdrawPlayer(player, econ.getBalance(player));
        if(r.transactionSuccess()) {} else {
            LOG.info(String.format("An error occured: %s", r.errorMessage));
        }
    }

    private void loadMoneyReload() {
        // on reload all player online must load from the file. To bad for losing money
        for(Player player : Bukkit.getOnlinePlayers()) {
            loadMoneyPlayerPerWorld(player, player.getWorld().getName());
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
            config.set("Player."+sGroup, 0f);

            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            dAmount = config.getDouble("Player."+sGroup);
        }

        // clear money before loading amount
        clearMoneyPlayer(player);

        // Now give the money from Group
        EconomyResponse r = econ.depositPlayer(player, dAmount);
        if(r.transactionSuccess()) {} else {
            LOG.info(String.format("An error occured: %s", r.errorMessage));
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

            config = YamlConfiguration.loadConfiguration(file);
            config.set("Player.Name", player.getName());
            config.set("Player.LastConnection", ""+returnDateHour());
            config.set("Player.default", (double) econ.getBalance(player));

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

            config = YamlConfiguration.loadConfiguration(file);
            config.set("Player.Name", player.getName());
            config.set("Player.LastConnection", ""+returnDateHour());
            config.set("Player.default", (double) econ.getBalance(player));

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

        // Find our group
        String sGroupFrom = getGroupNameByWorld(sWorld);

        // Save data group
        FileConfiguration config = null;
        File file = getFileMoneyPlayerPerGroup(player);

        config = YamlConfiguration.loadConfiguration(file);
        config.set("Player."+sGroupFrom, (double) econ.getBalance(player));

        try {
            config.save(file);
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

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getGroupNameByWorld(String sWorldName) {

        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
            for(String sWorld : dataFile.getStringList("group."+sGroup)) {
                if(sWorld.equalsIgnoreCase(sWorldName)) {
                    // WE ARE IN
                    return sGroup;
                }
            }
        }

        // Pass here we are not able to find our group so create it
        return addAutomaticNewWorld(sWorldName);

    }

    public boolean addNewGroup(String sGroup) {
        dataFile.getConfigurationSection("group").createSection(sGroup);

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
            sender.sendMessage(sPluginName);
            sender.sendMessage(sObjectColor+"{"+sErrorColor+sGroup+sObjectColor+"} is deleted. All world is now in default");
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
            sender.sendMessage(sPluginName);
            sender.sendMessage(sErrorColor+sGroupTo+" is not exist you need to add it first");
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
                sender.sendMessage(sPluginName);
                sender.sendMessage(sErrorColor+"World {"+sGoodWorld+"} seems not exist. Check world case sensitive.");
                return false;
            }
        }

        try {
            dataFile.save(dataFilef);
            sender.sendMessage(sPluginName);
            sender.sendMessage(sObjectColor+"World {"+sErrorColor+sWorldMove+sObjectColor+"} moved to {"+sErrorColor+sGroupTo+sObjectColor+"}");
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
            List<String> aNewGroup = new ArrayList<String>();
            aNewGroup.add(sWorld);
            dataFile.set("group."+sWorld+"Group", aNewGroup);
            LOG.info(ANSI_YELLOW+"[TheMultiWorldMoney]"+ANSI_RESET+" added {"+ANSI_CYAN+sWorld+ANSI_RESET+"} to "+ANSI_CYAN+"group."+sWorld+"Group"+ANSI_RESET);
            sGroup = sWorld+"Group";
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
                if(sWorld.equalsIgnoreCase(world.getName())) {
                    // WE ARE IN
                    bInGroup = true;
                }
            }
        }

        // Our world is not listed so check if we create a new group of place it in default one
        if(!bInGroup) {
            addAutomaticNewWorld(world.getName());
        }

        try {
            dataFile.save(dataFilef);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean setupEconomy() {

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            LOG.severe(String.format(ANSI_RED+"[%s] - Disabled due to no Vault dependency found!"+ANSI_RESET, getDescription().getName()));
            return false;
        }

        // First we check if there is already a service
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            // Bad luck is not working
            LOG.severe(String.format(ANSI_RED+"[%s] - Disabled due to no Economy plugin found!"+ANSI_RESET, getDescription().getName()));
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
            sender.sendMessage(sErrorColor+sStartLine+" [PLAYERNAME] list\n"+sCorrectColor+"  \\-- List of money by group");
            sender.sendMessage(sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] deposit [AMOUNT]\n"+sCorrectColor+"  \\-- deposit to group");
            sender.sendMessage(sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] withdraw [AMOUNT]\n"+sCorrectColor+"  \\-- withdraw from group");
            sender.sendMessage(sErrorColor+sStartLine+" [PLAYERNAME] [GROUP] set [AMOUNT]\n"+sCorrectColor+"  \\-- set exact money to group");
        }
        else {
            sender.sendMessage(sErrorColor+"You don't have permission to do that!");
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
            sender.sendMessage(sErrorColor+"You don't have permission 'pay'!");
        }
    }

    private void payPlayer(Player playerDonator, String playerName, String amount) {

        // Check if playerName is online
        Player playerReceive = Bukkit.getPlayer(playerName);
        if(playerReceive == null) {
            playerDonator.sendMessage(sPluginName);
            playerDonator.sendMessage(playerName+" is not online!");
            return;
        }

        // Check if both is in the same world (config for distance?)
        if(!playerDonator.getWorld().getName().equalsIgnoreCase(playerReceive.getWorld().getName().toLowerCase())) {
            playerDonator.sendMessage(sPluginName);
            playerDonator.sendMessage(playerName+" is not in the same world as you!");
            return;
        }

        // Convert string amount in integer
        int iAmount = Integer.parseInt(amount);
        if(iAmount < 1) {
            playerDonator.sendMessage(sPluginName);
            playerDonator.sendMessage("Well! Not sure this amount is enough!");
            return;
        }

        // Check if this player have this amount in his balance
        double iPlayerDonatorBal = econ.getBalance(playerDonator);
        if(iAmount > iPlayerDonatorBal) {
            playerDonator.sendMessage(sPluginName);
            playerDonator.sendMessage("Not enough money!");
            return;
        }

        // Remove from the player the amount
        boolean bWithdrawOK = true;
        EconomyResponse r = econ.withdrawPlayer(playerDonator,iAmount);
        if(r.transactionSuccess()) {} else {
            playerDonator.sendMessage(sPluginName);
            playerDonator.sendMessage("Transaction failed sorry as an OP to check console!");
            LOG.info(String.format("An error occured: %s", r.errorMessage));
            bWithdrawOK = false;
        }

        // Give the amount to the new player
        if(bWithdrawOK) {
            r = econ.depositPlayer(playerReceive, iAmount);
            if (r.transactionSuccess()) {
            } else {
                playerDonator.sendMessage(sPluginName);
                playerDonator.sendMessage("Transaction failed sorry as an OP to check console!");
                LOG.info(String.format("An error occured: %s", r.errorMessage));
            }
        }

        // Send Message to both if Eco was successful if ECO was not send is own message
        playerDonator.sendMessage(sPluginName);
        playerDonator.sendMessage("You pay §a-$"+iAmount+"§r to §a"+playerName+"§r!"); // But amount -

        playerReceive.sendMessage(sPluginName);
        playerReceive.sendMessage("Thanks to §a"+playerDonator.getName()+"§r that give you §a$"+iAmount+".00 §r!"); // But amount +

    }
    // Ratio killed vs killer

    @EventHandler
    public Entity getKiller(EntityDeathEvent event) {

        LivingEntity playerDie = event.getEntity();
        if(!(playerDie instanceof Player)) {
            return null;
        }
        LOG.info("getKiller was call because the EntityDeath is a player");

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
                    return shooter;
                }
            }

            if(damager instanceof Player) {
                String sGroup = getGroupNameByWorld(damager.getWorld().getName());
                setKilledPlayers((Player) damager, sGroup);

                // If die by another player
                setDiedByPlayers((Player) playerDie, sGroup);
            }

            return damager;
        }

        return null;
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

        player.sendMessage(sPluginName+sResetColor+" --> "+sYellowColor+groupWorld.toUpperCase());
        player.sendMessage("You have killed "+sYellowColor+killedCount+sResetColor+" player(s)");
        player.sendMessage("You have been killed "+sYellowColor+diedCount+sResetColor+" time(s)");
        if(diedCount != 0) {
            player.sendMessage("Your rate "+sYellowColor+df.format(rateCount)+sResetColor);
        }
        else {
            player.sendMessage("Your rate PERFECT!!");
        }

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
                    sender.sendMessage(sPluginName);
                    sender.sendMessage("You cannot use this command in console");
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

                        case "pay":

                            if(bConsole) {
                                sender.sendMessage(sPluginName);
                                sender.sendMessage("You cannot use this command in console");
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

                        case "player": // all command for player

                            if(!bConsoleOrOP) {
                                // SEND MESSAGE NO OP
                                sender.sendMessage(sPluginName);
                                sender.sendMessage(sErrorColor+"You dont have the permission for that!");
                                return true;
                            }

                            arg1 = "";
                            sStartLine = "/tmwm player";
                            if(args.length > 1) {
                                arg1 = args[1].toLowerCase();
                            }

                            // Param for player
                            switch(arg1) { // This suppose to be playerName
                                case "":
                                case "help":
                                    // HERE WILL BE A SMALL MENU
                                    helpMenuPlayer(sender);
                                    return true;
                            }

                            boolean bGetOne = false;
                            for(OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                if(offlinePlayer.getName().equalsIgnoreCase(arg1)) {
                                    // Found player Return what we looking for

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
                                            if(config.isDouble("Player."+args[2])) {

                                                Double dCurrent = config.getDouble("Player."+args[2]);

                                                arg3 = "";
                                                Double arg4 = 0.0;
                                                Double dTotal = 0.0;
                                                boolean bPass = false;
                                                if(args.length > 4) {
                                                    arg3 = args[3].toLowerCase();
                                                    arg4 = Double.parseDouble(args[4]);
                                                }

                                                switch(arg3) {

                                                    case "withdraw":
                                                        dTotal = dCurrent - arg4;
                                                        bPass = true;

                                                    case "deposit":

                                                        if(!bPass) {
                                                            dTotal = dCurrent + arg4;
                                                        }
                                                        saveMoneyPlayerInGroup(offlinePlayer, args[2], dTotal, false);
                                                        loadMoneyPlayerPerGroup(offlinePlayer, args[2]);
                                                        sender.sendMessage(sPluginName);
                                                        sender.sendMessage(sErrorColor+args[1]+sCorrectColor+" have now "+sErrorColor+dTotal+sCorrectColor+" in "+sErrorColor+args[2]);
                                                        return true;

                                                    case "set":
                                                        saveMoneyPlayerInGroup(offlinePlayer, args[2], arg4, false);
                                                        loadMoneyPlayerPerGroup(offlinePlayer, args[2]);
                                                        sender.sendMessage(sPluginName);
                                                        sender.sendMessage(sErrorColor+args[1]+sCorrectColor+" have now "+sErrorColor+arg4+sCorrectColor+" in "+sErrorColor+args[2]);
                                                        return true;

                                                    case "":
                                                    case "help":
                                                    default:
                                                        helpMenuPlayer(sender);
                                                        return true;
                                                }
                                            }
                                            else {
                                                sender.sendMessage(sPluginName);
                                                sender.sendMessage(sErrorColor+"Group ["+arg2+"] not found - case sentitive or player not visited yet");
                                            }
                                            return true;
                                    }
                                }
                            }

                            // No player found
                            if(!bGetOne) {
                                sender.sendMessage(sPluginName);
                                sender.sendMessage(sErrorColor+"Player ["+arg1+"] not found");
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
                                    if(bConsoleOrOP) {
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
                                    if(sender.isOp()) {
                                        // Add new group
                                        if(args.length > 2) {
                                            if(addNewGroup(args[2])) {
                                                sender.sendMessage(sPluginName);
                                                sender.sendMessage(sObjectColor+"Group {"+sErrorColor+args[2]+sObjectColor+"} added");
                                            }
                                        }
                                        else {
                                            sender.sendMessage(sPluginName);
                                            sender.sendMessage(sObjectColor+sStartLine+" add [GROUPNAME]");
                                        }
                                    }
                                    else {
                                        sender.sendMessage(sPluginName);
                                        sender.sendMessage(sErrorColor+"Need OP permission");
                                    }
                                    return true;

                                case "move":
                                    if(sender.isOp()) {
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
                                        sender.sendMessage(sPluginName);
                                        sender.sendMessage(sErrorColor+"Need OP permission");
                                    }
                                    return true;

                                case "delete":
                                    if(bConsoleOrOP) {
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
                                        sender.sendMessage(sPluginName);
                                        sender.sendMessage(sErrorColor+"Need OP permission");
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

        String sDate = ""+calendar.get(Calendar.YEAR)+sMonth+sDay+sHour+sMinute;
        return sDate;

    }
}

