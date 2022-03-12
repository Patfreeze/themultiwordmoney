package com.amedacier.themultiworldmoney;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
//import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class TheMultiWorldMoneyTabCompleter implements TabCompleter {

    String[] a_sLevel_1 = {"help","group","baltop", "pay","killedPlayer"};
    String[] a_sLevel_1_OP = {"player"};

    File dataFolder;

    //Material[] a_sMaterial = Material.values();

    public TheMultiWorldMoneyTabCompleter(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        List<String> list = new ArrayList<>();

        boolean bOP = sender.isOp();

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
                        for(Player player : Bukkit.getOnlinePlayers()) {
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
                // TheMultiWorldMoney module so check what we need
                a_groupList = new ArrayList<>();

                switch(args.length) {
                    ////////////////////////////////////////////
                    // We are on a LEVEL 1 check on what
                    /////////////////////////////////////////////
                    case 1: // ARGS[0] HELP GROUP PLAYER

                        if(bOP) {
                            for(String sLevel : a_sLevel_1_OP) {
                                if(args[0].equalsIgnoreCase("") || sLevel.startsWith(args[0]) || sLevel.toLowerCase().startsWith(args[0])) {
                                    list.add(sLevel);
                                }
                            }
                        }

                        for(String sLevel : a_sLevel_1) {
                            if(args[0].equalsIgnoreCase("") || sLevel.startsWith(args[0]) || sLevel.toLowerCase().startsWith(args[0])) {
                                list.add(sLevel);
                            }
                        }
                        return list;

                    ////////////////////////////////////////////
                    // We are on a LEVEL 2 check on what
                    /////////////////////////////////////////////
                    case 2:
                        switch(args[0].toLowerCase()) {

                            case "pay": // second list
                                for(Player player : Bukkit.getOnlinePlayers()) {
                                    if(args[1].equalsIgnoreCase("") || player.getName().startsWith(args[1]) || player.getName().toLowerCase().startsWith(args[1])) {
                                        list.add(player.getName());
                                    }
                                }
                                return list;

                            case "player": // second list
                                for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                                    if(args[1].equalsIgnoreCase("") || player.getName().startsWith(args[1]) || player.getName().toLowerCase().startsWith(args[1])) {
                                        list.add(player.getName());
                                    }
                                }
                                return list;

                            case "group": // second list

                                String[] a_sGroupOption = {"add","delete","move","list"};

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

                                a_groupList.add("1");
                                a_groupList.add("10");
                                a_groupList.add("100");

                                return list;

                            case "player": // PARAM 1 FOR 3

                                a_groupList.add("list");

                                for(String sGroup : dataFile.getConfigurationSection("group").getKeys(false)){
                                    a_groupList.add(sGroup);
                                }

                                for(String sGroupOption : a_groupList) {
                                    if(args[2].equalsIgnoreCase("") || sGroupOption.startsWith(args[2]) || sGroupOption.toLowerCase().startsWith(args[2])) {
                                        list.add(sGroupOption);
                                    }
                                }

                                return list;

                            case "group":

                                switch(args[1].toLowerCase()) {
                                    case "delete":

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

                                String[] a_sGroupOptions = {"Deposit","Withdraw","Set"};
                                for(String sGroupOption : a_sGroupOptions) {
                                    if(args[2].equalsIgnoreCase("") || sGroupOption.startsWith(args[3]) || sGroupOption.toLowerCase().startsWith(args[3])) {
                                        list.add(sGroupOption);
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

                                    default:
                                        // ignore
                                        break;
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
