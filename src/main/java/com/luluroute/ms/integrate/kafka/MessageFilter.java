package com.luluroute.ms.integrate.kafka;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.luluroute.ms.integrate.config.AppConfig;
import com.luluroute.ms.integrate.config.ConsumerFilterConfig;
import com.luluroute.ms.integrate.config.FilterAttributes;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;


@Component
public class MessageFilter {

    ConsumerFilterConfig filterConfig;

    MessageFilter(ConsumerFilterConfig filterConfig){
        this.filterConfig = filterConfig;
    }

    public  Predicate<ShipmentArtifact> shipmentArtifactFilter
            = shipmentArtifact -> {
        for(FilterAttributes filterAttributes: filterConfig.getFilters()){
            if(shipmentArtifact.getArtifactHeader().getArtifactType().toString().equalsIgnoreCase(filterAttributes.getArtifactType()) &&
            shipmentArtifact.getArtifactHeader().getArtifactStatus()==filterAttributes.getArtifactStatus())
                return true;
        }
         return false;
    };
}
