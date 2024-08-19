package com.luluroute.ms.integrate.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties
@Data
public class AppConfig {

    @Value("${config.shipment.artifact.core.topic}")
    String shipmentArtifactInputChannel;

    @Value("${config.shipment.artifact.retry.topic}")
    String shipmentArtifactRetryChannel;

    @Value("${config.shipment.artifact.dlq.topic}")
    String shipmentArtifactDlqChannel;

    @Value("${config.shipment.message.output.topic}")
    String shipmentMessageOutputChannel;

    @Value("${config.canadapost.carrier.code}")
    String canadaPostCarrierCode;

}
