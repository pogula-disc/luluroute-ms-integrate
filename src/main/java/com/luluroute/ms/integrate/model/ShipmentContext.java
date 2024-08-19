package com.luluroute.ms.integrate.model;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.artifact.message.ShipmentArtifact;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public final class ShipmentContext {

    final ShipmentMessage shipmentRequest;

    final ShipmentArtifact shipmentArtifact;

}
