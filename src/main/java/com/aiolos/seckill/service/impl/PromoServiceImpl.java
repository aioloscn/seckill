package com.aiolos.seckill.service.impl;

import com.aiolos.seckill.dao.PromoDOMapper;
import com.aiolos.seckill.dataobject.PromoDO;
import com.aiolos.seckill.model.PromoModel;
import com.aiolos.seckill.service.IPromoService;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

/**
 * @author Aiolos
 * @date 2019-06-16 23:08
 */
public class PromoServiceImpl implements IPromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {

        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        PromoModel promoModel = convertFromDataObject(promoDO);

        if (promoModel == null)
            return null;

        // 判断当前的时间是否秒杀活动即将开始或正在进行
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }

        return promoModel;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO) {

        if (promoDO == null)
            return null;

        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartTime(new DateTime(promoDO.getStartDate()));
        promoModel.setEndTime(new DateTime(promoDO.getEndDate()));

        return promoModel;
    }
}
