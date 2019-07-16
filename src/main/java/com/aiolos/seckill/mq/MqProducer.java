package com.aiolos.seckill.mq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Aiolos
 * @date 2019-07-17 00:12
 */
@Component
public class MqProducer {

    private DefaultMQProducer producer;

    @Value("mq.nameserver.addr")
    private String nameAddr;

    @Value("mq.topicname")
    private String topicName;

    @PostConstruct
    public void init() throws MQClientException {

        // mq producer初始化
        // producer的group name在所有的操作当中是没有影响的
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();
    }

    /**
     * 同步库存扣减消息
     * @param itemId
     * @param amount
     * @return
     */
    public SendResult asyncReduceStock(Integer itemId, Integer amount) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);

        Message message = new Message(topicName, "increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        return producer.send(message);
    }
}
