package com.luluroute.ms.integrate.service;


import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Service.TransitInfo;
import com.logistics.luluroute.domain.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.redis.shipment.entity.AssignedTransitModes;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.integrate.model.Scenario;
import com.luluroute.ms.integrate.model.ShipmentContext;
import com.luluroute.ms.integrate.processor.ShipmentIntegrateProcessor;
import com.luluroute.ms.integrate.redis.ShipmentMessageGlobalCache;
import com.luluroute.ms.integrate.util.DateUtil;
import com.luluroute.ms.integrate.util.OrderTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;



import static com.luluroute.ms.integrate.util.Constants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentIntegrateService {

    final ShipmentMessageGlobalCache shipmentMessageGlobalCache;

    @Autowired
    private RedisCacheLoader redisCacheLoader;
    
    @Value("${config.psd.calculation}")
    private List<String> pstDcs;

    @Async("AsyncTaskExecutor")
    public void processArtifactMessageExecutor(ShipmentArtifact shipmentArtifact) {
        String msg = "ShipmentIntegrateService.processArtifactMessageExecutor()";
        try {
            processArtifactMessage(shipmentArtifact);
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, e);
            throw e;
        }
    }

    public void processArtifactMessage(ShipmentArtifact shipmentArtifact) {
        ShipmentMessage shipmentResponseMessage;
        long plannedShipDate = 0;

        MDC.put(X_SHIPMENT_CORRELATION_ID, getShipmentCorrelationId(shipmentArtifact));

        // 1. Retrieve corresponding ShipmentMessage Request
        ShipmentMessage shipmentRequestMessage = shipmentMessageGlobalCache.getShipmentMessage(getShipmentCorrelationId(shipmentArtifact));

        log.info("Loading ShipmentMessage {} ", shipmentRequestMessage);

        ShipmentContext shipmentContext = new ShipmentContext(shipmentRequestMessage, shipmentArtifact);

        // 2. Identify the Scenario
        Scenario scenario = ShipmentIntegrateProcessor.identifyScenario(shipmentContext);
        log.info("Scenario Identified: {}",
               scenario.name());

        if(scenario == Scenario.CANCEL_SHIPMENT_SUCCESS || scenario == Scenario.CANCEL_SHIPMENT_FAILURE) {
            shipmentResponseMessage = scenario.buildResponse(shipmentContext);
            shipmentMessageGlobalCache.saveCancelShipmentResponse(getShipmentCorrelationId(shipmentArtifact), shipmentResponseMessage);
        } else {

            if( null != shipmentContext.getShipmentRequest()) {
                TransitInfo transitDetails = shipmentContext.getShipmentRequest().getMessageBody().getShipments().get(0).getTransitDetails();
                plannedShipDate = transitDetails.getDateDetails().getPlannedShipDate();
            }

            // 3. Build ShipmentMessage Response
            shipmentResponseMessage = scenario.buildResponse(shipmentContext);

            // Update Cut-off time for respective carrier-mode
            // If PSD has been moved/changed
           if(scenario == Scenario.CREATE_SHIPMENT_SUCCESS)
               updateCutOffTimes(shipmentResponseMessage, plannedShipDate, shipmentContext);

            // 4. Save ShipmentMessage Response to the cache
            shipmentMessageGlobalCache.saveShipmentResponse(getShipmentCorrelationId(shipmentArtifact), shipmentResponseMessage);
        }
        log.info("APP_MESSAGE=\"Response processed successfully to Redis\" | message=\"{}\"", shipmentResponseMessage);
    }

    private String getShipmentCorrelationId(ShipmentArtifact shipmentArtifact) {
        return  shipmentArtifact.getArtifactHeader().getShipmentCorrelationId();
    }

    /**
     *
     * @param shipmentMessage
     * @param shipmentContext
     */
    private void updateCutOffTimes(ShipmentMessage shipmentMessage,long plannedShipDate, ShipmentContext shipmentContext) {
        String msg = "ShipmentIntegrateService.updateCutOffTimes()";

        try {
            ShipmentInfo shipmentInfo = shipmentMessage.getMessageBody().getShipments().get(0);
            TransitInfo toUpdateTransitDetails = shipmentInfo.getTransitDetails();
            ShipmentArtifact shipmentArtifact = shipmentContext.getShipmentArtifact();

            EntityPayload entityProfile = redisCacheLoader.getEntityByCode(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode());
            if (null != shipmentContext.getShipmentArtifact().getArtifactBody().getCarrierExternalData()
                    && null != entityProfile) {
                AssignedTransitModes assignedTransitModes = getAssignedTransitModes(entityProfile,
                        shipmentContext.getShipmentArtifact().getArtifactBody().getCarrierExternalData().getCarrierCode(),
                        shipmentContext.getShipmentArtifact().getArtifactBody().getCarrierExternalData().getModeCode(),
                        shipmentInfo.getOrderDetails().getOrderType());

                log.info(String.format(STANDARD_FIELD_INFO, plannedShipDate,
                        shipmentArtifact.getArtifactBody().getCarrierExternalData().getTransitTimeInfo().getShippedDate()));

                log.info(String.format(STANDARD_FIELD_INFO, "CutOffHH", assignedTransitModes.getCutOffHH()));
                log.info(String.format(STANDARD_FIELD_INFO, "CutOffMM", assignedTransitModes.getCutOffMM()));
                log.info(String.format(STANDARD_FIELD_INFO, "DC s configured PST", pstDcs));
                //If request comes from MA Active we need to send in UTC and for others we need to send as PST
                long updatedCutOffHH  = 0;
                long updatedCutOffMM  = 0;
				if (pstDcs.contains(shipmentInfo.getShipmentHeader().getOrigin().getEntityCode())) {
					updatedCutOffHH = Long.parseLong(assignedTransitModes.getCutOffHH());
					updatedCutOffMM = Long.parseLong(assignedTransitModes.getCutOffMM());
				} else {
					updatedCutOffHH = Long.parseLong(assignedTransitModes.getCutOffHH())
							+ (DateUtil.offsetBetweenTimezone(entityProfile.getTimezone()) / 60);
					updatedCutOffMM = Long.parseLong(assignedTransitModes.getCutOffMM())
							+ (DateUtil.offsetBetweenTimezone(entityProfile.getTimezone()) % 60);
				}

                toUpdateTransitDetails.getDateDetails().setCutOffTimeHH(updatedCutOffHH % 24);
                toUpdateTransitDetails.getDateDetails().setCutOffTimeMM(updatedCutOffMM % 60);

                log.info(String.format(STANDARD_FIELD_INFO, "updated CutOffHH", toUpdateTransitDetails.getDateDetails().getCutOffTimeHH()));
                log.info(String.format(STANDARD_FIELD_INFO, "updated CutOffMM", toUpdateTransitDetails.getDateDetails().getCutOffTimeMM()));

                if (plannedShipDate !=
                        shipmentArtifact.getArtifactBody().getCarrierExternalData().getTransitTimeInfo().getShippedDate())
                    toUpdateTransitDetails.getDateDetails().setCutOffTimeApplied(Boolean.TRUE);

            }
        } catch (Exception e){
            log.error( STANDARD_ERROR,msg, e);
        }
    }

    /**
     *
     * @param entityProfile
     * @param carrierCode
     * @param carrierModeCode
     * @return
     */
    private AssignedTransitModes getAssignedTransitModes(EntityPayload entityProfile, String carrierCode, String carrierModeCode, String orderType) {
        String msg = "ShipmentIntegrateService.getAssignedTransitModes()";
        for (AssignedTransitModes assignedTransitModes : entityProfile.getAssignedTransitModes()) {
            if (carrierCode.equalsIgnoreCase(assignedTransitModes.getCarrierCode()) && carrierModeCode.equalsIgnoreCase(assignedTransitModes.getModeCode())) {
				if (isRetailAssignedTransitMode(orderType, assignedTransitModes.getRef1())
						|| isStratAssignedTransitMode(orderType, assignedTransitModes.getRef1())) {
					return assignedTransitModes;
				} else if (isNonRetailAssignedTransitMode(orderType, assignedTransitModes.getRef1())) {
                    return assignedTransitModes;
                }
            }
        }
        return null;
    }

    /**
     * @param shipmentOrderType Shipment Message Order Type
     * @param transitModeOrderType Current Assigned Transit Mode Order Type
     * @return true if Shipment Message is Retail, Current assigned transit mode's order type is RETAIL
     */
    private boolean isRetailAssignedTransitMode(String shipmentOrderType, String transitModeOrderType) {
        return OrderTypes.isRetailOrder(shipmentOrderType)
                && !StringUtils.isEmpty(transitModeOrderType)
                && transitModeOrderType.equalsIgnoreCase(RETAIL);
    }

    /**
     *
     * @param shipmentOrderType Shipment Message Order Type
     * @param transitModeOrderType Current Assigned Transit Mode Order Type
     * @return true if Shipment Message is NOT Retail, Current assigned transit mode's order type is null or empty string
     */
    private boolean isNonRetailAssignedTransitMode(String shipmentOrderType, String transitModeOrderType) {
        return !OrderTypes.isRetailOrder(shipmentOrderType)
                && StringUtils.isEmpty(transitModeOrderType);
    }
    
    
    /**
	 * @param shipmentOrderType    Shipment Message Order Type
	 * @param transitModeOrderType Current Assigned Transit Mode Order Type
	 * @return true if Shipment Message is STRAT , COMM or B2B,
	 */
	private boolean isStratAssignedTransitMode(String shipmentOrderType, String transitModeOrderType) {
		return OrderTypes.isStratOrder(shipmentOrderType) && !StringUtils.isEmpty(transitModeOrderType)
				&& transitModeOrderType.equalsIgnoreCase(shipmentOrderType);
	}
}
