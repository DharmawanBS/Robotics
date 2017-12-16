package com.dharmawan.fp;

public class SensorData {
    private int value;
    private byte sensor;
    private boolean active;
    private int port;

    public SensorData(boolean active,byte sensor,int port) {
        this.active = active;
        this.sensor = sensor;
        this.value = 0;
        this.port = port;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public byte getSensor() {
        return sensor;
    }

    public void setSensor(byte sensor) {
        this.sensor = sensor;
    }

    public void setValue(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
