package com.luluroute.ms.integrate.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "config.shipment.artifact",ignoreUnknownFields = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsumerFilterConfig {
    List<FilterAttributes> filters;
}
