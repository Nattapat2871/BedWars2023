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
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.itemMeta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (name == null) return this;
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
        if (lore == null) return this;
        itemMeta.setLore(Arrays.stream(lore).filter(Objects::nonNull).map(line -> line.replace("&", "§")).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (lore == null) return this;
        itemMeta.setLore(lore.stream().filter(Objects::nonNull).map(line -> line.replace("&", "§")).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder setSkull(org.bukkit.entity.Player player) {
        return setSkullInternal(player.getName(), player);
    }

    public ItemBuilder setSkull(String owner) {
        return setSkullInternal(owner, org.bukkit.Bukkit.getPlayerExact(owner));
    }

    private ItemBuilder setSkullInternal(String owner, org.bukkit.entity.Player p) {
        ItemStack head = null;
        String[] skin = null;
        
        // 1. ลองดึงสกินโดยตรงจาก Profile ของผู้เล่นผ่าน Reflection (รองรับทั้งไอดีแท้และไอดีเถื่อนที่ผ่าน Bungee/Velocity)
        if (p != null) {
            try {
                // ดึงข้อมูลผ่าน Profile API แบบ Reflection เพื่อไม่ให้ติดปัญหาตอน Compile กับ Spigot 1.8.8
                Object profile = p.getClass().getMethod("getPlayerProfile").invoke(p);
                java.util.Collection<?> properties = (java.util.Collection<?>) profile.getClass().getMethod("getProperties").invoke(profile);
                for (Object prop : properties) {
                    String propName = (String) prop.getClass().getMethod("getName").invoke(prop);
                    if (propName.equalsIgnoreCase("textures")) {
                        String propValue = (String) prop.getClass().getMethod("getValue").invoke(prop);
                        skin = new String[]{propValue};
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. ถ้ายังไม่ได้สกิน ให้ลองใช้ SkinsRestorer API (กรณีลงไว้ในเครื่อง Bedwar)
        if (skin == null) {
            Class<?> providerClass = null;
            Object apiInstance = null;
            boolean isV15 = false;
            
            try {
                // ลองดึงคลาสเวอร์ชันใหม่ v15+ (net.skinrestorer)
                providerClass = Class.forName("net.skinrestorer.api.SkinRestorerProvider");
                apiInstance = providerClass.getMethod("get").invoke(null);
                isV15 = true;
            } catch (Exception e) {
                try {
                    // โหลดโครงสร้างเก่า v14 ลงไปแบบ Fallback (net.skinsrestorer)
                    providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                    apiInstance = providerClass.getMethod("get").invoke(null);
                } catch (Exception ignored) {}
            }

            if (apiInstance != null) {
                try {
                    java.util.Optional<?> skinProp = java.util.Optional.empty();

                    if (isV15) {
                        // ใช้ลอจิกของ SkinsRestorer v15+ API
                        Object playerStorage = apiInstance.getClass().getMethod("getPlayerStorage").invoke(apiInstance);
                        if (p != null) {
                            try {
                                // ดึงผ่าน getSkinForPlayer(UUID, String) เพื่อความแม่นยำในเซิร์ฟเวอร์ Hybrid
                                skinProp = (java.util.Optional<?>) playerStorage.getClass()
                                    .getMethod("getSkinForPlayer", java.util.UUID.class, String.class)
                                    .invoke(playerStorage, p.getUniqueId(), p.getName());
                            } catch (NoSuchMethodException e) {
                                skinProp = (java.util.Optional<?>) playerStorage.getClass()
                                    .getMethod("getSkinOfPlayer", java.util.UUID.class)
                                    .invoke(playerStorage, p.getUniqueId());
                            }
                        } else {
                            Object skinStorage = apiInstance.getClass().getMethod("getSkinStorage").invoke(apiInstance);
                            skinProp = (java.util.Optional<?>) skinStorage.getClass()
                                .getMethod("findSkinData", String.class)
                                .invoke(skinStorage, owner);
                        }

                        if (skinProp.isPresent()) {
                            Object prop = skinProp.get();
                            String value = (String) prop.getClass().getMethod("getValue").invoke(prop);
                            skin = new String[]{value};
                        }
                    } else {
                        // ใช้ลอจิกเวอร์ชันเก่า v14 ลงไป
                        Object storage = apiInstance.getClass().getMethod("getPlayerStorage").invoke(apiInstance);
                        if (p != null) {
                            skinProp = (java.util.Optional<?>) storage.getClass().getMethod("getSkinOfPlayer", java.util.UUID.class).invoke(storage, p.getUniqueId());
                        } else {
                            Object skinStorage = apiInstance.getClass().getMethod("getSkinStorage").invoke(apiInstance);
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
                    }
                } catch (Exception ignored) {}
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