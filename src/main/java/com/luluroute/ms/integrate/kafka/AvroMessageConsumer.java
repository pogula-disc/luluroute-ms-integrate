package com.luluroute.ms.integrate.kafka;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.lululemon.luluroute.common.exception.PermanentException;
import com.lululemon.luluroute.common.exception.TransientException;
import com.lululemon.luluroute.common.kafka.AbstractTopicConsumer;
import com.lululemon.luluroute.common.kafka.EventHeaders;
import com.luluroute.ms.integrate.config.AppConfig;
import com.luluroute.ms.integrate.service.ShipmentIntegrateService;
import com.luluroute.ms.integrate.util.ObjectMapperUtil;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static com.luluroute.ms.integrate.util.Constants.*;

/**
 * Retrying Topic Consumer
 * <br>Thread 1: Main Topic Consumer
 * <br>Thread 2: Retry Topic Consumer
 * <br>Thread 3: Resume Task
 * <br>Note: with 3 Threads running in this file, any shared state must be
 * synchronized
 *
 * @author MANDALAKARTHIK1
 *
 */
@Slf4j
@Component
public class AvroMessageConsumer extends AbstractTopicConsumer<ShipmentArtifact> {

	@Autowired
	private ShipmentIntegrateService shipmentIntegrateService;
	@Autowired
	private Counter successCounter;

	@Autowired
	private MessageFilter messageFilter;

	public AvroMessageConsumer(AppConfig appConfig) {
		super(appConfig.getShipmentArtifactInputChannel());
	}

	/**
	 * Thread 1 - Main Topic Consumer
	 *
	 * @param shipmentArtifact - Message Payload
	 * @param headers - Headers as a Map
	 * @param ack     - Manually control offset commit
	 */
	@Override
	@KafkaListener(id = "${config.shipment.artifact.core.topic}", topics = "${config.shipment.artifact.core.topic}", groupId = "${config.shipment.artifact.consumerGroup}"
			,concurrency ="${config.shipment.artifact.corepoolsize}" )
	public void consumeMessage(@Payload ShipmentArtifact shipmentArtifact, @Headers MessageHeaders headers, Acknowledgment ack) {
		if (messageFilter.shipmentArtifactFilter.test(shipmentArtifact)) {
			super.consumeMessage(shipmentArtifact, headers, ack);
		}
		else{
			ack.acknowledge();
		}
	}

	@Override
	protected void processMessageImpl(ShipmentArtifact shipmentArtifact, EventHeaders headers)
			throws PermanentException, TransientException {
		log.info("APP_MESSAGE=\"Received AVRO message\" | message=\"{}\"", shipmentArtifact);
		try {
			com.logistics.luluroute.domain.artifact.message.ShipmentArtifact artifact =
					ObjectMapperUtil.map(shipmentArtifact, com.logistics.luluroute.domain.artifact.message.ShipmentArtifact.class);
			shipmentIntegrateService.processArtifactMessageExecutor(artifact);
			successCounter.increment();
		} finally {
			MDC.clear();
		}
		log.info("APP_MESSAGE=\"Received AVRO message and processed successfully\"");

	}

	@Override
	public void addLoggingContext(ShipmentArtifact shipmentArtifact, MessageHeaders headers) {
		if(shipmentArtifact != null && shipmentArtifact.getArtifactHeader() != null) {
			MDC.put(X_CORRELATION_ID, String.valueOf(shipmentArtifact.getArtifactHeader().getMessageCorrelationId()));
			MDC.put(X_SHIPMENT_CORRELATION_ID, String.valueOf(shipmentArtifact.getArtifactHeader().getShipmentCorrelationId()));
		}
	}

	@Override
	public void removeLoggingContext() {
		MDC.remove(X_CORRELATION_ID);
		MDC.remove(X_SHIPMENT_CORRELATION_ID);
		MDC.remove(X_TRANSACTION_REFERENCE);
	}

}