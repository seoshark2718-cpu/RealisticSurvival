package com.realmc.mobs;

import com.realmc.RealMCPlugin;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * "일반 몬스터" 시스템 - 게임 시작부터 등장.
 * - 바닐라 좀비/스켈레톤 자연 스폰은 막고, 대신 이 매니저가 직접 스폰시킨다.
 * - 무기를 들고 나오며, 종족(species)이 다르면 서로 적대적이다.
 * - 햇빛에 타지 않는다.
 * - 플레이어를 먼저 공격하지 않는다 (맞아야 반격).
 * - 체력이 낮아지면 겁쟁이(도망) / 광전사(끝까지 싸움) 성향에 따라 다르게 행동한다.
 */
public class MonsterManager implements Listener {

    private final RealMCPlugin plugin;
    private final Random random = new Random();

    private static final String META_MANAGED = "realmc_managed";
    private static final String META_SPECIES = "realmc_species";
    private static final String META_PROVOKED = "realmc_provoked";
    private static final String META_COWARD = "realmc_coward";
    private static final String META_SURRENDERED = "realmc_surrendered";

    public enum Species { UNDEAD, SKELETAL }

    public MonsterManager(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalTicks = plugin.getConfig().getInt("monsters.spawn-tick-interval-seconds", 15) * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("monsters.enabled", true)) return;
                double chance = plugin.getConfig().getDouble("monsters.spawn-chance", 0.4);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (random.nextDouble() < chance) {
                        trySpawnNear(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, intervalTicks);
    }

    private void trySpawnNear(Player player) {
        int max = plugin.getConfig().getInt("monsters.max-nearby-per-player", 6);
        long nearby = player.getNearbyEntities(32, 32, 32).stream()
                .filter(e -> e.hasMetadata(META_MANAGED)).count();
        if (nearby >= max) return;

        Location loc = player.getLocation().clone().add(
                random.nextInt(41) - 20, 0, random.nextInt(41) - 20);
        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);

        Species species = random.nextBoolean() ? Species.UNDEAD : Species.SKELETAL;
        spawnMonster(loc, species);
    }

    public LivingEntity spawnMonster(Location loc, Species species) {
        EntityType type = species == Species.UNDEAD ? EntityType.ZOMBIE : EntityType.SKELETON;
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

        entity.setMetadata(META_MANAGED, new FixedMetadataValue(plugin, true));
        entity.setMetadata(META_SPECIES, new FixedMetadataValue(plugin, species.name()));
        entity.setMetadata(META_PROVOKED, new FixedMetadataValue(plugin, false));
        boolean coward = random.nextDouble() < 0.4; // 40%는 겁쟁이, 나머지는 광전사 성향
        entity.setMetadata(META_COWARD, new FixedMetadataValue(plugin, coward));

        equip(entity, species);
        return entity;
    }

    private void equip(LivingEntity entity, Species species) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        // 무기: 종족에 따라 근접/원거리 성향 다르게, 확률적으로 방어구 등급 부여
        if (species == Species.SKELETAL || random.nextDouble() < 0.3) {
            eq.setItemInMainHand(new ItemStack(Material.BOW));
        } else {
            Material sword = pickWeighted(
                    new Material[]{Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD},
                    new double[]{0.4, 0.35, 0.2, 0.05});
            eq.setItemInMainHand(new ItemStack(sword));
        }

        Material[] tierHelmet = {Material.LEATHER_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET};
        Material[] tierChest = {Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE};
        double[] tierWeights = {0.55, 0.35, 0.10};
        int tier = weightedIndex(tierWeights);
        eq.setHelmet(new ItemStack(tierHelmet[tier]));
        eq.setChestplate(new ItemStack(tierChest[tier]));

        eq.setItemInMainHandDropChance(0.5f);
        eq.setHelmetDropChance(0.2f);
        eq.setChestplateDropChance(0.2f);
    }

    private Material pickWeighted(Material[] options, double[] weights) {
        return options[weightedIndex(weights)];
    }

    private int weightedIndex(double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (r <= cumulative) return i;
        }
        return weights.length - 1;
    }

    /** 햇빛에 타지 않도록 처리 */
    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntity().hasMetadata(META_MANAGED)) {
            event.setCancelled(true);
        }
    }

    /** 맞기 전엔 플레이어를 공격하지 않음 + 종족 다르면 서로 적대적 허용 */
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!event.getEntity().hasMetadata(META_MANAGED)) return;
        if (event.getTarget() == null) return;

        if (event.getTarget() instanceof Player) {
            boolean provoked = event.getEntity().getMetadata(META_PROVOKED).get(0).asBoolean();
            if (!provoked) {
                event.setCancelled(true);
            }
            return;
        }

        // 다른 종족 커스텀 몬스터라면 적대 허용
        if (event.getTarget().hasMetadata(META_MANAGED)) {
            String mySpecies = event.getEntity().getMetadata(META_SPECIES).get(0).asString();
            String otherSpecies = event.getTarget().getMetadata(META_SPECIES).get(0).asString();
            if (mySpecies.equals(otherSpecies)) {
                event.setCancelled(true); // 같은 종족끼리는 안 싸움
            }
        }
    }

    /** 플레이어에게 맞으면 그때부터 반격(provoked) 하고, 체력에 따라 도망/항복 처리 */
    @EventHandler
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (!event.getEntity().hasMetadata(META_MANAGED)) return;
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        mob.setMetadata(META_PROVOKED, new FixedMetadataValue(plugin, true));
        if (mob instanceof Mob mobEntity) {
            mobEntity.setTarget(player);
        }

        double healthPercent = (mob.getHealth() - event.getFinalDamage())
                / mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();

        boolean coward = mob.getMetadata(META_COWARD).get(0).asBoolean();

        if (healthPercent <= 0.15 && !mob.hasMetadata(META_SURRENDERED)) {
            // 항복 또는 도주
            if (coward) {
                mob.setMetadata(META_SURRENDERED, new FixedMetadataValue(plugin, true));
                if (mob instanceof Mob mobEntity) mobEntity.setTarget(null);
                EntityEquipment eq = mob.getEquipment();
                if (eq != null) {
                    ItemStack weapon = eq.getItemInMainHand();
                    if (weapon != null && weapon.getType() != Material.AIR) {
                        mob.getWorld().dropItem(mob.getLocation(), weapon);
                        eq.setItemInMainHand(null);
                    }
                }
                player.sendMessage(ChatColor.YELLOW + "상대가 무기를 버리고 항복했습니다. (죽이거나 살려둘 수 있습니다)");
            } else if (healthPercent <= 0.30 && random.nextDouble() < 0.5) {
                // 겁쟁이는 조금 더 일찍 도망 시도
                mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ZOMBIE_HURT, 1f, 1.5f);
            }
        }
    }

    /** 자연 스폰(바닐라 좀비/스켈레톤 등)은 취소하고, 관리형 스폰만 허용 */
    @EventHandler
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (!plugin.getConfig().getBoolean("general.disable-vanilla-hostile-mobs", true)) return;
        if (event.getEntity().hasMetadata(META_MANAGED)) return; // 우리가 스폰시킨 건 통과

        EntityType type = event.getEntityType();
        List<EntityType> hostileVanilla = List.of(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.WITCH, EntityType.DROWNED, EntityType.HUSK, EntityType.STRAY,
                EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.ENDERMAN
        );
        if (hostileVanilla.contains(type)
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.COMMAND) {
            event.setCancelled(true);
        }
    }
}
