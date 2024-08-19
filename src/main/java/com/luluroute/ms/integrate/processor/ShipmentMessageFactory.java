package com.luluroute.ms.integrate.processor;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.integrate.model.ShipmentContext;

public class ShipmentMessageFactory {
    public static ShipmentMessage buildShipmentProcessingFailureResponse(ShipmentContext shipmentContext) {
        return ShipmentMessageResponseBuilder.buildFailureResponse(shipmentContext.getShipmentArtifact());

    }


    public static ShipmentMessage buildCreateShipmentSuccessResponse(ShipmentContext shipmentContext){

        return ShipmentMessageResponseBuilder.buildCreateShipmentSuccess(
                shipmentContext.getShipmentRequest(),
                shipmentContext.getShipmentArtifact()
        );
    }

    public static ShipmentMessage buildCreateShipmentFailureResponse(ShipmentContext shipmentContext){

        return ShipmentMessageResponseBuilder.buildCreateShipmentFailure(shipmentContext.getShipmentRequest(),shipmentContext.getShipmentArtifact());
    }

    public static ShipmentMessage buildCancelShipmentSuccessResponse(ShipmentContext shipmentContext){

        return ShipmentMessageResponseBuilder.buildCancelShipmentSuccess(shipmentContext.getShipmentArtifact());
    }

    public static ShipmentMessage buildCancelShipmentFailureResponse(ShipmentContext shipmentContext){

        return ShipmentMessageResponseBuilder.buildCancelShipmentFailure(shipmentContext.getShipmentArtifact());
    }
}
