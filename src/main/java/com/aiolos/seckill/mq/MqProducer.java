package com.aiolos.seckill.mq;

import com.aiolos.seckill.dao.StockLogDOMapper;
import com.aiolos.seckill.dataobject.StockLogDO;
import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.service.IOrderService;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
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

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @PostConstruct
    public void init() throws MQClientException {

        // mq producer初始化
        // producer的group name在所有的操作当中是没有影响的
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {

            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {

                // 真正要处理的业务 创建订单
                Integer itemId = Integer.parseInt(((Map) o).get("itemId").toString());
                Integer amount = Integer.parseInt(((Map) o).get("amount").toString());
                Integer userId = Integer.parseInt(((Map) o).get("userId").toString());
                Integer promoId = Integer.parseInt(((Map) o).get("promoId").toString());
                String stockLogId = ((Map) o).get("stockLogId").toString();

                try {
                    orderService.createOrder(userId, itemId, promoId, amount, stockLogId);
                } catch (BusinessException e) {

                    e.printStackTrace();
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {

                // 根据是否扣减库存成功，来返回COMMIT,ROLLBACK还是继续UNKNOWN
                Map<String, Object> map = JSON.parseObject(messageExt.getBody().toString(), Map.class);
                Integer itemId = Integer.parseInt(map.get("itemId").toString());
                Integer amount = Integer.parseInt(map.get("amount").toString());
                String stockLogId = map.get("stockLogId").toString();
                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);

                if (stockLogId == null) {
                    return LocalTransactionState.UNKNOW;
                }

                if (stockLogDO.getStatus().intValue() == 2) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else if (stockLogDO.getStatus().intValue() == 1) {
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        });
    }


    /**
     * 事务型异步库存扣减消息
     * @param itemId
     * @param amount
     * @return
     */
    public boolean transactionAsyncReduceStock(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) {

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("itemId", itemId);
        argsMap.put("amount", amount);
        argsMap.put("userId", userId);
        argsMap.put("promoId", promoId);
        argsMap.put("stockLogId", stockLogId);

        Message message = new Message(topicName, "increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult sendResult = null;

        try {
            // 发送事务型消息，状态为prepare状态，不会被消费者看到，在prepare状态下会去client端执行executeLocalTransaction方法
            sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }

        if (null != sendResult && sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE)
            return true;
        else if (null != sendResult && sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE)
            return false;
        else
            return false;
    }

    /**
     * 异步库存扣减消息
     * @param itemId
     * @param amount
     * @return
     */
    public boolean asyncReduceStock(Integer itemId, Integer amount) {

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);

        Message message = new Message(topicName, "increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);
        } catch (MQClientException e) {
            return false;
        } catch (RemotingException e) {
            return false;
        } catch (MQBrokerException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }
}
