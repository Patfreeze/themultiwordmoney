package com.amedacier.themultiworldmoney;

import org.bukkit.entity.Player;

public class HandleMessage {

    private Player player;
    private String sType;
    private String inputPlayer = "";
    private ShopAdmin shopAdmin;

    public HandleMessage(Player p, String type, String inputP, ShopAdmin shopAd) {
        this.player = p;
        this.sType = type;
        this.inputPlayer = inputP;
        this.shopAdmin = shopAd;
    }

    public boolean setInputPlayer(String inputPlayer) {
        this.inputPlayer = inputPlayer;

        double amount = 0.0;

        switch(sType) {
            case "buying_price":

                try {
                    amount = Float.parseFloat(this.inputPlayer);
                    if(!shopAdmin.setBuyingPrice(amount)) {
                        player.sendMessage("§c-----| sell > buy |-----");
                        return false;
                    }
                }
                catch(NumberFormatException e) {
                    player.sendMessage("§c-----| Not a number... |-----");
                    return false;
                }
                break;

            case "selling_price":
                try {
                    amount = Float.parseFloat(this.inputPlayer);
                    if(!shopAdmin.setSellingPrice(amount)) {
                        player.sendMessage("§c-----| sell > buy |-----");
                        return false;
                    }
                }
                catch(NumberFormatException e) {
                    player.sendMessage("§c-----| Not a number... |-----");
                    return false;
                }
                break;

            default:
                System.out.println(sType+" is not implemented in class HandleMessage");
                return false;
        }
        return true;

    }

    public String getInputPlayer() {
        return inputPlayer;
    }

    public Player getPlayer() {
        return player;
    }

    public String getsType() {
        return sType;
    }

    public ShopAdmin getShopAdmin() {
        return shopAdmin;
    }
}
