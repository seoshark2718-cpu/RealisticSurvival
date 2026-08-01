package com.realmc.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RuleCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===== 생존 안내 =====");
        sender.sendMessage(ChatColor.YELLOW + "이 서버는 리얼리스틱 서바이벌 플러그인이 적용되어 있습니다.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + "[체온] " + ChatColor.WHITE
                + "추운 곳/밤/비를 맞으면 체온이 떨어집니다. 불 근처에 있거나 갑옷을 입으세요.");
        sender.sendMessage(ChatColor.AQUA + "  체온이 너무 낮으면 저체온증으로 지속 피해를 입습니다.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "[부상] " + ChatColor.WHITE
                + "몬스터에게 맞으면 확률적으로 출혈이 발생합니다.");
        sender.sendMessage(ChatColor.RED + "  붕대로 지혈, 약초로 완치할 수 있습니다. 방치하면 감염됩니다.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "[무게] " + ChatColor.WHITE
                + "인벤토리에 무거운 것(갑옷, 블록 등)을 너무 많이 들면 느려집니다.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "[재난] " + ChatColor.WHITE
                + "폭우, 지진, 해일, 폭설, 가뭄, 산불이 랜덤한 강도로 발생합니다. 전조가 보이면 대비하세요.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_RED + "[적대 세력] " + ChatColor.WHITE
                + "인게임 150일 이후부터 조직화된 적대 세력이 등장해 당신을 추적하고 공격합니다.");
        sender.sendMessage(ChatColor.DARK_RED + "  그 전에 장비와 거점을 미리 준비해두세요.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "[몬스터] " + ChatColor.WHITE
                + "일반 몬스터는 먼저 공격하지 않으면 공격하지 않습니다. 종족이 다르면 몬스터끼리도 싸웁니다.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "/생존정보 명령어로 자신의 상태를 확인할 수 있습니다.");
        return true;
    }
}
