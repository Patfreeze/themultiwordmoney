package com.amedacier.themultiworldmoney;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import java.util.ArrayList;

public class GuiCMD {

    Player player;
    String sType;
    Inventory inventoryBoxShop;
    ItemStack itemStack;

    public GuiCMD(Player player, String sType) {
        this.player = player;
        this.sType = sType;
        inventoryBoxShop = Bukkit.getServer().createInventory(player, this.getSize(), TheMultiWorldMoney.sPluginNameNoColor+":"+sType);
    }

    private int getSize() {
        // Multiple of 9 so : 9, 18, 27,  36, 45, max 54
        switch(this.sType) {
            case "adminShop":
                return 18;

            default:
               return 9;
        }

    }

    private void placeVoidItems() {
        ArrayList<String> lore = new ArrayList<String>();
        for(int i=0; i<this.getSize(); i++) {
            placeItem(i, Material.BLACK_STAINED_GLASS_PANE, "!!!", lore, 1, false);
        }
    }

    private void placeItem(int position, ItemStack itemStack, String displayName, ArrayList<String> lore) {

        ItemMeta metaref1 = itemStack.getItemMeta();

        // Set Lore to Item
        metaref1.setLore(lore);
        metaref1.setDisplayName(displayName);

        itemStack.setItemMeta(metaref1);
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

    private void getAdminShop() {

        int iCount=0;
        ArrayList<String> lore;
        HashMap<ItemStack, String> a_sItemPrice = new HashMap<>(); // TODO: Ce n'est plus qu'un objet
        for(ItemStack itemStack : a_sItemPrice.keySet()) {

            ItemStack itemStackShop = itemStack.clone();

            String[] a_sAmount = a_sItemPrice.get(itemStack).split(":");
            itemStackShop.setAmount(1);

            lore = new ArrayList<>();
            lore.add("Right click: §6Edit");

            placeItem(iCount, itemStackShop, itemStackShop.getType().name()+":shop", lore);
            iCount++;
        }

        if(a_sItemPrice.size() == 0) {
            ItemStack itemStackShop = new ItemStack(Material.STRUCTURE_VOID);
            lore = new ArrayList<>();
            lore.add("§4Shop empty");
            placeItem(iCount, itemStackShop, "Empty:shop", lore);
        }

    }

    public void render() {
        if(player != null) {
            // First we place all void items
            placeVoidItems();

            switch(sType) {
                case "adminShop":
                    getAdminShop();
                    break;
            }

            //Here opens the inventory
            player.openInventory(inventoryBoxShop);
        }
    }

}