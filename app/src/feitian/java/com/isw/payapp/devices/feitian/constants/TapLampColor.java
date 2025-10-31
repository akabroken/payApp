package com.isw.payapp.devices.feitian.constants;

public class TapLampColor {
    // Default
    public interface Normal {
        int RED = 0xBA;
        int GREEN = 0xD7;
        int BLUE = 0x20;
    }
    // breathing light RGB
    public interface Breath {
        int RED_0 = 249;
        int GREEN_0 = 30;
        int BLUE_0 = 15;

        int RED_1 = 0x09;
        int GREEN_1 = 0xF6;
        int BLUE_1 = 0x0B;

        int RED_2 = 0x2C;
        int GREEN_2 = 0x9B;
        int BLUE_2 = 0xF5;
    }
    public interface Success {
        int RED = 0x16;
        int GREEN = 0xD6;
        int BLUE = 0xF3;
    }
    public interface Failed {
        int RED = 0xFF;
        int GREEN = 0x5B;
        int BLUE = 0x29;
    }
}
