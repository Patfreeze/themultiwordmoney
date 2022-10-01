package com.amedacier.themultiworldmoney;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;

public class ShopAdmin {

    private Player player;
    private File dataFolder;
    private ItemStack itemStack;
    private int quantity = 0;
    private double amount = 0;
    private boolean hasInfinity = true;

    public ShopAdmin(Player p, File df) {
        this.player = p;
        this.dataFolder = df;
        itemStack = new ItemStack(Material.STICK); // By default we put a stick
    }

    public void setItemStack(ItemStack is) {
        this.itemStack = is;
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setQuantity(int qts) {
        this.quantity = qts;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public ShopAdmin setInfinity(boolean hasInfinity) {
        this.hasInfinity = hasInfinity;
        return this;
    }

    public boolean hasInfinity() {
        return this.hasInfinity;
    }

    public void saveOnFile() {
        // TODO: Save data on file
    }
}
