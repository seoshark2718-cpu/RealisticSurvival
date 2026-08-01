package com.realmc.data;

/**
 * 플레이어 1명의 생존 관련 상태를 담는 클래스.
 * (체온, 출혈, 감염, 수영 지속시간 등)
 */
public class PlayerState {

    private double temperature = 50; // 0~100, 50이 중립
    private boolean bleeding = false;
    private boolean infected = false;
    private long bleedStartMillis = 0;
    private long swimStartMillis = 0;
    private boolean swimming = false;

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = Math.max(0, Math.min(100, temperature));
    }

    public void addTemperature(double delta) {
        setTemperature(this.temperature + delta);
    }

    public boolean isBleeding() {
        return bleeding;
    }

    public void startBleeding() {
        if (!bleeding) {
            this.bleeding = true;
            this.bleedStartMillis = System.currentTimeMillis();
        }
    }

    public void stopBleeding() {
        this.bleeding = false;
        this.bleedStartMillis = 0;
    }

    public long getBleedDurationSeconds() {
        if (!bleeding) return 0;
        return (System.currentTimeMillis() - bleedStartMillis) / 1000;
    }

    public boolean isInfected() {
        return infected;
    }

    public void setInfected(boolean infected) {
        this.infected = infected;
    }

    public void cureAll() {
        this.bleeding = false;
        this.infected = false;
        this.bleedStartMillis = 0;
    }

    public boolean isSwimming() {
        return swimming;
    }

    public void startSwimming() {
        if (!swimming) {
            swimming = true;
            swimStartMillis = System.currentTimeMillis();
        }
    }

    public void stopSwimming() {
        swimming = false;
        swimStartMillis = 0;
    }

    public long getSwimDurationSeconds() {
        if (!swimming) return 0;
        return (System.currentTimeMillis() - swimStartMillis) / 1000;
    }
}
