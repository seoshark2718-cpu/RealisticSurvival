package com.realmc.systems;

import com.realmc.RealMCPlugin;
import com.realmc.data.PlayerState;
import com.realmc.items.CustomItems;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * 부상(출혈) / 감염 / 붕대 / 약초 치료 시스템.
 */
public class InjurySystem implements Listener {

    private final RealMCPlugin plugin;
    private final Random random = new Random();

    public InjurySystem(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // 출혈/감염 틱 처리
        int intervalTicks = plugin.getConfig().getInt("injury.bleed-damage-interval-seconds", 4) * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("injury.enabled", true)) return;
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    tickInjury(player);
                }
            }
        }.runTaskTimer(plugin, 20L, intervalTicks);
    }

    private void tickInjury(Player player) {
        PlayerState state = plugin.getPlayerData().get(player.getUniqueId());
        if (!state.isBleeding() && !state.isInfected()) return;

        long infectionAfter = plugin.getConfig().getInt("injury.infection-after-seconds", 90);
        if (state.isBleeding() && state.getBleedDurationSeconds() >= infectionAfter) {
            state.setInfected(true);
        }

        if (state.isInfected()) {
            player.damage(2);
            player.sendActionBar(ChatColor.DARK_RED + "상처가 감염되었습니다! 약초로 치료하세요.");
        } else if (state.isBleeding()) {
            player.damage(1);
            player.sendActionBar(ChatColor.RED + "출혈 중입니다... 붕대나 약초를 사용하세요.");
        }
    }

    /**
     * 몹에게 공격당했을 때 확률적으로 출혈 발생.
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("injury.enabled", true)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity)) return;

        double chance = plugin.getConfig().getDouble("injury.bleed-chance-on-hit", 0.35);
        if (random.nextDouble() < chance) {
            PlayerState state = plugin.getPlayerData().get(player.getUniqueId());
            if (!state.isBleeding()) {
                state.startBleeding();
                player.sendMessage(ChatColor.RED + "상처를 입어 출혈이 시작되었습니다!");
            }
        }
    }

    /**
     * 붕대 / 약초 우클릭 사용 처리.
     */
    @EventHandler
    public void onUseItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        PlayerState state = plugin.getPlayerData().get(player.getUniqueId());

        if (CustomItems.isBandage(item)) {
            if (!state.isBleeding() && !state.isInfected()) {
                player.sendMessage(ChatColor.GRAY + "지금은 붕대가 필요하지 않습니다.");
                return;
            }
            state.stopBleeding();
            player.setHealth(Math.min(player.getHealth() + 2, player.getAttribute(
                    org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
            player.sendMessage(ChatColor.WHITE + "붕대를 감아 출혈을 멈췄습니다. (감염은 치료되지 않았습니다)");
            item.setAmount(item.getAmount() - 1);
        } else if (CustomItems.isHerb(item)) {
            if (!state.isBleeding() && !state.isInfected()) {
                player.sendMessage(ChatColor.GRAY + "지금은 약초가 필요하지 않습니다.");
                return;
            }
            player.sendMessage(ChatColor.GREEN + "약초를 사용했습니다. 잠시 후 완치됩니다...");
            item.setAmount(item.getAmount() - 1);
            new BukkitRunnable() {
                @Override
                public void run() {
                    state.cureAll();
                    player.sendMessage(ChatColor.GREEN + "완치되었습니다!");
                }
            }.runTaskLater(plugin, 100L); // 5초 후 완치
        }
    }
}
