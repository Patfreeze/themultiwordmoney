package com.amedacier.themultiworldmoney;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class AuctionItem {
    private final OfflinePlayer playerOwner;
    private final ItemStack itemStack;
    private final double dPrice;
    private final String dtDate;
    private final String id;

    public AuctionItem(OfflinePlayer p, ItemStack is, double price, String date) {
        this.playerOwner = p;
        this.itemStack = is;
        this.dPrice = price;
        this.dtDate = date;
        this.id = p.getName()+is.getType()+price+date;
    }

    public String getId() { return id; }

    public double getPrice() {
        return dPrice;
    }

    public String getDate() {
        return dtDate;
    }

    public String getExpirationDate() {

        Calendar calendarCurrentItem = Calendar.getInstance();
        calendarCurrentItem.setTimeZone(TimeZone.getTimeZone(TheMultiWorldMoney.sTimezone));
        calendarCurrentItem.setTimeInMillis(Long.parseLong(this.getDate()));
        calendarCurrentItem.add(Calendar.DATE,TheMultiWorldMoney.expirationAuctionDay);

        Date date =  calendarCurrentItem.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date); //2013-10-15 16:16:39

    }

    public OfflinePlayer getPlayerOwner() {
        return playerOwner;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

}
