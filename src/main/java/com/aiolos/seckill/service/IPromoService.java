package com.aiolos.seckill.service;

import com.aiolos.seckill.model.PromoModel;

/**
 * @author Aiolos
 * @date 2019-06-16 23:07
 */
public interface IPromoService {

    PromoModel getPromoByItemId(Integer itemId);
}
