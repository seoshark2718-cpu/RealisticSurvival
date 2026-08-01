package com.realmc.systems;

import com.realmc.RealMCPlugin;
import com.realmc.data.PlayerState;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 체온 시스템.
 * 바이옴 기온, 시간대, 비/눈, 수중여부, 근처 열원(불/용암), 갑옷 여부를 종합해
 * 플레이어 체온을 0~100 사이에서 변화시키고 그에 따른 디버프를 적용한다.
 */
public class TemperatureSystem {

    private final RealMCPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public TemperatureSystem(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalTicks = plugin.getConfig().getInt("temperature.tick-interval-seconds", 5) * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    tick(player);
                }
            }
        }.runTaskTimer(plugin, 20L, intervalTicks);
    }

    private void tick(Player player) {
        if (!plugin.getConfig().getBoolean("temperature.enabled", true)) return;

        PlayerState state = plugin.getPlayerData().get(player.getUniqueId());
        double delta = calculateDelta(player);
        state.addTemperature(delta);
        applyEffects(player, state);
        updateBossBar(player, state);
    }

    private double calculateDelta(Player player) {
        World world = player.getWorld();
        Biome biome = world.getBiome(player.getLocation());
        double delta = 0;

        // 바이옴 기반 기본 변화
        if (isColdBiome(biome)) {
            delta -= 3;
        } else if (isHotBiome(biome)) {
            delta += 3;
        } else {
            delta += 0.5; // 평온한 바이옴은 서서히 중립(50)으로 수렴
        }

        // 밤 시간대는 더 추움
        long time = world.getTime();
        boolean isNight = time > 13000 && time < 23000;
        if (isNight) delta -= 1;

        // 비/눈
        if (world.hasStorm()) {
            if (isColdBiome(biome)) delta -= 2; // 눈보라
            else delta -= 1; // 비 맞으면 체온 하락
        }

        // 수중
        if (player.isInWater()) {
            double penalty = plugin.getConfig().getInt("swim.cold-water-temp-penalty", 3);
            delta -= penalty;
        }

        // 높은 고도는 더 추움
        if (player.getLocation().getY() > 100) {
            delta -= 1;
        }

        // 근처 열원(불, 용암, 모닥불)
        if (isNearHeatSource(player)) {
            delta += 4;
        }

        // 갑옷 착용 시 보온 효과
        int armorPieces = 0;
        if (player.getInventory().getHelmet() != null) armorPieces++;
        if (player.getInventory().getChestplate() != null) armorPieces++;
        if (player.getInventory().getLeggings() != null) armorPieces++;
        if (player.getInventory().getBoots() != null) armorPieces++;
        delta += armorPieces * 0.3;

        return delta;
    }

    private boolean isNearHeatSource(Player player) {
        int radius = 3;
        var loc = player.getLocation();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Material type = loc.clone().add(x, y, z).getBlock().getType();
                    if (type == Material.CAMPFIRE || type == Material.FIRE
                            || type == Material.LAVA || type == Material.SOUL_FIRE
                            || type == Material.FURNACE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isColdBiome(Biome biome) {
        String name = biome.name();
        return name.contains("SNOW") || name.contains("ICE") || name.contains("FROZEN")
                || name.contains("TAIGA") || name.contains("COLD");
    }

    private boolean isHotBiome(Biome biome) {
        String name = biome.name();
        return name.contains("DESERT") || name.contains("BADLANDS") || name.contains("SAVANNA")
                || name.contains("NETHER");
    }

    private void applyEffects(Player player, PlayerState state) {
        double temp = state.getTemperature();
        double coldThreshold = plugin.getConfig().getInt("temperature.cold-threshold", 25);
        double hotThreshold = plugin.getConfig().getInt("temperature.hot-threshold", 75);
        double hypothermiaDamage = plugin.getConfig().getInt("temperature.hypothermia-damage", 25);

        if (temp <= hypothermiaDamage) {
            // 저체온 - 지속 피해 + 심한 둔화
            player.damage(1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 2, true, false));
            player.sendActionBar(org.bukkit.ChatColor.AQUA + "체온이 위험할 정도로 낮습니다! 저체온증!");
        } else if (temp <= coldThreshold) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 0, true, false));
        } else if (temp >= hotThreshold) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, true, false));
        }
    }

    private void updateBossBar(Player player, PlayerState state) {
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), u -> {
            BossBar b = plugin.getServer().createBossBar("체온", BarColor.BLUE, BarStyle.SEGMENTED_10);
            b.addPlayer(player);
            return b;
        });
        double progress = Math.max(0, Math.min(1, state.getTemperature() / 100.0));
        bar.setProgress(progress);
        bar.setColor(colorFor(state.getTemperature()));
        bar.setTitle("체온: " + Math.round(state.getTemperature()) + "/100");
    }

    private BarColor colorFor(double temp) {
        if (temp <= 25) return BarColor.BLUE;
        if (temp >= 75) return BarColor.RED;
        return BarColor.GREEN;
    }

    public void removeBar(UUID uuid) {
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) bar.removeAll();
    }
}
