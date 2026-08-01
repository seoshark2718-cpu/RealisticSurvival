package com.realmc.commands;

import com.realmc.RealMCPlugin;
import com.realmc.data.PlayerState;
import com.realmc.util.DayCounter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SurvivalInfoCommand implements CommandExecutor {

    private final RealMCPlugin plugin;

    public SurvivalInfoCommand(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        PlayerState state = plugin.getPlayerData().get(player.getUniqueId());
        long day = DayCounter.getDay(player.getWorld());
        long unlockDay = plugin.getConfig().getInt("factions.unlock-day", 150);

        player.sendMessage(ChatColor.GOLD + "===== 생존 정보 =====");
        player.sendMessage(ChatColor.AQUA + "체온: " + ChatColor.WHITE + Math.round(state.getTemperature()) + "/100");
        player.sendMessage(ChatColor.RED + "출혈: " + ChatColor.WHITE + (state.isBleeding() ? "예" : "아니오"));
        player.sendMessage(ChatColor.DARK_RED + "감염: " + ChatColor.WHITE + (state.isInfected() ? "예" : "아니오"));
        player.sendMessage(ChatColor.GRAY + "인게임 " + day + "일째");
        if (day < unlockDay) {
            player.sendMessage(ChatColor.YELLOW + "적대 세력 등장까지 " + (unlockDay - day) + "일 남음");
        } else {
            player.sendMessage(ChatColor.DARK_RED + "적대 세력이 활동 중입니다. 주의하세요.");
        }
        return true;
    }
}
