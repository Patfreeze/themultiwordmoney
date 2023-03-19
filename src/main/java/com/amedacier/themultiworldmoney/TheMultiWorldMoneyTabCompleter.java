package com.amedacier.themultiworldmoney;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Bukkit;
//import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class TheMultiWorldMoneyTabCompleter implements TabCompleter {

    String[] a_sLevel_1 = {"help","group","baltop", "pay", "create_shop"}; // if you add stuff here, also add in permission
    String[] a_sLevel_1_OP = {"player"}; // "show_armor_stand"

    File dataFolder;

    //Material[] a_sMaterial = Material.values();

    public TheMultiWorldMoneyTabCompleter(File dataFolder) {
        this.dataFolder = dataFolder;
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
            case "help":
            case "group":
            case "baltop":
                return true;
            case "killedplayers": // OP ADMIN MOD killedplayers.use
                return bUsekilledPlayers;
            case "pay": // OP ADMIN MOD PAY
                return bUsePay;
            case "mod": // OP ADMIN MOD
                return bMod;
            case "admin": // OP ADMIN
                return bAdmin;
            default: // by default is a permission check
                return p.hasPermission(sType);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        List<String> list = new ArrayList<>();

        boolean bAdmin = havePermission(sender, "admin") || havePermission(sender, "console");

        // Allez chercher la liste des groupes
        File dataFilef = new File(dataFolder, "data.yml");
        FileConfiguration dataFile = new YamlConfiguration();
        try {
            dataFile.load(dataFilef);
        } catch (IOException | InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<String> a_groupList = new ArrayList<>();


        switch(cmd.getName().toLowerCase()) {

            case "auction":
            case "ah":
            case "ach":
            case "ac":
            case "hdv":
                a_groupList = new ArrayList<>();
                a_groupList.add("display");
                a_groupList.add("sell");

                switch(args.length) {
                    case 1: //
                        for(String sGroupOption : a_groupList) {
                            if(args[0].equalsIgnoreCase("") || sGroupOption.startsWith(args[0]) || sGroupOption.toLowerCase().startsWith(args[0])) {
                                list.add(sGroupOption);
                            }
                        }
                        return list;

                    case 2:
                        // amount
                        if(args[0].equalsIgnoreCase("sell")) {
                            list.add("AMOUNT");
                            return list;
                        }
                        else {
                            return null;
                        }

                    case 3:
                        // amount
                        if(args[0].equalsIgnoreCase("sell")) {
                            list.add("QTS");
                            return list;
                        }
                        else {
                            return null;
                        }
                }
                return null;

            case "shop":
                return null;

            case "killedplayers":
                a_groupList = new ArrayList<>();
                switch(args.length) {
                    case 1:
                        // list of group of world
                        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                            a_groupList.add(sGroup);
                        }

                        for(String sGroupOption : a_groupList) {
                            if(args[0].equalsIgnoreCase("") || sGroupOption.startsWith(args[0]) || sGroupOption.toLowerCase().startsWith(args[0])) {
                                list.add(sGroupOption);
                            }
                        }

                        return list;
                }
                return null;

            case "payto":

                switch(args.length) {
                    case 1:
                        // list of player
                        if(Bukkit.getServer() == null) {
                            return null;
                        }

                        for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                            if(args[0].equalsIgnoreCase("") || player.getName().startsWith(args[0]) || player.getName().toLowerCase().startsWith(args[0])) {
                                list.add(player.getName());
                            }
                        }
                        return list;

                    case 2:
                        // amount
                        list.add("1");
                        list.add("10");
                        list.add("100");
                        return list;
                }


                return null;

            case "themultiworldmoney":
            case "tmwm":
            case "themoney":
                // TheMultiWorldMoney module so check what we need
                a_groupList = new ArrayList<>();

                switch(args.length) {
                    ////////////////////////////////////////////
                    // We are on a LEVEL 1 check on what
                    /////////////////////////////////////////////
                    case 1: // ARGS[0] HELP GROUP PLAYER

                        if(bAdmin) {
                            for(String sLevel : a_sLevel_1_OP) {
                                if(args[0].equalsIgnoreCase("") || sLevel.startsWith(args[0]) || sLevel.toLowerCase().startsWith(args[0])) {
                                    list.add(sLevel);
                                }
                            }
                        }

                        for(String sLevel : a_sLevel_1) {
                            if(havePermission((Player) sender, sLevel)) {
                                if (args[0].equalsIgnoreCase("") || sLevel.startsWith(args[0]) || sLevel.toLowerCase().startsWith(args[0])) {
                                    list.add(sLevel);
                                }
                            }
                        }
                        return list;

                    ////////////////////////////////////////////
                    // We are on a LEVEL 2 check on what
                    /////////////////////////////////////////////
                    case 2:
                        switch(args[0].toLowerCase()) {

                            case "pay": // second list
                                if (Bukkit.getServer() == null) {
                                    return null;
                                }

                                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                                    if (args[1].equalsIgnoreCase("") || player.getName().startsWith(args[1]) || player.getName().toLowerCase().startsWith(args[1])) {
                                        list.add(player.getName());
                                    }
                                }
                                return list;

                            case "player": // second list

                                if (Bukkit.getServer() == null) {
                                    return null;
                                }

                                if(bAdmin) {
                                    for(OfflinePlayer player : Bukkit.getServer().getOfflinePlayers()) {
                                        String playerName = null;
                                        if(player != null) {
                                            playerName = player.getName();
                                            if(playerName == null && player.getPlayer() != null) {
                                                playerName = player.getPlayer().getName();
                                            }

                                            if(playerName != null && (args[1].equalsIgnoreCase("") || playerName.startsWith(args[1]) || playerName.toLowerCase().startsWith(args[1]))) {
                                                list.add(playerName);
                                            }
                                        }
                                    }
                                }
                                if(list.isEmpty()) {
                                    return null;
                                }
                                return list;

                            case "group": // second list

                                List<String> a_sGroupOption = new ArrayList<String>();
                                a_sGroupOption.add("help");
                                a_sGroupOption.add("list");

                                if(bAdmin) {
                                    a_sGroupOption.add("add");
                                    a_sGroupOption.add("delete");
                                    a_sGroupOption.add("move");
                                    a_sGroupOption.add("setamount");
                                }

                                for(String sGroupOption : a_sGroupOption) {
                                    if(args[1].equalsIgnoreCase("") || sGroupOption.startsWith(args[1]) || sGroupOption.toLowerCase().startsWith(args[1])) {
                                        list.add(sGroupOption);
                                    }
                                }
                                return list;

                            case "baltop": // second list

                                for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                                    a_groupList.add(sGroup);
                                }

                                for(String sGroupOption : a_groupList) {
                                    if(args[1].equalsIgnoreCase("") || sGroupOption.startsWith(args[1]) || sGroupOption.toLowerCase().startsWith(args[1])) {
                                        list.add(sGroupOption);
                                    }
                                }

                                return list;

                            default:
                                return null;
                        }

                        ////////////////////////////////////////////
                        // We are on a LEVEL 3 check on what
                        /////////////////////////////////////////////
                    case 3:
                        switch(args[0].toLowerCase()) {

                            case "pay": // amount to player
                                a_groupList.clear();

                                a_groupList.add("1");
                                a_groupList.add("10");
                                a_groupList.add("100");

                                return a_groupList;

                            case "player": // PARAM 1 FOR 3

                                if(bAdmin) {
                                    a_groupList.add("list");

                                    for (String sGroup : dataFile.getConfigurationSection("group").getKeys(false)) {
                                        a_groupList.add(sGroup);
                                    }

                                    for (String sGroupOption : a_groupList) {
                                        if (args[2].equalsIgnoreCase("") || sGroupOption.startsWith(args[2]) || sGroupOption.toLowerCase().startsWith(args[2])) {
                                            list.add(sGroupOption);
                                        }
                                    }
                                    return list;
                                }

                                return null;

                            case "group":

                                switch(args[1].toLowerCase()) {
                                    case "delete":
                                    case "setamount":

                                        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                                            a_groupList.add(sGroup);
                                        }

                                        for(String sGroupOption : a_groupList) {
                                            if(args[2].equalsIgnoreCase("") || sGroupOption.startsWith(args[2]) || sGroupOption.toLowerCase().startsWith(args[2])) {
                                                list.add(sGroupOption);
                                            }
                                        }
                                        break;

                                    case "move":
                                        // list of world
                                        for(World world : Bukkit.getServer().getWorlds()){
                                            String sWorldName = world.getName();
                                            if(args[2].equalsIgnoreCase("") || sWorldName.toLowerCase().startsWith(args[2].toLowerCase()) || sWorldName.toLowerCase().startsWith(args[2])) {
                                                list.add(sWorldName);
                                            }
                                        }
                                        break;

                                    case "add":
                                        // list of world
                                        a_groupList.add("Creative");
                                        a_groupList.add("Prison");
                                        a_groupList.add("ForFun");
                                        for(String lister : a_groupList) {
                                            if (args[2].equalsIgnoreCase("")) { //  || lister.toLowerCase().startsWith(args[2].toLowerCase()) || lister.toLowerCase().startsWith(args[2])
                                                list.add(lister);
                                            }
                                        }
                                        break;

                                    default:
                                        // ignore
                                        break;
                                }

                                return list;

                            default:
                                return null;
                        }

                    case 4:
                        switch(args[0].toLowerCase()) {

                            case "player": // PARAM 1 FOR 4

                                if(bAdmin) {
                                    String[] a_sGroupOptions = {"Deposit", "Withdraw", "Set", "Set_Auction_limit"};
                                    for (String sGroupOption : a_sGroupOptions) {
                                        if (args[3].equalsIgnoreCase("") || sGroupOption.startsWith(args[3]) || sGroupOption.toLowerCase().startsWith(args[3])) {
                                            list.add(sGroupOption);
                                        }
                                    }
                                }
                                return list;

                            case "group":

                                switch(args[1].toLowerCase()) {

                                    case "move":
                                        // list of group
                                        for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                                            a_groupList.add(sGroup);
                                        }

                                        for(String sGroupOption : a_groupList) {
                                            if(args[3].equalsIgnoreCase("") || sGroupOption.startsWith(args[3]) || sGroupOption.toLowerCase().startsWith(args[3])) {
                                                list.add(sGroupOption);
                                            }
                                        }
                                        break;

                                    case "add":
                                        // list of world
                                        list.add("0.00");
                                        list.add("10.00");
                                        list.add("100.00");
                                        break;

                                    default:
                                        // ignore
                                        break;
                                }

                                return list;

                            default:
                                return null;
                        }

                    case 5:
                        switch(args[0].toLowerCase()) {

                            case "player": // PARAM 1 FOR 5

                                if(bAdmin) {
                                    String[] a_sGroupOptions = {"AMOUNT"};
                                    for (String sGroupOption : a_sGroupOptions) {
                                        if (args[4].equalsIgnoreCase("") || sGroupOption.startsWith(args[4]) || sGroupOption.toLowerCase().startsWith(args[4])) {
                                            list.add(sGroupOption);
                                        }
                                    }
                                }
                                return list;


                            default:
                                return null;
                        }
                        //return null;

                    default:
                        return null;
                }

            default: // Not a command from MultiWorldMoney so ignore it
                return null;

        }
    }
}
