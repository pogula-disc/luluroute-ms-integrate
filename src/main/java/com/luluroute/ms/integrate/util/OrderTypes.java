package com.luluroute.ms.integrate.util;

import java.util.Arrays;
import java.util.List;

public enum OrderTypes {

    ALLOC,
    REPLEN,
    SPECIAL,
    TRUNK,
    ECOMM,
    STRAT;

    public static boolean isRetailOrder(String orderType) {
        List<String> retailOrderTypeList = Arrays.asList(ALLOC.name(), REPLEN.name(), SPECIAL.name(), TRUNK.name());
        return retailOrderTypeList.stream().anyMatch(orderType::equalsIgnoreCase);
    }

    public static boolean isEcommOrder(String orderType) {
        return ECOMM.name().equalsIgnoreCase(orderType);
    }

    public static boolean isStratOrder(String orderType) {
        return STRAT.name().equalsIgnoreCase(orderType);
    }

}
