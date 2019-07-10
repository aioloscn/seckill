package com.aiolos.seckill.service;

import com.aiolos.seckill.model.PromoModel;

/**
 * @author Aiolos
 * @date 2019-06-16 23:07
 */
public interface IPromoService {

    /**
     * 根据item id获取即将进行的或正在进行的秒杀活动
     * @param itemId
     * @return
     */
    PromoModel getPromoByItemId(Integer itemId);

    /**
     * 活动发布
     * @param promoId
     */
    void publishPromo(Integer promoId);
}
