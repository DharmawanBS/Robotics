package com.dharmawan.fp;

/**
 * Created by dharmawan on 12/15/2017.
 */

public class Sensor_value {
    int touch1,touch2,sound,light;

    public Sensor_value() {
        this.touch1 = 255;
        this.touch2 = 255;
        this.sound = 0;
        this.light = 0;
    }

    public Sensor_value(int touch1, int touch2, int sound, int light) {
        this.touch1 = touch1;
        this.touch2 = touch2;
        this.sound = sound;
        this.light = light;
    }

    public int getTouch1() {
        return touch1;
    }

    public void setTouch1(int touch1) {
        this.touch1 = touch1;
    }

    public int getTouch2() {
        return touch2;
    }

    public void setTouch2(int touch2) {
        this.touch2 = touch2;
    }

    public int getSound() {
        return sound;
    }

    public void setSound(int sound) {
        this.sound = sound;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }
}
