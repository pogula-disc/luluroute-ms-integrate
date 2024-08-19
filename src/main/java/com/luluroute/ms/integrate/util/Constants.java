package com.luluroute.ms.integrate.util;

public interface Constants {

    String V1 = "v1";
    String X_CORRELATION_ID = "X-Correlation-Id";
    String X_JOB_START_TIME = "X-Job-Start-Time";
    String X_SHIPMENT_CORRELATION_ID = "X-Shipment-Correlation-Id";
    String X_MESSAGE_CORRELATION_ID = "X-Message-Correlation-Id";
    String X_TRANSACTION_REFERENCE = "X-Transaction-Reference";
    String MESSAGE_NO_ARTIFACTS = "APP_MESSAGE=No ShipmentArtifacts(5100,2100) streamed for carrier {%s} in last {%s} sec";
    String SHIPMENT_REQUEST_CANCEL = "9989";
    String SHIPMENT_REQUEST_RELEASE = "2000";
    String BUILD_ARTIFACT_ROUTE_RULES = "5100";
    String BUILD_ARTIFACT_LABEL_RENDERED = "7900";
    String BUILD_ARTIFACT_CANCEL = "9100";
    String BUILD_ARTIFACT_LABEL_REQUIRE_RENDER = "7100";
    String BUILD_ARTIFACT_LABEL_DND_REQUIRE_RENDER = "7200";
    String MESSAGE_PUBLISHED = "APP_MESSAGE=Message Published | Key=\"{}\" | Message=\"{}\" | Topic=\"{}\"";
    String SCHEMA_REGISTRY_URL = "schema.registry.url";
    String CONSUMER_STREAM_CONSUMER_GROUP = "%s_%s";
    String MESSAGE_REDIS_KEY_LOADING = "APP_MESSAGE=\"Loading %s for key %s from Redis";

    String LABEL_PROCESS_NAME = "Shipment_Artifact_Label";
    long LABEL_PROCESS_CODE = 200;
    long PROCESS_STATUS_COMPLETED = 200;
    long PROCESS_STATUS_FAILED = 500;

    String FROM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    String STANDARD_ERROR = "APP_MESSAGE=Error Occurred | METHOD=\"{}\" | ERROR=\"{}\"";
    String STANDARD_FIELD_INFO = "APP_MESSAGE=Field Identifier# \"{%s}\" | Value # \"{%s}\" ";
    long STATUS_SUCCESS = 200;
    long STATUS_ERROR = 500;
    String STREAM_MESSAGE = "APP_MESSAGE=\"Stream %s \" | key=\"{%s}\"  | value=\"{%s}\" | ShipmentCorrelationId=\"{%s}\"";
    String PROFILE_MESSAGE = "APP_MESSAGE=\"Profile %s \" | key=\"{%s}\"  | value=\"{%s}\" | ShipmentCorrelationId=\"{%s}\"";
    String PROCESSING_SHIPMENT_REQUEST = "APP_MESSAGE=\"Processing starting for ShipmentCorrelationId {%s} Message\" | message=\"{%s}\"";

    String SHIPMENT_RESPONSE_SUCCESS = "1000";
    String SHIPMENT_RESPONSE_FAILED = "9000";
    String CARRIER_CANCEL_ACCEPTED = "ACCEPTED";
    String PROCESS_EXCEPTION_DETAIL = "%s-%s: %s ||| ";
    String RESPONSE_MESSAGE_FAILURE = "Failed: Cancel Shipment Failure";
    String RETAIL = "RETAIL";
    String CANADAPOST_CARRIERNAME = "CNP" ;
}
