package com.middleware.processor.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
    
    @Value("${rabbitmq.queue.inbound.processor}")
    private String inboundProcessorQueue;
    
    @Value("${rabbitmq.prefetch.count:30}")
    private int prefetchCount;
    
    @Value("${rabbitmq.concurrent.consumers:3}")
    private int concurrentConsumers;
    
    @Value("${rabbitmq.max.concurrent.consumers:10}")
    private int maxConcurrentConsumers;
    
    @Value("${rabbitmq.thread.pool.size:20}")
    private int threadPoolSize;
    
    @Bean
    public Queue inboundProcessorQueue() {
        return new Queue(inboundProcessorQueue, true); // durable queue
    }
    
    @Bean
    public Queue highPriorityQueue() {
        return QueueBuilder.durable("client.high.priority")
            .withArgument("x-max-priority", 10)
            .build();
    }
    
    @Bean
    public Queue normalPriorityQueue() {
        return QueueBuilder.durable("client.normal.priority")
            .withArgument("x-max-priority", 10)
            .build();
    }
    
    @Bean
    public Queue lowPriorityQueue() {
        return QueueBuilder.durable("client.low.priority")
            .withArgument("x-max-priority", 10)
            .build();
    }
    
    @Bean
    public DirectExchange priorityExchange() {
        return new DirectExchange("middleware.priority", true, false);
    }
    
    @Bean
    public Binding highPriorityBinding() {
        return BindingBuilder.bind(highPriorityQueue())
            .to(priorityExchange())
            .with("high");
    }
    
    @Bean
    public Binding normalPriorityBinding() {
        return BindingBuilder.bind(normalPriorityQueue())
            .to(priorityExchange())
            .with("normal");
    }
    
    @Bean
    public Binding lowPriorityBinding() {
        return BindingBuilder.bind(lowPriorityQueue())
            .to(priorityExchange())
            .with("low");
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
