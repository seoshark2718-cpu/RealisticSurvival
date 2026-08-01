package com.realmc.systems;

import com.realmc.RealMCPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

/**
 * 랜덤 재난 시스템: 폭우, 지진, 해일, 폭설, 가뭄, 산불.
 * 강도(severity)는 매번 랜덤으로 결정된다.
 */
public class DisasterSystem {

    public enum DisasterType { HEAVY_RAIN, EARTHQUAKE, TSUNAMI, SNOWSTORM, DROUGHT, WILDFIRE }

    private final RealMCPlugin plugin;
    private final Random random = new Random();
    private DisasterType activeDisaster = null;

    public DisasterSystem(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalTicks = plugin.getConfig().getInt("disaster.check-interval-minutes", 20) * 60 * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("disaster.enabled", true)) return;
                double chance = plugin.getConfig().getDouble("disaster.chance-per-check", 0.25);
                if (activeDisaster == null && random.nextDouble() < chance) {
                    triggerRandomDisaster();
                }
            }
        }.runTaskTimer(plugin, 20L * 60, intervalTicks);
    }

    public void triggerRandomDisaster() {
        List<String> typeNames = plugin.getConfig().getStringList("disaster.types");
        if (typeNames.isEmpty()) return;
        String chosen = typeNames.get(random.nextInt(typeNames.size()));
        trigger(DisasterType.valueOf(chosen));
    }

    public void trigger(DisasterType type) {
        double min = plugin.getConfig().getDouble("disaster.min-severity", 0.5);
        double max = plugin.getConfig().getDouble("disaster.max-severity", 2.0);
        double severity = min + random.nextDouble() * (max - min);

        activeDisaster = type;
        announceForecast(type);

        // 전조 후 실제 발동까지 약간의 텀
        new BukkitRunnable() {
            @Override
            public void run() {
                runDisaster(type, severity);
            }
        }.runTaskLater(plugin, 200L); // 10초 후 본격 시작
    }

    private void announceForecast(DisasterType type) {
        String name = koreanName(type);
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[전조] " + ChatColor.GRAY
                + "하늘이 심상치 않습니다... 곧 " + name + "이(가) 닥칠 것 같습니다.");
    }

    private void runDisaster(DisasterType type, double severity) {
        Bukkit.broadcastMessage(ChatColor.RED + "[재난] " + ChatColor.WHITE
                + koreanName(type) + "이(가) 발생했습니다! (강도 " + String.format("%.1f", severity) + ")");

        int durationSeconds = (int) (60 * severity);

        switch (type) {
            case HEAVY_RAIN -> runHeavyRain(durationSeconds);
            case EARTHQUAKE -> runEarthquake(severity);
            case TSUNAMI -> runTsunami(severity);
            case SNOWSTORM -> runSnowstorm(durationSeconds);
            case DROUGHT -> runDrought(durationSeconds);
            case WILDFIRE -> runWildfire(severity);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                activeDisaster = null;
                Bukkit.broadcastMessage(ChatColor.GREEN + "[재난 종료] " + koreanName(type) + "이(가) 지나갔습니다.");
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }

    private void runHeavyRain(int durationSeconds) {
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(true);
            world.setThundering(true);
            world.setWeatherDuration(durationSeconds * 20);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));
        }
    }

    private void runEarthquake(double severity) {
        int shakes = (int) (10 * severity);
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= shakes) {
                    cancel();
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    // 화면 흔들림 흉내: 짧은 넉백성 velocity + 멀미 효과
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 30, 0, true, false));
                    p.setVelocity(new Vector(
                            (random.nextDouble() - 0.5) * 0.2,
                            0.05,
                            (random.nextDouble() - 0.5) * 0.2));

                    // 근처 블록 낙하 연출 (파쿠르용 모래/자갈만 낙하 처리)
                    Block base = p.getLocation().getBlock();
                    if (random.nextDouble() < 0.2) {
                        Block target = base.getRelative(
                                random.nextInt(5) - 2, 3, random.nextInt(5) - 2);
                        if (target.getType() == Material.GRAVEL || target.getType() == Material.SAND) {
                            target.getWorld().spawnFallingBlock(target.getLocation(), target.getBlockData());
                        }
                    }
                }
                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void runTsunami(double severity) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isNearOcean(p)) {
                Vector push = p.getLocation().getDirection().multiply(-1).setY(0.3 * severity);
                p.setVelocity(push);
                p.sendMessage(ChatColor.AQUA + "거대한 파도가 밀려옵니다!");
            }
        }
    }

    private boolean isNearOcean(Player p) {
        String biome = p.getWorld().getBiome(p.getLocation()).name();
        return biome.contains("OCEAN") || biome.contains("BEACH") || biome.contains("RIVER");
    }

    private void runSnowstorm(int durationSeconds) {
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(true);
            world.setWeatherDuration(durationSeconds * 20);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, durationSeconds * 20, 1, true, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, true, false));
        }
    }

    private void runDrought(int durationSeconds) {
        // 가뭄: 작물 성장 정지를 흉내내기 위해 근처 농작물 랜덤으로 성장단계 되돌리지 않고,
        // 단순히 플레이어에게 배고픔 소모 증가 디버프로 대체 (블록 대량 순회는 성능상 지양)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatColor.GOLD + "극심한 가뭄으로 작물이 잘 자라지 않고, 갈증이 심해집니다.");
            p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, durationSeconds * 20, 0, true, false));
        }
    }

    private void runWildfire(double severity) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String biome = p.getWorld().getBiome(p.getLocation()).name();
            if (biome.contains("FOREST") || biome.contains("TAIGA") || biome.contains("JUNGLE")) {
                if (random.nextDouble() < 0.3 * severity) {
                    Block target = p.getLocation().getBlock().getRelative(
                            random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
                    if (target.getType() == Material.AIR &&
                            target.getRelative(0, -1, 0).getType().isSolid()) {
                        target.setType(Material.FIRE);
                    }
                    p.sendMessage(ChatColor.RED + "산불이 번지고 있습니다! 대피하세요!");
                }
            }
        }
    }

    private String koreanName(DisasterType type) {
        return switch (type) {
            case HEAVY_RAIN -> "폭우";
            case EARTHQUAKE -> "지진";
            case TSUNAMI -> "해일";
            case SNOWSTORM -> "폭설";
            case DROUGHT -> "가뭄";
            case WILDFIRE -> "산불";
        };
    }

    public boolean isDisasterActive() {
        return activeDisaster != null;
    }
}
