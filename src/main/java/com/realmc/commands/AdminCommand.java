package com.realmc.commands;

import com.realmc.RealMCPlugin;
import com.realmc.systems.DisasterSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final RealMCPlugin plugin;

    public AdminCommand(RealMCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("realmc.admin")) {
            sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "사용법: /재난 <폭우|지진|해일|폭설|가뭄|산불>");
            return true;
        }

        DisasterSystem.DisasterType type = switch (args[0]) {
            case "폭우" -> DisasterSystem.DisasterType.HEAVY_RAIN;
            case "지진" -> DisasterSystem.DisasterType.EARTHQUAKE;
            case "해일" -> DisasterSystem.DisasterType.TSUNAMI;
            case "폭설" -> DisasterSystem.DisasterType.SNOWSTORM;
            case "가뭄" -> DisasterSystem.DisasterType.DROUGHT;
            case "산불" -> DisasterSystem.DisasterType.WILDFIRE;
            default -> null;
        };

        if (type == null) {
            sender.sendMessage(ChatColor.RED + "알 수 없는 재난 종류입니다. (폭우/지진/해일/폭설/가뭄/산불)");
            return true;
        }

        plugin.getDisasterSystem().trigger(type);
        sender.sendMessage(ChatColor.GREEN + type.name() + " 재난을 강제로 발동했습니다.");
        return true;
    }
}
