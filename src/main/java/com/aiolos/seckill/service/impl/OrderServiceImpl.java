package com.aiolos.seckill.service.impl;

import com.aiolos.seckill.dao.OrderDOMapper;
import com.aiolos.seckill.dao.SequenceDOMapper;
import com.aiolos.seckill.dao.StockLogDOMapper;
import com.aiolos.seckill.dataobject.OrderDO;
import com.aiolos.seckill.dataobject.SequenceDO;
import com.aiolos.seckill.dataobject.StockLogDO;
import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.model.ItemModel;
import com.aiolos.seckill.model.OrderModel;
import com.aiolos.seckill.service.IItemService;
import com.aiolos.seckill.service.IOrderService;
import com.aiolos.seckill.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Aiolos
 * @date 2019-06-16 20:38
 */
@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private IItemService itemService;

    @Autowired
    private IUserService userService;

    @Autowired
    private OrderDOMapper orderDOMapper;

    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException {

        // 1.检验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");

//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if (userModel == null)
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");

        if (amount <= 0 || amount > 99)
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不正确");

        // 校验活动信息
//        if (promoId != null) {
//            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
//                // （1）校验对应活动是否存在这个适用商品
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
//            } else if (itemModel.getPromoModel().getStatus().intValue() != 2) {
//                // （2）校验活动是否正在进行中
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动还未开始");
//            }
//        }

        // 2.落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result)
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);

        // 3.订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setPromoId(promoId);
        orderModel.setAmount(amount);

        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }

        orderModel.setItemPrice(itemModel.getPrice());
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        // 4.生成订单号
        orderModel.setId(generateOrderNo());

        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        // 5.增加商品的销量
        itemService.increaseSales(itemId, amount);

        // 6.设置库存流水状态为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO == null) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//
//            // 在最近一个Transactional标签commit之后执行
//            @Override
//            public void afterCommit() {
//                // 6.异步更新库存
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
////                if (!mqResult) {
////                    itemService.increaseStock(itemId, amount);
////                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });

        return orderModel;
    }

    private OrderDO convertFromOrderModel(OrderModel orderModel) {

        if (orderModel == null)
            return null;

        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }

    /**
     * 生成订单号
     * REQUIRES_NEW : 开启一个新的事物，该代码块执行完直接提交该事物
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo() {

        StringBuilder sb = new StringBuilder();

        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        sb.append(nowDate);

        // 获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);

        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            sb.append(0);
        }

        sb.append(sequenceStr);
        sb.append("00");

        return sb.toString();
    }
}
