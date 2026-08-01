package com.realmc;

import com.realmc.commands.AdminCommand;
import com.realmc.commands.RuleCommand;
import com.realmc.commands.SurvivalInfoCommand;
import com.realmc.data.PlayerDataManager;
import com.realmc.items.CustomItems;
import com.realmc.mobs.FactionManager;
import com.realmc.mobs.MonsterManager;
import com.realmc.systems.*;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealMCPlugin extends JavaPlugin implements Listener {

    private PlayerDataManager playerData;
    private TemperatureSystem temperatureSystem;
    private WeightSystem weightSystem;
    private InjurySystem injurySystem;
    private WeatherSystem weatherSystem;
    private DisasterSystem disasterSystem;
    private SwimSystem swimSystem;
    private MonsterManager monsterManager;
    private FactionManager factionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        playerData = new PlayerDataManager();

        CustomItems.init(
                new NamespacedKey(this, "bandage"),
                new NamespacedKey(this, "herb")
        );

        temperatureSystem = new TemperatureSystem(this);
        weightSystem = new WeightSystem(this);
        injurySystem = new InjurySystem(this);
        weatherSystem = new WeatherSystem(this);
        disasterSystem = new DisasterSystem(this);
        swimSystem = new SwimSystem(this);
        monsterManager = new MonsterManager(this);
        factionManager = new FactionManager(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(injurySystem, this);
        getServer().getPluginManager().registerEvents(weatherSystem, this);
        getServer().getPluginManager().registerEvents(monsterManager, this);
        getServer().getPluginManager().registerEvents(factionManager, this);

        temperatureSystem.start();
        weightSystem.start();
        injurySystem.start();
        weatherSystem.start();
        disasterSystem.start();
        swimSystem.start();
        monsterManager.start();
        factionManager.start();

        getCommand("룰").setExecutor(new RuleCommand());
        getCommand("재난").setExecutor(new AdminCommand(this));
        getCommand("생존정보").setExecutor(new SurvivalInfoCommand(this));

        getLogger().info("RealisticSurvival 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RealisticSurvival 플러그인이 비활성화되었습니다.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerData.get(player.getUniqueId()); // 상태 초기화
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        temperatureSystem.removeBar(event.getPlayer().getUniqueId());
        playerData.remove(event.getPlayer().getUniqueId());
    }

    public PlayerDataManager getPlayerData() {
        return playerData;
    }

    public DisasterSystem getDisasterSystem() {
        return disasterSystem;
    }

    public MonsterManager getMonsterManager() {
        return monsterManager;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }
}
