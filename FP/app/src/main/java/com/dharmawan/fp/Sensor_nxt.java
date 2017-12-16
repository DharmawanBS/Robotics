package com.dharmawan.fp;

/**
 * Created by dharmawan on 12/15/17.
 */

public class Sensor_nxt {
    public static final byte port1 = 0x00;
    public static final byte port2 = 0x01;
    public static final byte port3 = 0x02;
    public static final byte port4 = 0x03;

    public static final byte no_sensor = 0x00;
    public static final byte touch = 0x01;
    public static final byte temperature = 0x02;
    public static final byte reflection = 0x03;
    public static final byte angle = 0x04;
    public static final byte light_active = 0x05;
    public static final byte light_inactive = 0x06;
    public static final byte sound_db = 0x07;
    public static final byte sound_dba = 0x08;
    public static final byte custom = 0x09;
    public static final byte lowspeed = 0x0A;
    public static final byte lowspeed_9v = 0x0B;
    public static final byte sonar_metric = 0x0C;
    public static final byte sonar_inch = 0x0D;
    public static final byte compass = 0x0E;
    public static final byte io_8574_sensor = 0x0E;
    public static final byte colour = 0x0E;
    public static final byte gyro = 0x0E;
    public static final byte tilt = 0x0E;

    public static final byte raw_mode = (byte) 0x00;
    public static final byte bool_mode = (byte) 0x20;
    public static final byte transition_mode = (byte) 0x40;
    public static final byte period_mode = (byte) 0x60;
    public static final byte percent_mode = (byte) 0x80;
    public static final byte celcius_mode = (byte) 0XA0;
    public static final byte fahrenheit_mode = (byte) 0xC0;
    public static final byte angle_mode = (byte) 0xE0;

    public static final int power_jalan = 100;
    public static final int power_stop = 0;

    public static final String depan_nabrak = "Depan nabrak\n";
    public static final String belakang_nabrak = "Belakang nabrak\n";
    public static final String lampu_merah = "Lampu merah\n";
    public static final String diklakson = "Ada yang ngelakson\n";

    public static boolean con = false;

    public static final int min_red = 61;
    public static final int max_red = 64;

    public static final String start_string = "Please Connect";
}
