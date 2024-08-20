package com.isw.payapp.utils;

import java.util.Random;

public class CommonUtil {

    public String goRundom(int length){
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero");
        }
        // Generate a random number with the specified length
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // Generate random digit (0-9)
        }
        return sb.toString();
    }
}
