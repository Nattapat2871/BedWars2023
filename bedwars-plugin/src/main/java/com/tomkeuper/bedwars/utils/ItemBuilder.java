package com.tomkeuper.bedwars.utils;

import com.saicone.rtag.util.SkullTexture;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.itemMeta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        itemMeta.setDisplayName(name.replace("&", "§"));
        return this;
    }

    public ItemBuilder setGlow(boolean glow) {
        if (glow) {
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        itemMeta.setLore(Arrays.stream(lore).map(line -> line.replace("&", "§")).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        itemMeta.setLore(lore.stream().map(line -> line.replace("&", "§")).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder setSkull(String owner) {
        ItemStack head = null;
        String[] skin = null;
        
        // Try SkinsRestorer first for immediate skin
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            try {
                Object api = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider").getMethod("get").invoke(null);
                Object storage = api.getClass().getMethod("getPlayerStorage").invoke(api);
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(owner);
                java.util.Optional<?> skinProp;
                if (p != null) {
                    skinProp = (java.util.Optional<?>) storage.getClass().getMethod("getSkinOfPlayer", java.util.UUID.class).invoke(storage, p.getUniqueId());
                } else {
                    Object skinStorage = api.getClass().getMethod("getSkinStorage").invoke(api);
                    skinProp = (java.util.Optional<?>) skinStorage.getClass().getMethod("findSkinData", String.class).invoke(skinStorage, owner);
                }
                
                if (skinProp.isPresent()) {
                    Object prop = skinProp.get();
                    if (prop.getClass().getSimpleName().equals("InputDataResult")) {
                        prop = prop.getClass().getMethod("getProperty").invoke(prop);
                    }
                    String value = (String) prop.getClass().getMethod("getValue").invoke(prop);
                    skin = new String[]{value};
                }
            } catch (Exception ignored) {}
        }

        ItemMeta oldMeta = this.itemMeta;
        if (skin != null) {
             this.item = SkullTexture.setTexture(new ItemStack(Material.valueOf(com.tomkeuper.bedwars.BedWars.getForCurrentVersion("SKULL_ITEM", "PLAYER_HEAD", "PLAYER_HEAD"))), skin[0]);
        } else {
            this.item = SkullTexture.getTexturedHead(owner);
        }
        this.itemMeta = this.item.getItemMeta();
        
        // Preserve old meta
        if (oldMeta != null) {
            if (oldMeta.hasDisplayName()) this.itemMeta.setDisplayName(oldMeta.getDisplayName());
            if (oldMeta.hasLore()) this.itemMeta.setLore(oldMeta.getLore());
            for (org.bukkit.inventory.ItemFlag flag : oldMeta.getItemFlags()) this.itemMeta.addItemFlags(flag);
            for (java.util.Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet()) {
                this.itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        
        return this;
    }

    public ItemBuilder setMaterial(Material material) {
        item.setType(material);
        return this;
    }

    public ItemBuilder setDurability(int durability) {
        item.setDurability((short) durability);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setLeatherColor(Color color) {
        if (itemMeta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherMeta = (LeatherArmorMeta) itemMeta;
            leatherMeta.setColor(color);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(itemMeta);
        return item;
    }
}
