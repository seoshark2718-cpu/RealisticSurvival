package com.realmc.systems;

import com.realmc.RealMCPlugin;
import com.realmc.data.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 오래 수영하면 피로(채굴피로)가 쌓이는 시스템.
 * 체온에 대한 냉수 패널티는 TemperatureSystem에서 처리한다.
 */
public class SwimSystem {

    private final RealMCPlugin plugin;

    public SwimSystem(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("swim.enabled", true)) return;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    tick(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void tick(Player player) {
        PlayerState state = plugin.getPlayerData().get(player.getUniqueId());

        if (player.isInWater()) {
            state.startSwimming();
            long fatigueAfter = plugin.getConfig().getInt("swim.fatigue-after-seconds", 45);
            if (state.getSwimDurationSeconds() >= fatigueAfter) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 0, true, false));
            }
        } else {
            state.stopSwimming();
        }
    }
}
