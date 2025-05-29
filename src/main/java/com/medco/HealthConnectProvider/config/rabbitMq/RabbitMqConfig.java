//package com.medco.HealthConnectProvider.config.rabbitMq;
//
//import org.springframework.amqp.core.*;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitMqConfig {
//
//    @Value("${rabbitmq.services.queue}")
//    private String serviceQueue;
//
//    @Value("${rabbitmq.services.exchange}")
//    private String serviceExchange;
//
//    @Value("${rabbitmq.services.routingkey}")
//    private String serviceKey;
//
//    @Bean
//    public Queue servicesQueue() {
//        return new Queue("serviceQueue", true);
//    }
//
//    @Bean
//    public DirectExchange exchange() {
//        return new DirectExchange("serviceExchange");
//    }
//
//    @Bean
//    public Binding servicesBinding(Queue servicesQueue, DirectExchange servicesExchange) {
//        return BindingBuilder.bind(servicesQueue).to(servicesExchange).with("serviceKey");
//    }
//
//    @Bean
//    public MessageConverter jsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//
//    @Bean
//    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
//        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
//        template.setMessageConverter(jsonMessageConverter());
//        return template;
//    }
//}
