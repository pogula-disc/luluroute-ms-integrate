/**
 * Author: Sravani Duggirala
 * Date:8/19/24
 */

package com.luluroute.ms.integrate.util;

import com.luluroute.ms.integrate.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class CarrierUtil {
    @Autowired
    private AppConfig appConfig;

    public static String CANADA_POST_CARRIER_CODE;

    @PostConstruct
    public void init() {
        CANADA_POST_CARRIER_CODE = appConfig.getCanadaPostCarrierCode();
    }



}
