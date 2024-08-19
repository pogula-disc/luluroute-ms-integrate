package com.luluroute.ms.integrate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
public class SpringAsyncConfig {

    @Value("${config.async.integrate.corepoolsize}")
    private int integrateCorePoolSize;

    @Value("${config.async.integrate.maxpoolsize}")
    private int integrateMaxPoolSize;

    @Bean(name = "AsyncTaskExecutor")
    public Executor createShipmentTaskExecutor() {
        return new ThreadPoolExecutor(integrateCorePoolSize, integrateMaxPoolSize, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

    }
}
