package com.realmc.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 접속 중인 모든 플레이어의 PlayerState를 관리한다.
 */
public class PlayerDataManager {

    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    public PlayerState get(UUID uuid) {
        return states.computeIfAbsent(uuid, u -> new PlayerState());
    }

    public void remove(UUID uuid) {
        states.remove(uuid);
    }
}
