package com.luluroute.ms.integrate.redis;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.integrate.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ShipmentMessageGlobalCache {

    final RedisConfig redisConfig;

    final ReactiveRedisOperations<String, ShipmentMessage> reactiveRedisOperations;

    public ShipmentMessage getShipmentMessage(String correlationId){
        String key = redisConfig.keyPrefix + correlationId;
        Boolean exists = reactiveRedisOperations.hasKey(key).block();
        if(exists) {
            return reactiveRedisOperations.opsForValue().get(key).block();
        } else {
            return null;
        }
    }

    public void saveShipmentResponse(String shipmentCorrelationId, ShipmentMessage shipmentMessage) {

        String key = redisConfig.shipmentResponseKeyPrefix + shipmentCorrelationId;
        ReactiveValueOperations<String, ShipmentMessage> reactiveValueOperations = reactiveRedisOperations.opsForValue();
        reactiveValueOperations.set(key, shipmentMessage, Duration.ofMillis(redisConfig.cacheClearTime)).subscribe();
    }

    public void saveCancelShipmentResponse(String shipmentCorrelationId, ShipmentMessage shipmentResponseMessage) {
        String key = redisConfig.shipmentCancelResponseKeyPrefix + shipmentCorrelationId;
        ReactiveValueOperations<String, ShipmentMessage> reactiveValueOperations = reactiveRedisOperations.opsForValue();
        reactiveValueOperations.set(key, shipmentResponseMessage, Duration.ofMillis(redisConfig.cacheClearTime)).subscribe();
    }
}
