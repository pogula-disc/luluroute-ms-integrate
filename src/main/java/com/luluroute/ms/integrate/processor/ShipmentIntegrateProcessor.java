package com.luluroute.ms.integrate.processor;

import com.luluroute.ms.integrate.model.Scenario;
import com.luluroute.ms.integrate.model.ShipmentContext;


public class ShipmentIntegrateProcessor {

    public static Scenario identifyScenario(ShipmentContext shipmentContext){
        String artifactType = shipmentContext.getShipmentArtifact().getArtifactHeader().getArtifactType();
        long artifactStatus = shipmentContext.getShipmentArtifact().getArtifactHeader().getArtifactStatus();
        if(artifactType.equals("9100") && artifactStatus==200)
            return Scenario.CANCEL_SHIPMENT_SUCCESS;

        if(artifactType.equals("9100") && artifactStatus==500)
            return Scenario.CANCEL_SHIPMENT_FAILURE;

        if(shipmentContext.getShipmentRequest()==null)
            return Scenario.SHIPMENT_PROCESSING_FAILURE;


        if(artifactType.equals("7900") && artifactStatus==200)
            return Scenario.CREATE_SHIPMENT_SUCCESS;

        if(artifactType.equals("7900") && artifactStatus==500)
            return Scenario.CREATE_SHIPMENT_FAILURE;

        if(artifactType.equals("5100") && artifactStatus == 500)
            return Scenario.ROUTE_RULE_FAILURE;



        return Scenario.SHIPMENT_PROCESSING_FAILURE;
    }
}
