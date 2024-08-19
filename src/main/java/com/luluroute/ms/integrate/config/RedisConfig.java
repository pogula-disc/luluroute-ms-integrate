package com.luluroute.ms.integrate.config;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;

import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import io.lettuce.core.ReadFrom;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;


/**
 * 
 * @author SathishRaghupathy
 *
 */
@Configuration
@EnableCaching
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

	@Bean
	@ConditionalOnProperty(prefix = "config.redis.cluster", name = "enabled", havingValue = "true")
	LettuceConnectionFactory redisConnectionFactory(RedisClusterConfiguration redisConfiguration) {
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
				.readFrom(ReadFrom.REPLICA_PREFERRED).build();
		return new LettuceConnectionFactory(redisConfiguration, clientConfig);
	}

	@Bean
	@ConditionalOnProperty(prefix = "config.redis.cluster", name = "enabled", havingValue = "true")
	RedisClusterConfiguration redisConfiguration() {
		List<String> list = new ArrayList<>();
		list.add(redisHost);
		RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(list);
		redisClusterConfiguration.setMaxRedirects(3);
		return redisClusterConfiguration;
	}

	@Bean
	@ConditionalOnProperty(prefix = "config.redis.cluster", name = "enabled", havingValue = "false")
	public LettuceConnectionFactory lettuceConnectionFactory(){
		RedisStandaloneConfiguration redisStandaloneConfiguration =
				new RedisStandaloneConfiguration(redisHost, redisPort);
		return new LettuceConnectionFactory(redisStandaloneConfiguration);
	}

	@Bean
	public ReactiveRedisOperations<String, ShipmentMessage> shipmentMessageTemplate(LettuceConnectionFactory lettuceConnectionFactory){
		RedisSerializer<ShipmentMessage> valueSerializer = new Jackson2JsonRedisSerializer<>(ShipmentMessage.class);
		RedisSerializationContext<String, ShipmentMessage> serializationContext = RedisSerializationContext.<String, ShipmentMessage>newSerializationContext(RedisSerializer.string())
				.value(valueSerializer)
				.build();

		return new ReactiveRedisTemplate<>(lettuceConnectionFactory, serializationContext);
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
	public CacheManager cacheManager(RedisConnectionFactory factory) {
		RedisSerializationContext.SerializationPair<Object> jsonSerializer = RedisSerializationContext.SerializationPair
				.fromSerializer(new GenericJackson2JsonRedisSerializer());
		return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(factory)
				.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(cacheClearTime))
						.serializeValuesWith(jsonSerializer))
				.build();

	}

}
