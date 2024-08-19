package com.luluroute.ms.integrate.processor;

import com.logistics.luluroute.domain.Shipment.Carrier.CarrierInfo;
import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.MessageHeaderInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentHeader;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.BillingInfo;
import com.logistics.luluroute.domain.Shipment.Service.RequestInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Service.TransitInfo;
import com.logistics.luluroute.domain.Shipment.Shared.ItemInfo;
import com.logistics.luluroute.domain.Shipment.Shared.RateShopResponse;
import com.logistics.luluroute.domain.Shipment.Shared.ResponseItem;
import com.logistics.luluroute.domain.Shipment.Shared.StatusItem;
import com.logistics.luluroute.domain.artifact.message.*;
import com.logistics.luluroute.validator.ValidationUtil;
import com.luluroute.ms.integrate.config.AppConfig;
import com.luluroute.ms.integrate.util.Constants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.luluroute.ms.integrate.util.Constants.PROCESS_EXCEPTION_DETAIL;
import static com.luluroute.ms.integrate.util.Constants.RESPONSE_MESSAGE_FAILURE;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShipmentMessageResponseBuilder {

    private static String CANADAPOST_CARRIER_CODE;

    @PostConstruct
    public void init() {
        CANADAPOST_CARRIER_CODE = appConfig.getCanadaPostCarrierCode();
    }

    public static ShipmentMessage buildCreateShipmentSuccess(ShipmentMessage shipmentRequest, ShipmentArtifact shipmentArtifact) {


        long CREATE_SHIPMENT_SUCCESS_REQUEST_TYPE = 2000;
        long CREATE_SHIPMENT_SUCCESS_RESPONSE_CODE = 1000;
        String RESPONSE_MESSAGE_SUCCESS = "Success";
        String carrierCode = shipmentArtifact.getArtifactBody().getCarrierExternalData().getCarrierCode();
        long date = Instant.now().getEpochSecond();

        RequestInfo.RequestInfoBuilder requestInfoBuilder = RequestInfo.builder();
        requestInfoBuilder
                .requestType(String.valueOf(CREATE_SHIPMENT_SUCCESS_REQUEST_TYPE))
                .requestDate(date)
                .response(ResponseItem.builder()
                        .responseCode(String.valueOf(CREATE_SHIPMENT_SUCCESS_RESPONSE_CODE))
                        .responseMessage(RESPONSE_MESSAGE_SUCCESS)
                        .responseDate(date)
                        .build());

        StatusItem.StatusItemBuilder messageStatusBuilder = StatusItem.builder();
        messageStatusBuilder.status(CREATE_SHIPMENT_SUCCESS_RESPONSE_CODE)
                .statusDate(date);

        MessageHeaderInfo.MessageHeaderInfoBuilder messageHeaderBuilder = MessageHeaderInfo.builder();
        messageHeaderBuilder
                .messageCorrelationId(shipmentRequest.getMessageHeader().getMessageCorrelationId())
                .sequence(1)
                .totalSequence(1)
                .messageDate(date)
                .messageSources(shipmentRequest.getMessageHeader().getMessageSources());


        MessageBodyInfo.MessageBodyInfoBuilder messageBodyInfoBuilder = MessageBodyInfo.builder();
        ShipmentInfo shipmentsRequest = shipmentRequest.getMessageBody().getShipments().get(0);

        TransitInfo transitDetails = shipmentsRequest.getTransitDetails();
        transitDetails.getLabelDetails().setLabel(shipmentArtifact.getArtifactBody().getLabelInfo().getContentRendered());
        //ITLO-8782 :sending the Format as PDF for Canadapost in the ms-service response
        if(carrierCode!=null && carrierCode.equalsIgnoreCase(CANADAPOST_CARRIER_CODE)) {
            transitDetails.getLabelDetails().setFormat(shipmentArtifact.getArtifactBody().getLabelInfo().getFormatFromCarrier());
        }
        transitDetails.setShipmentId(shipmentArtifact.getArtifactBody().getCarrierExternalData().getShipmentId());
        transitDetails.setTrackingNo(shipmentArtifact.getArtifactBody().getCarrierExternalData().getTracking().getTrackingId());
        transitDetails.setAltTrackingNo(shipmentArtifact.getArtifactBody().getCarrierExternalData().getTracking()
                .getConsignmentId());
        transitDetails.setTransitMode(shipmentArtifact.getArtifactBody().getCarrierExternalData().getModeCode());
        transitDetails.getDateDetails().setPlannedDeliveryDate(
                shipmentArtifact.getArtifactBody().getCarrierExternalData().getTransitTimeInfo().getPlannedDeliveryDate()
        );
        transitDetails.getDateDetails().setPlannedShipDate(
                shipmentArtifact.getArtifactBody().getCarrierExternalData().getTransitTimeInfo().getShippedDate()
        );
        transitDetails.getDateDetails().setTransitDays(calculateBusinessTransitDays(shipmentArtifact));

        ShipmentHeader.ShipmentHeaderBuilder shipmentHeaderBuilder = shipmentsRequest.getShipmentHeader().toBuilder();
        shipmentHeaderBuilder.carrier(
                CarrierInfo.builder()
                        .carrierCode(shipmentArtifact.getArtifactBody().getCarrierExternalData().getCarrierCode())
                        .carrierName(shipmentArtifact.getArtifactBody().getCarrierExternalData().getCarrierName())
                        .build()
        );
			
		setAccountDetails(shipmentArtifact, shipmentsRequest);
        ShipmentInfo shipmentsResponse = ShipmentInfo.builder()
                .shipmentHeader(shipmentHeaderBuilder.build())
                .transitDetails(transitDetails)
                .orderDetails(shipmentsRequest.getOrderDetails())
                .shipmentStatus(shipmentsRequest.getShipmentStatus())
                .shipmentPieces(shipmentsRequest.getShipmentPieces())
                .rateShopResponses(getRatesInfo(shipmentArtifact))
                .build();

        messageBodyInfoBuilder.shipments(List.of(shipmentsResponse));

        return ShipmentMessage.builder().
                RequestHeader(requestInfoBuilder.build())
                .MessageStatus(messageStatusBuilder.build())
                .MessageHeader(messageHeaderBuilder.build())
                .MessageBody(messageBodyInfoBuilder.build())
                .build();


    }

    /**
     * This method is added to persist account details which are needed for Manifest IA
     * @param shipmentArtifact
     * @param shipmentsRequest
     */
	private static void setAccountDetails(ShipmentArtifact shipmentArtifact, ShipmentInfo shipmentsRequest) {
		List<String> referenceAccounts = shipmentArtifact.getArtifactBody().getCarrierExternalData().getTracking()
				.getReferences();
		if (!CollectionUtils.isEmpty(referenceAccounts)) {
			if (null != shipmentsRequest.getOrderDetails().getBillingDetails()) {
				shipmentsRequest.getOrderDetails().getBillingDetails().setBillTo(referenceAccounts.get(0));
			} else {
				shipmentsRequest.getOrderDetails()
						.setBillingDetails(BillingInfo.builder().billToAccount(referenceAccounts.get(0)).build());
			}
		}
	}

    /**
     * Return the number of transit days for Lulu business, defined outside of Luluroute. This is the actual full number
     * of days including holidays and weekends (different from what Luluroute calls "transit days" everywhere else).
     */
    private static int calculateBusinessTransitDays(ShipmentArtifact shipmentArtifact) {
        TransitTimeInfo shipmentDatesInfo = shipmentArtifact.getArtifactBody().getCarrierExternalData().getTransitTimeInfo();
        return (int) ChronoUnit.DAYS.between(
                Instant.ofEpochSecond(shipmentDatesInfo.getShippedDate()),
                Instant.ofEpochSecond(shipmentDatesInfo.getPlannedDeliveryDate()))
                + 1; // ChronoUnit gives floor, use ceiling
    }

    private static List<RateShopResponse> getRatesInfo(ShipmentArtifact shipmentArtifact) {
        RateShop rateShop = shipmentArtifact.getArtifactBody().getRateShop();
        RouteRules routeRules = shipmentArtifact.getArtifactBody().getRouteRules();
        if (rateShop == null || rateShop.getRates() == null || rateShop.getRates().isEmpty()
                || routeRules == null || routeRules.getRuleResult() == null) {
            return List.of();
        }

        Rate selectedRate = findSelectedRate(rateShop.getRates(), routeRules.getRuleResult());
        if(selectedRate == null) {
            return List.of();
        }

        return List.of(buildResponseFromRate(selectedRate));
    }

    public static ShipmentMessage buildCreateShipmentFailure(ShipmentMessage shipmentRequest,ShipmentArtifact shipmentArtifact) {


        long CREATE_SHIPMENT_FAILURE_REQUEST_TYPE = 2000;
        long CREATE_SHIPMENT_FAILURE_RESPONSE_CODE = 9000;
        long CREATE_SHIPMENT_MESSAGE_STATUS = 1000;
        long date = Instant.now().getEpochSecond();

        // add extended list for any validation errors
        ProcessException processException = shipmentArtifact.getArtifactHeader().getProcesses().get(0).getProcessException();
        List<ItemInfo> extended = null;
        if (!CollectionUtils.isEmpty(shipmentArtifact.getArtifactHeader().getProcesses().get(0).getExtended())) {
            extended = ValidationUtil.mapList(shipmentArtifact.getArtifactHeader().getProcesses().get(0).getExtended(), ItemInfo.class);
        }

        RequestInfo.RequestInfoBuilder requestInfoBuilder = RequestInfo.builder();
        requestInfoBuilder
                .requestType(String.valueOf(CREATE_SHIPMENT_FAILURE_REQUEST_TYPE))
                .requestDate(date)
                .response(ResponseItem.builder()
                        .responseCode(String.valueOf(CREATE_SHIPMENT_FAILURE_RESPONSE_CODE))
                        .responseMessage(getErrorMessage(processException, shipmentArtifact.getArtifactBody().getRouteRules()))
                        .responseDate(date)
                        .extended(extended)
                        .build());

        StatusItem.StatusItemBuilder messageStatusBuilder = StatusItem.builder();
        messageStatusBuilder.status(CREATE_SHIPMENT_MESSAGE_STATUS)
                .statusDate(date);

        MessageHeaderInfo.MessageHeaderInfoBuilder messageHeaderBuilder = MessageHeaderInfo.builder();
        messageHeaderBuilder
                .messageCorrelationId(shipmentRequest.getMessageHeader().getMessageCorrelationId())
                .sequence(1)
                .totalSequence(1)
                .messageDate(date)
                .messageSources(shipmentRequest.getMessageHeader().getMessageSources());


        MessageBodyInfo.MessageBodyInfoBuilder messageBodyInfoBuilder = MessageBodyInfo.builder();
        ShipmentInfo shipmentsRequest = shipmentRequest.getMessageBody().getShipments().get(0);

        ShipmentInfo shipmentsResponse = ShipmentInfo.builder()
                .shipmentHeader(shipmentsRequest.getShipmentHeader())
                .transitDetails(shipmentsRequest.getTransitDetails())
                .orderDetails(shipmentsRequest.getOrderDetails())
                .shipmentStatus(shipmentsRequest.getShipmentStatus())
                .shipmentPieces(shipmentsRequest.getShipmentPieces())
                .build();

        messageBodyInfoBuilder.shipments(List.of(shipmentsResponse));

        return ShipmentMessage.builder().
                RequestHeader(requestInfoBuilder.build())
                .MessageStatus(messageStatusBuilder.build())
                .MessageHeader(messageHeaderBuilder.build())
                .MessageBody(messageBodyInfoBuilder.build())
                .build();


    }


    public static ShipmentMessage buildCancelShipmentSuccess(ShipmentArtifact shipmentArtifact) {


         long CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE = 9989;
         String CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE = "1000";
         String RESPONSE_MESSAGE_SUCCESS = "Success";
         long date = Instant.now().getEpochSecond();

         RequestInfo.RequestInfoBuilder requestInfoBuilder = RequestInfo.builder();
         requestInfoBuilder.requestType(String.valueOf(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE))
                 .requestDate(date)
                 .response(ResponseItem.builder()
                         .responseCode(CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE)
                         .responseMessage(RESPONSE_MESSAGE_SUCCESS)
                         .responseDate(date)
                         .build());

         StatusItem.StatusItemBuilder messageStatusBuilder = StatusItem.builder();
         messageStatusBuilder.status(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE)
                 .statusDate(date);

         MessageHeaderInfo.MessageHeaderInfoBuilder messageHeaderBuilder = MessageHeaderInfo.builder();
         messageHeaderBuilder
                 .messageCorrelationId(shipmentArtifact.getArtifactHeader().getMessageCorrelationId())
                 .sequence(1)
                 .totalSequence(1)
                 .messageDate(date);
         // if required this needs to be populated in ms-service as it has the request in DBs
//                 .messageSources(shipmentRequest.getMessageHeader().getMessageSources());


         MessageBodyInfo.MessageBodyInfoBuilder messageBodyInfoBuilder = MessageBodyInfo.builder();
//         ShipmentInfo shipmentsRequest = shipmentRequest.getMessageBody().getShipments().get(0);

         ShipmentInfo shipmentsResponse = ShipmentInfo.builder()
                 .shipmentHeader(ShipmentHeader.builder()
                         .shipmentCorrelationId(shipmentArtifact.getArtifactHeader().getShipmentCorrelationId())
                         .build())
                 .shipmentStatus(messageStatusBuilder.build())
                 .build();

         messageBodyInfoBuilder.shipments(List.of(shipmentsResponse));

         return ShipmentMessage.builder().
                 RequestHeader(requestInfoBuilder.build())
                 .MessageStatus(messageStatusBuilder.build())
                 .MessageHeader(messageHeaderBuilder.build())
                 .MessageBody(messageBodyInfoBuilder.build())
                 .build();


     }

    public static ShipmentMessage buildCancelShipmentFailure(ShipmentArtifact shipmentArtifact) {




            long CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE = 9989;
            String CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE = "9000";
            long date = Instant.now().getEpochSecond();

            RequestInfo.RequestInfoBuilder requestInfoBuilder = RequestInfo.builder();
        ProcessException processException = shipmentArtifact.getArtifactHeader().getProcesses().get(0).getProcessException();
        requestInfoBuilder.requestType(String.valueOf(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE))
                    .requestDate(date)
                    .response(ResponseItem.builder()
                            .responseCode(CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE)
                            .responseMessage(getErrorMessage(processException, null))
                            .responseDate(date)
                            .build());

            StatusItem.StatusItemBuilder messageStatusBuilder = StatusItem.builder();
            messageStatusBuilder.status(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE)
                    .statusDate(date);

            MessageHeaderInfo.MessageHeaderInfoBuilder messageHeaderBuilder = MessageHeaderInfo.builder();
            messageHeaderBuilder
                    .messageCorrelationId(shipmentArtifact.getArtifactHeader().getMessageCorrelationId())
                    .sequence(1)
                    .totalSequence(1)
                    .messageDate(date);
        // if required this needs to be populated in ms-service as it has the request in DBs
//                    .messageSources(shipmentRequest.getMessageHeader().getMessageSources());


            MessageBodyInfo.MessageBodyInfoBuilder messageBodyInfoBuilder = MessageBodyInfo.builder();
//            ShipmentInfo shipmentsRequest = shipmentRequest.getMessageBody().getShipments().get(0);

            ShipmentInfo shipmentsResponse = ShipmentInfo.builder()
                    .shipmentHeader(ShipmentHeader.builder()
                            .shipmentCorrelationId(shipmentArtifact.getArtifactHeader().getShipmentCorrelationId())
                            .build())
                    .shipmentStatus(messageStatusBuilder.build())
                    .build();

            messageBodyInfoBuilder.shipments(List.of(shipmentsResponse));


        return ShipmentMessage.builder().
                RequestHeader(requestInfoBuilder.build())
                .MessageStatus(messageStatusBuilder.build())
                .MessageHeader(messageHeaderBuilder.build())
                .MessageBody(messageBodyInfoBuilder.build())
                .build();



    }

    public static ShipmentMessage buildFailureResponse(ShipmentArtifact shipmentArtifact) {
        long CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE = 9989;
        String CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE = "9000";
        String RESPONSE_MESSAGE_FAILURE = "Shipment Processing failed";
        long date = Instant.now().getEpochSecond();

        RequestInfo.RequestInfoBuilder requestInfoBuilder = RequestInfo.builder();
        requestInfoBuilder.requestType(String.valueOf(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE))
                .requestDate(date)
                .response(ResponseItem.builder()
                        .responseCode(CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE)
                        .responseMessage(RESPONSE_MESSAGE_FAILURE)
                        .responseDate(date)
                        .build());

        StatusItem.StatusItemBuilder messageStatusBuilder = StatusItem.builder();
        messageStatusBuilder.status(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE)
                .statusDate(date);

        MessageHeaderInfo.MessageHeaderInfoBuilder messageHeaderBuilder = MessageHeaderInfo.builder();
        messageHeaderBuilder
                .messageCorrelationId(shipmentArtifact.getArtifactHeader().getMessageCorrelationId())
                .sequence(1)
                .totalSequence(1)
                .messageDate(date);


        MessageBodyInfo.MessageBodyInfoBuilder messageBodyInfoBuilder = MessageBodyInfo.builder();

        ShipmentInfo shipmentsResponse = ShipmentInfo.builder()
                .shipmentHeader(ShipmentHeader.builder()
                        .shipmentCorrelationId(shipmentArtifact.getArtifactHeader().getMessageCorrelationId())
                        .build())
                .shipmentStatus(messageStatusBuilder.build())
                .build();

        messageBodyInfoBuilder.shipments(List.of(shipmentsResponse));


        return ShipmentMessage.builder().
                RequestHeader(requestInfoBuilder.build())
                .MessageStatus(messageStatusBuilder.build())
                .MessageHeader(messageHeaderBuilder.build())
                .MessageBody(messageBodyInfoBuilder.build())
                .build();
    }

    static String getErrorMessage(ProcessException processException, RouteRules routeRules){
        StringBuilder errorMessage = new StringBuilder();
        if (processException != null && !StringUtils.isEmpty(processException.getDescription())) {
            errorMessage.append(processException.getDescription());
        }
        if (routeRules != null && routeRules.getRulesError() != null && !routeRules.getRulesError().isEmpty()) {
            errorMessage.append(" ||| Error details by mode: ");
            errorMessage.append(buildErrorMessageFromRulesError(routeRules.getRulesError()));
        }
        if( ObjectUtils.isNotEmpty(errorMessage)) {
            return errorMessage.toString();
        }
        return RESPONSE_MESSAGE_FAILURE;
    }

    private static String buildErrorMessageFromRulesError(List<RulesError> rulesErrors) {
        StringBuilder description = new StringBuilder();
        // Add any exception
        rulesErrors.forEach(rulesError ->
                description.append(String.format(PROCESS_EXCEPTION_DETAIL,
                        rulesError.getTargetCarrierCode(),
                        rulesError.getTargetCarrierModeCode(),
                        rulesError.getProcessException().getDescription())));

        return description.toString();
    }


    private static Rate findSelectedRate(List<Rate> rates, RuleResult selectedRule) {
        return rates.stream()
                .filter(rate -> StringUtils.equalsIgnoreCase(rate.getCarrierCode(), selectedRule.getTargetCarrierCode())
                        && StringUtils.equalsIgnoreCase(rate.getMode(), selectedRule.getTargetCarrierModeCode()))
                .findAny()
                .orElse(null);
    }

    private static RateShopResponse buildResponseFromRate(Rate rate) {
        return RateShopResponse.builder()
                .carrierCode(rate.getCarrierCode())
                .modeCode(rate.getMode())
                .cost(rate.getBaseCost())
                .additionalCost(rate.getAdcCost())
                .responseMessage(rate.getNotes())
                .build();
    }
}
