package com.aiolos.seckill.service.impl;

import com.aiolos.seckill.dao.PromoDOMapper;
import com.aiolos.seckill.dataobject.PromoDO;
import com.aiolos.seckill.model.ItemModel;
import com.aiolos.seckill.model.PromoModel;
import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.service.IItemService;
import com.aiolos.seckill.service.IPromoService;
import com.aiolos.seckill.service.IUserService;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Aiolos
 * @date 2019-06-16 23:08
 */
@Service
public class PromoServiceImpl implements IPromoService {

    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private IItemService itemService;

    @Autowired
    private IUserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

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

    @Override
    public void publishPromo(Integer promoId) {

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO == null || promoDO.getId().intValue() == 0) {
            return;
        }

        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        // 将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());

        // 将大闸的限制数量设到redis内
        redisTemplate.opsForValue().set("promo_door_count_" + promoId, itemModel.getStock().intValue());
    }

    @Override
    public String generateSeckillToken(Integer promoId, Integer itemId, Integer userId) {

        // 判断库存是否已售罄
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
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

        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null)
            return null;

        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null)

        if (promoModel.getStatus().intValue() != 2)
            return null;

        // 获取秒杀大闸count
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);
        if (result < 0) {
            return null;
        }

        String promoToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId, promoToken);
        redisTemplate.expire("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId, 5, TimeUnit.MINUTES);
        return promoToken;
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
