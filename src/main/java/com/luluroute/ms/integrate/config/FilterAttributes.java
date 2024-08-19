package com.luluroute.ms.integrate.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilterAttributes {

    private String artifactType;
    private long artifactStatus;
}
