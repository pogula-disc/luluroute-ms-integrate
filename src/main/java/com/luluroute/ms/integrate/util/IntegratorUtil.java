package com.luluroute.ms.integrate.util;

import java.util.UUID;

public class IntegratorUtil {
    public static String getCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
