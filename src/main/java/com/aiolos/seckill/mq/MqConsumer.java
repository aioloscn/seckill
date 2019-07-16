package com.aiolos.seckill.mq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author Aiolos
 * @date 2019-07-17 00:11
 */
@Component
public class MqConsumer {

    private DefaultMQPushConsumer consumer;

    @Value("mq.nameserver.addr")
    private String nameAddr;

    @Value("mq.topicname")
    private String topicName;

    @PostConstruct
    public void init() throws MQClientException {

        consumer = new DefaultMQPushConsumer("stock_consumer_group");
        consumer.setNamesrvAddr(nameAddr);
        consumer.subscribe(topicName, "*"); // 订阅所有的消息

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
    }
}
