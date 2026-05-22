package com.tomkeuper.bedwars.utils;

import com.saicone.rtag.util.SkullTexture;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.itemMeta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (name != null) {
            itemMeta.setDisplayName(name.replace("&", "§"));
        }
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
        if (lore != null) {
            itemMeta.setLore(Arrays.stream(lore).filter(java.util.Objects::nonNull).map(line -> line.replace("&", "§")).collect(Collectors.toList()));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (lore != null) {
            itemMeta.setLore(lore.stream().filter(java.util.Objects::nonNull).map(line -> line.replace("&", "§")).collect(Collectors.toList()));
        }
        return this;
    }

    public ItemBuilder setSkull(String owner) {
        ItemStack head = null;
        String[] skin = null;

        // Try SkinRestorer API directly for immediate skin
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SkinRestorer")) {
            try {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayerExact(owner);
                Optional<SkinProperty> skinProp = Optional.empty();

                if (p != null) {
                    // ดึงจาก UUID ก่อนถ้าผู้เล่นออนไลน์
                    skinProp = SkinsRestorerProvider.get().getPlayerStorage().getSkinForPlayer(p.getUniqueId(), p.getName());
                } else {
                    // ดึงจากชื่อถ้าออฟไลน์ (ใช้ reflection หรือ casting ถ้าจำเป็น แต่ในที่นี้เราใช้การดึงค่าจาก result โดยตรงถ้าเป็นไปได้)
                    // เพื่อความปลอดภัยและลดปัญหา compile-time เราจะพยายามดึงผ่าน API ปกติก่อน
                    Object result = SkinsRestorerProvider.get().getSkinStorage().findSkinData(owner).orElse(null);
                    if (result != null) {
                        java.lang.reflect.Method getProperty = result.getClass().getMethod("getProperty");
                        skinProp = Optional.ofNullable((SkinProperty) getProperty.invoke(result));
                    }
                }

                if (skinProp.isPresent()) {
                    skin = new String[]{skinProp.get().getValue()};
                }
            } catch (Exception ignored) {
                // Ignore fallback to normal skull
            }
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

    public ItemBuilder setSkull(org.bukkit.entity.Player player) {
        String[] skin = null;

        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("SkinRestorer")) {
            try {
                Optional<SkinProperty> skinProp = SkinsRestorerProvider.get().getPlayerStorage().getSkinForPlayer(player.getUniqueId(), player.getName());
                if (skinProp.isPresent()) {
                    skin = new String[]{skinProp.get().getValue()};
                }
            } catch (Exception ignored) {
            }
        }

        ItemMeta oldMeta = this.itemMeta;

        if (skin != null) {
            this.item = SkullTexture.setTexture(new ItemStack(Material.valueOf(com.tomkeuper.bedwars.BedWars.getForCurrentVersion("SKULL_ITEM", "PLAYER_HEAD", "PLAYER_HEAD"))), skin[0]);
        } else {
            this.item = SkullTexture.getTexturedHead(player.getName());
        }

        this.itemMeta = this.item.getItemMeta();

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