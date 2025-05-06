package com.middleware.processor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.prefetch.count:30}")
    private int prefetchCount;
    
    @Value("${rabbitmq.concurrent.consumers:3}")
    private int concurrentConsumers;
    
    @Value("${rabbitmq.max.concurrent.consumers:10}")
    private int maxConcurrentConsumers;
    
    @Value("${rabbitmq.thread.pool.size:20}")
    private int threadPoolSize;
    
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
    
    // ASN Queues
    @Bean
    public Queue asnHighPriorityQueue() {
        return QueueBuilder.durable("inbound.processor.asn.high")
            .withArgument("x-message-ttl", 86400000)
            .withArgument("x-max-priority", 10)
            .withArgument("x-dead-letter-exchange", "middleware.dlx")
            .withArgument("x-dead-letter-routing-key", "inbound.processor.asn.high.dlq")
            .withArgument("x-max-length", 10000)
            .withArgument("x-max-length-bytes", 104857600)
            .withArgument("x-overflow", "reject-publish")
            .withArgument("x-queue-mode", "lazy")
            .build();
    }
    
    // ORDER Queues
    @Bean
    public Queue orderHighPriorityQueue() {
        return QueueBuilder.durable("inbound.processor.order.high")
            .withArgument("x-message-ttl", 86400000)
            .withArgument("x-max-priority", 10)
            .withArgument("x-dead-letter-exchange", "middleware.dlx")
            .withArgument("x-dead-letter-routing-key", "inbound.processor.order.high.dlq")
            .withArgument("x-max-length", 10000)
            .withArgument("x-max-length-bytes", 104857600)
            .withArgument("x-overflow", "reject-publish")
            .withArgument("x-queue-mode", "lazy")
            .build();
    }
    
    @Bean
    public DirectExchange middlewareDirectExchange() {
        return new DirectExchange("middleware.direct", true, false);
    }
    
    // ASN Bindings
    @Bean
    public Binding asnHighPriorityBinding() {
        return BindingBuilder.bind(asnHighPriorityQueue())
            .to(middlewareDirectExchange())
            .with("inbound.processor.asn.high");
    }
    
    // ORDER Bindings
    @Bean
    public Binding orderHighPriorityBinding() {
        return BindingBuilder.bind(orderHighPriorityQueue())
            .to(middlewareDirectExchange())
            .with("inbound.processor.order.high");
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("processor-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setPrefetchCount(prefetchCount);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        factory.setFailedDeclarationRetryInterval(5000L);
        factory.setTaskExecutor(taskExecutor());
        return factory;
    }
} 
