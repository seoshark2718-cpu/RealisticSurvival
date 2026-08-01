package com.realmc.util;

import org.bukkit.World;

/**
 * 월드의 fullTime을 기반으로 "인게임 몇 일째"인지 계산하는 유틸리티.
 * 마인크래프트 하루 = 24000 틱.
 */
public final class DayCounter {

    private DayCounter() {}

    public static long getDay(World world) {
        return world.getFullTime() / 24000L;
    }
}
