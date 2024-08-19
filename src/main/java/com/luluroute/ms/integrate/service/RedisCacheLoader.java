package com.luluroute.ms.integrate.service;

import com.logistics.luluroute.redis.shipment.carriermain.CarrierMainPayload;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.integrate.util.Constants.MESSAGE_REDIS_KEY_LOADING;

@Slf4j
@Service
public class RedisCacheLoader {

    @Cacheable(cacheNames = "MSE01-PROFILE", key = "#key", unless = "#result == null")
    public EntityPayload getEntityByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Entity Profile", key));
        return null;
    }

    @Cacheable(cacheNames = "MSCM01-PROFILE", key = "#key", unless = "#result == null")
    public CarrierMainPayload getCarrierByCode(String key) {
        log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Carrier Profile", key));
        return null;
    }

}


