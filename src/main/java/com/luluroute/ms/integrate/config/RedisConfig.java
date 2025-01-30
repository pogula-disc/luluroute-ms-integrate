package com.luluroute.ms.integrate.config;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import io.lettuce.core.ReadFrom;

import java.io.Serializable;
import java.time.Duration;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import org.springframework.util.StringUtils;


/**
 * 
 * @author SathishRaghupathy
 *
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

	@Value("${shipmentMessage.redis.host}")
	private String redisHost;

	@Value("${shipmentMessage.redis.port}")
	private int redisPort;

	@Value("${shipmentMessage.redis.requestCache.keyPrefix}")
	public String keyPrefix;

	@Value("${shipmentMessage.redis.responseCache.keyPrefix}")
	public String shipmentResponseKeyPrefix;
	@Value("${shipmentMessage.redis.cancelResponseCache.keyPrefix}")
	public String shipmentCancelResponseKeyPrefix;


	@Value("${shipmentMessage.redis.ttlInMilliseconds}")
	public long cacheClearTime;

	@Value("${spring.redis.auth-token:}")
	private String redisAuthToken;

	@Value("${spring.redis.use-secure-connection:false}")
	private boolean useSecureConnection;

	@Value("${spring.redis.command-timeout-seconds:10}")
	private int commandTimeoutSeconds;

	@Value("${spring.redis.max-redirects:3}")
	private int maxRedirects;

	@Primary
	@Bean
	@ConditionalOnProperty(prefix = "config.redis.cluster", name = "enabled", havingValue = "true")
	public LettuceConnectionFactory redisConnectionFactory() {
		log.info("Initializing Redis cluster connection with host: {} and port: {}", redisHost, redisPort);
		RedisClusterConfiguration redisConfiguration = redisClusterConfiguration();
		LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder()
				.readFrom(ReadFrom.REPLICA_PREFERRED)
				.commandTimeout(Duration.ofSeconds(commandTimeoutSeconds));

		if (useSecureConnection) {
			clientConfigBuilder.useSsl();
		}
		LettuceClientConfiguration clientConfig = clientConfigBuilder.build();
		log.info("Redis cluster SSL connection is {}", useSecureConnection ? "enabled" : "disabled");
		return new LettuceConnectionFactory(redisConfiguration, clientConfig);
	}

	@Bean
	@ConditionalOnProperty(prefix = "config.redis.cluster", name = "enabled", havingValue = "true")
	public RedisClusterConfiguration redisClusterConfiguration() {
		String hostAndPort = redisHost.contains(":") ? redisHost : String.format("%s:%d", redisHost, redisPort);
		List<String> nodes = List.of(hostAndPort);

		RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(nodes);
		redisClusterConfiguration.setMaxRedirects(maxRedirects);

		if (useSecureConnection) {
			if (StringUtils.hasText(redisAuthToken)) {
				redisClusterConfiguration.setPassword(redisAuthToken);
				log.info("Using Redis AUTH token for secure cluster connection.");
			} else {
				throw new IllegalArgumentException("Redis Auth Token is required for secure connection.");
			}
		} else {
			if (StringUtils.hasText(redisAuthToken)) {
				log.warn("Redis AUTH token provided for an unsecured connection. This is a potential security risk.");
			} else {
				log.info("No Redis AUTH token provided. Connecting to an unsecured Redis instance.");
			}
		}
		return redisClusterConfiguration;
	}

	@Bean
	@ConditionalOnProperty(prefix = "config.redis.cluster", name = "enabled", havingValue = "false", matchIfMissing = true)
	public LettuceConnectionFactory standaloneRedisConnectionFactory() {
		String host = redisHost.contains(":") ? redisHost.split(":")[0] : redisHost;
		int port = redisHost.contains(":") ? Integer.parseInt(redisHost.split(":")[1]) : redisPort;

		log.info("Initializing standalone Redis connection with host: {} and port: {}", host, port);
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);

		if (useSecureConnection) {
			if (StringUtils.hasText(redisAuthToken)) {
				redisStandaloneConfiguration.setPassword(redisAuthToken);
				log.info("Using Redis AUTH token for secure standalone connection.");
			} else {
				throw new IllegalArgumentException("Redis Auth Token is required for secure connection.");
			}
		} else {
			if (StringUtils.hasText(redisAuthToken)) {
				log.warn("Redis AUTH token provided for an unsecured connection. This is a potential security risk.");
			} else {
				log.info("No Redis AUTH token provided, connecting to an unsecured Redis instance.");
			}
		}
		LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder()
				.commandTimeout(Duration.ofSeconds(commandTimeoutSeconds));
		if (useSecureConnection) {
			clientConfigBuilder.useSsl();
		}
		LettuceClientConfiguration clientConfig = clientConfigBuilder.build();
		log.info("Standalone Redis SSL connection is {}", useSecureConnection ? "enabled" : "disabled");
		return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
	}

	@Bean
	public ReactiveRedisOperations<String, ShipmentMessage> shipmentMessageTemplate(LettuceConnectionFactory redisConnectionFactory) {
		RedisSerializer<ShipmentMessage> valueSerializer = new Jackson2JsonRedisSerializer<>(ShipmentMessage.class);
		RedisSerializationContext<String, ShipmentMessage> serializationContext = RedisSerializationContext.<String, ShipmentMessage>newSerializationContext(RedisSerializer.string())
				.value(valueSerializer)
				.build();

		return new ReactiveRedisTemplate<>(redisConnectionFactory, serializationContext);
	}

	@Bean
	public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Serializable> template = new RedisTemplate<>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}

	@Bean
	public CacheManager cacheManager(LettuceConnectionFactory redisConnectionFactory) {
		RedisSerializationContext.SerializationPair<Object> jsonSerializer = RedisSerializationContext.SerializationPair
				.fromSerializer(new GenericJackson2JsonRedisSerializer());
		return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
				.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(cacheClearTime))
						.serializeValuesWith(jsonSerializer))
				.build();

	}

}
