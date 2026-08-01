package com.realmc.systems;

import com.realmc.RealMCPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 인벤토리 무게 시스템.
 * 아이템 종류별로 무게를 매겨 총합을 계산하고,
 * 최대 허용치를 넘으면 이동속도 감소 디버프를 준다.
 */
public class WeightSystem {

    private final RealMCPlugin plugin;
    private static final double MAX_WEIGHT = 100.0; // 인벤토리가 전부 "무거운 아이템"일 때 기준치

    public WeightSystem(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalTicks = plugin.getConfig().getInt("weight.tick-interval-seconds", 2) * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("weight.enabled", true)) return;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    tick(player);
                }
            }
        }.runTaskTimer(plugin, 20L, intervalTicks);
    }

    private void tick(Player player) {
        double totalWeight = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            totalWeight += weightOf(item.getType()) * item.getAmount();
        }

        double percent = Math.min(100, (totalWeight / MAX_WEIGHT) * 100);
        double threshold = plugin.getConfig().getInt("weight.slow-threshold", 70);

        if (percent >= threshold) {
            int amplifier = percent >= 90 ? 2 : (percent >= 80 ? 1 : 0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, amplifier, true, false));
        }
    }

    /**
     * 아이템 종류별 무게 가중치. 갑옷/도구/블록류는 무겁고, 잡화는 가볍게 설정.
     */
    private double weightOf(Material material) {
        String name = material.name();
        if (name.endsWith("_BLOCK") || material.isBlock() && material.name().contains("ORE")) return 4.0;
        if (name.contains("ANVIL")) return 15.0;
        if (name.contains("HELMET") || name.contains("CHESTPLATE")
                || name.contains("LEGGINGS") || name.contains("BOOTS")) {
            if (name.contains("NETHERITE")) return 6.0;
            if (name.contains("DIAMOND") || name.contains("IRON")) return 4.5;
            return 2.5;
        }
        if (name.contains("SWORD") || name.contains("AXE") || name.contains("PICKAXE")
                || name.contains("SHOVEL") || name.contains("HOE")) return 2.0;
        if (material.isBlock()) return 1.5;
        return 0.5; // 음식, 잡화 등
    }
}
