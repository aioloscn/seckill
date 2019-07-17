package com.aiolos.seckill.service;

import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.model.OrderModel;

/**
 * @author Aiolos
 * @date 2019-06-16 20:37
 */
public interface IOrderService {

    /**
     * 通过前端url上传过来秒杀活动id，然后下单接口内校验对应id是否属于对应商品且活动已开始
     * @param userId
     * @param itemId
     * @param promoId
     * @param amount
     * @return
     * @throws BusinessException
     */
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException;
}
