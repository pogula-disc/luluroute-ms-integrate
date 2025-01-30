package com.luluroute.ms.integrate.controller;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/v1/api/integrate")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "config.testlabellocal", havingValue = "true")
public class IntegrationServiceController {

    @PostMapping(value = "/artifact", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processLabelMessage(
            @RequestBody ShipmentMessage shipmentMessage) {
        return ResponseEntity.ok("Test label message is processed!");
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("health check is fine!");
    }

}
