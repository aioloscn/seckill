package com.aiolos.seckill.service.impl;

import com.aiolos.seckill.dao.ItemDOMapper;
import com.aiolos.seckill.dao.ItemStockDOMapper;
import com.aiolos.seckill.dao.StockLogDOMapper;
import com.aiolos.seckill.dataobject.ItemDO;
import com.aiolos.seckill.dataobject.ItemStockDO;
import com.aiolos.seckill.dataobject.StockLogDO;
import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.model.ItemModel;
import com.aiolos.seckill.model.PromoModel;
import com.aiolos.seckill.mq.MqProducer;
import com.aiolos.seckill.service.IItemService;
import com.aiolos.seckill.service.IPromoService;
import com.aiolos.seckill.validator.ValidationResult;
import com.aiolos.seckill.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Aiolos
 * @date 2019-06-16 17:42
 */
@Service
public class ItemServiceImpl implements IItemService {

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;

    @Autowired
    private IPromoService promoService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {

        ValidationResult result = validator.validate(itemModel);

        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrorMsg());
        }

        ItemDO itemDO = convertItemDOFromItemModel(itemModel);
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());

        ItemStockDO itemStockDO = this.convertItemStockDOFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);

        return this.getItemById(itemModel.getId());
    }

    @Override
    public List<ItemModel> listItem() {

        List<ItemDO> itemDOList = itemDOMapper.listItem();

        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = this.convertItemModelFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());

        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {

        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (itemDO == null)
            return null;

        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        ItemModel itemModel = convertItemModelFromDataObject(itemDO, itemStockDO);

        // 获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (promoModel != null && promoModel.getStatus().intValue() != 3) {
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {

        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if (itemModel == null) {
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id, itemModel);
            redisTemplate.expire("item_validate_" + id, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {

        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {

//        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue() * -1);

        if (result > 0) {
            return true;
        } else if (result == 0) {
            // 库存售罄
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        } else {
            return this.increaseStock(itemId, amount);
        }
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {

        boolean mqResult = mqProducer.asyncReduceStock(itemId, amount);
        return mqResult;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {

        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String initStockLog(Integer itemId, Integer amount) {

        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStatus(1);
        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }

    private ItemDO convertItemDOFromItemModel(ItemModel itemModel) {

        if (itemModel == null)
            return null;

        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());

        return itemDO;
    }

    private ItemStockDO convertItemStockDOFromItemModel(ItemModel itemModel) {

        if (itemModel == null)
            return null;

        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());

        return itemStockDO;
    }

    private ItemModel convertItemModelFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO) {

        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }
}
