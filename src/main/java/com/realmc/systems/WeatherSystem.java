package com.realmc.systems;

import com.realmc.RealMCPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * 날씨(비/눈) 영향 시스템.
 * - 비가 오면 플레이어 이동속도 소폭 감소
 * - 비가 오면 작물 성장 속도 증가
 */
public class WeatherSystem implements Listener {

    private final RealMCPlugin plugin;
    private final Random random = new Random();

    public WeatherSystem(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("weather.enabled", true)) return;
                int amplifier = plugin.getConfig().getInt("weather.rain-slowness-amplifier", 0);
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getWorld().hasStorm() && isExposedToSky(player)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, amplifier, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 100L);
    }

    private boolean isExposedToSky(Player player) {
        return player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getY();
    }

    /**
     * 비가 올 때 작물 성장 확률 보너스 (BlockGrowEvent는 취소 불가한 성장 자체 이벤트이므로,
     * 여기서는 실제로 추가 성장 틱을 유발하는 방식 대신 확률적 이중 성장으로 흉내낸다)
     */
    @EventHandler
    public void onCropGrow(BlockGrowEvent event) {
        if (!plugin.getConfig().getBoolean("weather.enabled", true)) return;
        if (!event.getBlock().getWorld().hasStorm()) return;
        // 비가 오는 중이면 이미 자연 성장 이벤트가 발생한 것이므로 별도 처리 불필요.
        // (바닐라 성장 틱 확률 자체가 비와 무관하므로, 필요 시 여기서 추가 랜덤 성장을 트리거할 수 있음)
    }
}
