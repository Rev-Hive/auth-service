package com.revhive.auth.util;

import java.util.concurrent.ThreadLocalRandom;

public class OtpUtil {

    public static String generateOtp() {
        return String.valueOf(
                ThreadLocalRandom.current()
                        .nextInt(100000, 1000000)
        );
    }
}
