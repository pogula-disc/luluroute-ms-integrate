package com.luluroute.ms.integrate.processor;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.artifact.message.ShipmentArtifact;
import com.luluroute.ms.integrate.model.ShipmentContext;

public interface ShipmentResponse {

    ShipmentMessage buildResponse(ShipmentContext shipmentContext);
}
