package com.realmc.mobs;

import com.realmc.RealMCPlugin;
import com.realmc.util.DayCounter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * 적대 세력 시스템.
 * - 인게임 150일 이후부터 등장 (config: factions.unlock-day)
 * - 플레이어를 발견하면 먼저 추적하고 전투를 건다 (일반 몬스터와 반대)
 * - 세력마다 다른 갑옷/무기 세트를 착용
 * - 등장 빈도는 150일 이후 시간이 지날수록 점진적으로 증가 (상한 있음)
 * - 300일 이후에는 우두머리(보스)급 개체가 드물게 등장
 */
public class FactionManager implements Listener {

    private final RealMCPlugin plugin;
    private final Random random = new Random();
    private boolean firstAppearanceAnnounced = false;
    private boolean forebodingStarted = false;

    private static final String META_FACTION = "realmc_faction";
    private static final String META_BOSS = "realmc_faction_boss";

    public enum Faction { IRON_BLOOD, SHADOW_CLAN }

    public FactionManager(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalTicks = plugin.getConfig().getInt("factions.spawn-interval-seconds", 60) * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("factions.enabled", true)) return;
                tick();
            }
        }.runTaskTimer(plugin, 100L, intervalTicks);
    }

    private void tick() {
        if (Bukkit.getWorlds().isEmpty()) return;
        World mainWorld = Bukkit.getWorlds().get(0);
        long day = DayCounter.getDay(mainWorld);
        long unlockDay = plugin.getConfig().getInt("factions.unlock-day", 150);
        long hintDaysBefore = plugin.getConfig().getInt("factions.foreboding-hint-days-before", 10);

        // 등장 전 불길한 징조
        if (day >= unlockDay - hintDaysBefore && day < unlockDay) {
            if (!forebodingStarted) {
                forebodingStarted = true;
            }
            if (random.nextDouble() < 0.15) {
                Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "불길한 기운이 감돕니다... 무언가 다가오고 있습니다.");
            }
            return;
        }

        if (day < unlockDay) return;

        // 첫 등장 이벤트
        if (!firstAppearanceAnnounced) {
            firstAppearanceAnnounced = true;
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD
                    + "===== 적대 세력이 나타났습니다 =====");
            Bukkit.broadcastMessage(ChatColor.RED
                    + "이제부터 조직화된 적대 세력이 당신을 노립니다. 대비하십시오.");
        }

        int maxAlive = plugin.getConfig().getInt("factions.max-alive-server-wide", 25);
        long aliveNow = Bukkit.getWorlds().stream()
                .flatMap(w -> w.getEntities().stream())
                .filter(e -> e.hasMetadata(META_FACTION)).count();
        if (aliveNow >= maxAlive) return;

        // 150일 직후엔 적게, 시간이 지날수록 점차 증가 (최대 3배)
        double growthDays = Math.min(day - unlockDay, 150); // 150일 더 지나면 성장 상한
        double scale = 1.0 + (growthDays / 150.0) * 2.0;
        double baseChance = plugin.getConfig().getDouble("factions.base-spawn-chance", 0.3);
        double chance = Math.min(0.9, baseChance * scale);

        long bossUnlockDay = plugin.getConfig().getInt("factions.boss-unlock-day", 300);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (random.nextDouble() > chance) continue;
            Location spawnLoc = pickSpawnLocation(player);
            if (spawnLoc == null) continue;

            boolean spawnBoss = day >= bossUnlockDay && random.nextDouble() < 0.03;
            Faction faction = random.nextBoolean() ? Faction.IRON_BLOOD : Faction.SHADOW_CLAN;
            spawnFactionMob(spawnLoc, faction, spawnBoss);
        }
    }

    private Location pickSpawnLocation(Player player) {
        int minDist = plugin.getConfig().getInt("factions.min-distance-from-player", 20);
        int maxDist = plugin.getConfig().getInt("factions.max-distance-from-player", 45);
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            double dx = Math.cos(angle) * dist;
            double dz = Math.sin(angle) * dist;
            Location loc = player.getLocation().clone().add(dx, 0, dz);
            loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
            if (loc.getBlock().getType() == Material.AIR || loc.getBlock().isPassable()) {
                return loc;
            }
        }
        return null;
    }

    public LivingEntity spawnFactionMob(Location loc, Faction faction, boolean boss) {
        EntityType type = faction == Faction.IRON_BLOOD ? EntityType.VINDICATOR : EntityType.PILLAGER;
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, type);

        entity.setMetadata(META_FACTION, new FixedMetadataValue(plugin, faction.name()));
        if (boss) {
            entity.setMetadata(META_BOSS, new FixedMetadataValue(plugin, true));
            entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(80);
            entity.setHealth(80);
            entity.setCustomName(ChatColor.DARK_RED + factionName(faction) + " 우두머리");
            entity.setCustomNameVisible(true);
        } else {
            entity.setCustomName(ChatColor.RED + factionName(faction));
            entity.setCustomNameVisible(false);
        }

        equip(entity, faction, boss);

        // 즉시 추적 상태로 진입: 가장 가까운 플레이어를 타겟팅
        Player nearest = loc.getWorld().getPlayers().stream()
                .min((a, b) -> Double.compare(a.getLocation().distance(loc), b.getLocation().distance(loc)))
                .orElse(null);
        if (nearest != null && entity instanceof Mob mob) {
            mob.setTarget(nearest);
        }

        if (boss) {
            Bukkit.broadcastMessage(ChatColor.DARK_RED + "[경고] " + factionName(faction)
                    + " 우두머리가 나타났습니다!");
        }

        return entity;
    }

    private void equip(LivingEntity entity, Faction faction, boolean boss) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        if (faction == Faction.IRON_BLOOD) {
            // 철혈단: 중장갑 근접
            eq.setHelmet(new ItemStack(boss ? Material.NETHERITE_HELMET : Material.IRON_HELMET));
            eq.setChestplate(new ItemStack(boss ? Material.NETHERITE_CHESTPLATE : Material.IRON_CHESTPLATE));
            eq.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            eq.setBoots(new ItemStack(Material.IRON_BOOTS));
            eq.setItemInMainHand(new ItemStack(boss ? Material.NETHERITE_AXE : Material.IRON_AXE));
        } else {
            // 그림자단: 경장갑 원거리
            eq.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            eq.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            eq.setItemInMainHand(new ItemStack(Material.CROSSBOW));
        }

        eq.setItemInMainHandDropChance(0.4f);
        eq.setHelmetDropChance(0.15f);
        eq.setChestplateDropChance(0.15f);
    }

    private String factionName(Faction faction) {
        return faction == Faction.IRON_BLOOD ? "철혈단" : "그림자단";
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.hasMetadata(META_FACTION) && !entity.hasMetadata(META_BOSS)) {
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata(META_FACTION)) return;
        if (entity.hasMetadata(META_BOSS)) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "적대 세력의 우두머리가 처치되었습니다!");
        }
    }
}
