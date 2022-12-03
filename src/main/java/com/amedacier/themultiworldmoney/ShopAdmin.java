package com.amedacier.themultiworldmoney;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * TODO:
 *  - verification quand pas OP que la balance reflete que ce le player peut acheter
 *  - faire la partie vendre au shop player 2 shop
 *  - autre chose a faire... a voir...
 *
 */

public class ShopAdmin {

    private Player player;
    private File dataFolder;
    private ItemStack itemStack;
    private int quantity = 0;
    private double balance = 0;
    private double sellPrice = 1;
    private double buyPrice = 2;
    private boolean hasInfinity = false;
    private World world;
    private Location locationBarrel;

    public ShopAdmin(Player p, File df, Location lBarrel, boolean bLoadFromFile) {

        // If this 2 is null we load from file
        this.locationBarrel = lBarrel;
        this.world = lBarrel.getWorld();
        this.dataFolder = df;
        this.player = p;

        if(bLoadFromFile) {
            loadFromFile();
            // Create a dropItem not pickable and not despawn
        }
        else {
            itemStack = new ItemStack(Material.STICK, 1); // By default, we put a stick
        }

    }

    public boolean isSellPriceUpperThanBuying(double sell, double buy) {
        return sell > buy;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public boolean setBalance(double bal) {
        if(bal < 0) {
            return false;
        }
        this.balance = bal;
        this.saveOnFile();
        return true;
    }

    public double getBalance() {
        return balance;
    }

    public boolean setSellingPrice(double sellP) {
        if(isSellPriceUpperThanBuying(sellP, buyPrice)) {
            return false;
        }
        BigDecimal result = new BigDecimal(sellP).setScale(2, RoundingMode.HALF_UP);
        this.sellPrice = result.doubleValue();
        this.saveOnFile();
        return true;
    }

    public boolean setBuyingPrice(double buyP) {
        if(isSellPriceUpperThanBuying(sellPrice, buyP)) {
            return false;
        }
        BigDecimal result = new BigDecimal(buyP).setScale(2, RoundingMode.HALF_UP);
        this.buyPrice = result.doubleValue();
        this.saveOnFile();
        return true;
    }

    public void setItemStack(ItemStack is) {
        this.itemStack = is;
        this.saveOnFile();
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setQuantity(int qts) {
        this.quantity = qts;
        this.saveOnFile();
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setInfinity(boolean hasInfinity) {
        this.hasInfinity = hasInfinity;
        this.saveOnFile();
    }

    public boolean hasInfinity() {
        return this.hasInfinity;
    }

    public Location getLocation() {
        return locationBarrel;
    }

    private File getFile() {
        // CREATE PLAYER FILE
        FileConfiguration config = null;

        String sLocation = locationBarrel.getBlockX()+"_"+locationBarrel.getBlockY()+"_"+locationBarrel.getBlockZ();

        File file = new File(dataFolder+File.separator+"Shop", locationBarrel.getWorld().getName()+"_"+sLocation+".yml");

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

        // Before save we set itemStack to 1
        itemStack.setAmount(1);

        // All that is shop handle as var
        config.set("itemStack", itemStack);
        config.set("quantity", quantity);
        config.set("sellPrice", sellPrice);
        config.set("buyPrice", buyPrice);
        config.set("hasInfinity", hasInfinity);
        config.set("locationBarrel", locationBarrel);
        config.set("balance", balance);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateSign(ItemStack itemStack) {

        int X = locationBarrel.getBlockX();
        int Y = locationBarrel.getBlockY();
        int Z = locationBarrel.getBlockZ();

        Block block = world.getBlockAt(X, Y, Z);
        if (block.getType().name().contains("_SIGN")) {

            Sign sign = (Sign) block.getState();
            sign.setGlowingText(true);

            if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("[tmwm]")) {
                sign.setLine(0, "§6[TMWM]");
                sign.setLine(1, "§1SHOP");

                sign.setLine(2, "§3"+itemStack.getType().name());
                sign.setLine(3, "§2S:"+getSellPrice()+" §4B:"+getBuyPrice());

                sign.update();
            }

        }
    }

    /*
    private void createHologram(World world, String sMessage, Location location) {
        ArmorStand as = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND); //Spawn the ArmorStand

        as.setInvulnerable(false); // Can be destroyable
        as.setRemoveWhenFarAway(true); // Despawn when no player...Check it out!
        as.setGravity(false); //Make sure it doesn't fall
        as.setCanPickupItems(false); //I'm not sure what happens if you leave this as it is, but you might as well disable it
        as.setCustomName(sMessage); //Set this to the text you want
        as.setCustomNameVisible(true); //This makes the text appear no matter if you're looking at the entity or not
        as.setVisible(false); //Makes the ArmorStand invisible

        // Save coor to delete it?
    }
    */


    private Collection<Entity> getEntityShop(EntityType entType) {
        if(entType != null) {
            Predicate<Entity> filter = new Predicate<Entity>() {
                @Override
                public boolean test(Entity entity) {
                    System.out.println("entity.getType(): " + entity.getType());
                    System.out.println("Good type?: " + (entity.getType() == entType));
                    return entity.getType() == entType;
                }
            };
            return this.world.getNearbyEntities(this.locationBarrel.add(0, 0, 0), 2, 2, 2, filter);
        }
        return this.world.getNearbyEntities(this.locationBarrel.add(0, 0, 0), 2, 2, 2);
    }


    public void deleteShop() {

        // remove also the item
        //deleteDropItem();

        // Delete the file?
    }

    private void loadFromFile() {
        FileConfiguration config = null;
        File file = getFile();
        config = YamlConfiguration.loadConfiguration(file);

        this.itemStack = config.getItemStack("itemStack");
        this.quantity = config.getInt("quantity");
        this.sellPrice = config.getDouble("sellPrice");
        this.buyPrice = config.getDouble("buyPrice");
        this.hasInfinity = config.getBoolean("hasInfinity");
        this.locationBarrel = config.getLocation("locationBarrel");
        this.balance = config.getDouble("balance");

        this.world = this.locationBarrel.getWorld();
    }

}
