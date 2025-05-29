package com.medco.HealthConnectProvider.utils.password;

import java.util.Random;

public class RandomNumberGenerator {

    public static String generateSixDigitNumber() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);

        return String.format("%06d", number);
    }
}
