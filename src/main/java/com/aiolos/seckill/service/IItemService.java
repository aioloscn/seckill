package com.aiolos.seckill.service;

import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.model.ItemModel;

import java.util.List;

/**
 * @author Aiolos
 * @date 2019-06-16 17:41
 */
public interface IItemService {

    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    List<ItemModel> listItem();

    ItemModel getItemById(Integer id);

    /**
     * item及promo cache缓存模型
     * @param id
     * @return
     */
    ItemModel getItemByIdInCache(Integer id);

    /**
     * 库存扣减
     * @param itemId
     * @param amount
     * @return
     * @throws BusinessException
     */
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    /**
     * 商品销量增加
     * @param itemId
     * @param amount
     * @throws BusinessException
     */
    void increaseSales(Integer itemId, Integer amount) throws BusinessException;
}
