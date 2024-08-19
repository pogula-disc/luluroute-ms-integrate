package com.luluroute.ms.integrate.model;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.integrate.processor.ShipmentMessageFactory;
import com.luluroute.ms.integrate.processor.ShipmentResponse;

public enum Scenario implements ShipmentResponse {

    CREATE_SHIPMENT_SUCCESS{
        @Override
        public ShipmentMessage buildResponse(ShipmentContext shipmentContext) {
            return ShipmentMessageFactory.buildCreateShipmentSuccessResponse(shipmentContext);

        }
    },
    CREATE_SHIPMENT_FAILURE{
        @Override
        public ShipmentMessage buildResponse(ShipmentContext shipmentContext) {
            return ShipmentMessageFactory.buildCreateShipmentFailureResponse(shipmentContext);
        }
    },

    CANCEL_SHIPMENT_SUCCESS{
        @Override
        public ShipmentMessage buildResponse(ShipmentContext shipmentContext) {
            return ShipmentMessageFactory.buildCancelShipmentSuccessResponse(shipmentContext);
        }
    },

    CANCEL_SHIPMENT_FAILURE{
        @Override
        public ShipmentMessage buildResponse(ShipmentContext shipmentContext) {
            return ShipmentMessageFactory.buildCancelShipmentFailureResponse(shipmentContext);
        }
    },

    SHIPMENT_PROCESSING_FAILURE{
        @Override
        public ShipmentMessage buildResponse(ShipmentContext shipmentContext) {
            return ShipmentMessageFactory.buildShipmentProcessingFailureResponse(shipmentContext);
        }
    },

    ROUTE_RULE_FAILURE {
        @Override
        public ShipmentMessage buildResponse(ShipmentContext shipmentContext) {
            return ShipmentMessageFactory.buildCreateShipmentFailureResponse(shipmentContext);
        }
    }



}
