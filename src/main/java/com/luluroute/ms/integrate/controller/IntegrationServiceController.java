package com.luluroute.ms.integrate.controller;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.integrate.config.SwaggerConfig;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/integrate")
@Slf4j
@Api(value = "/v1/api/integrate", tags = {SwaggerConfig.INTEGRATOR_SERVICE})
@RequiredArgsConstructor
@ConditionalOnProperty(value = "config.testlabellocal", havingValue = "true")
public class IntegrationServiceController {

    @PostMapping(value = "/artifact", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processLabelMessage(
            @RequestBody ShipmentMessage shipmentMessage) {
        return ResponseEntity.ok("Test label message is processed!");
    }

}
