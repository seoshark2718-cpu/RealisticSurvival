package com.realmc.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.List;

/**
 * 붕대 / 약초 커스텀 아이템 정의.
 * 리소스팩 없이도 바로 작동하도록 기존 텍스처(양털, 고사리)를 재활용하고
 * 커스텀 이름 + PDC 태그로 구분한다.
 * 리소스팩을 추가하면 이 클래스의 Material만 커스텀 모델로 교체하면 된다.
 */
public class CustomItems {

    public static NamespacedKey BANDAGE_KEY;
    public static NamespacedKey HERB_KEY;

    public static void init(NamespacedKey bandageKey, NamespacedKey herbKey) {
        BANDAGE_KEY = bandageKey;
        HERB_KEY = herbKey;
    }

    public static ItemStack createBandage() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "붕대");
        meta.setLore(List.of(
                ChatColor.GRAY + "출혈을 멈추고 체력을 조금 회복시킵니다.",
                ChatColor.GRAY + "(임시 지혈용 - 감염은 치료하지 못함)"
        ));
        meta.getPersistentDataContainer().set(BANDAGE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createHerb() {
        ItemStack item = new ItemStack(Material.FERN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "약초");
        meta.setLore(List.of(
                ChatColor.GRAY + "감염과 출혈을 완전히 치료합니다.",
                ChatColor.GRAY + "(완치까지 약간의 시간이 걸립니다)"
        ));
        meta.getPersistentDataContainer().set(HERB_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBandage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BANDAGE_KEY, PersistentDataType.BYTE);
    }

    public static boolean isHerb(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(HERB_KEY, PersistentDataType.BYTE);
    }
}
