package com.jerio.miaosha.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by Jerio on 2018/9/1
 */
@Configuration
public class MQConfig {
    public static final String MIAOSHA_QUEUE = "miaosha.queue";


    @Bean
    public Queue miaoshaQueue(){
        return new Queue(MQConfig.MIAOSHA_QUEUE);
    }
}
