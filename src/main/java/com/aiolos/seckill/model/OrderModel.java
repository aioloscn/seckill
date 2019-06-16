package com.aiolos.seckill.model;

import java.math.BigDecimal;

/**
 * @author Aiolos
 * @date 2019-06-16 20:31
 */
public class OrderModel {

    private String id;

    private Integer userId;

    private Integer itemId;

    /**
     * 若非空，则表示以秒杀商品方式下单
     */
    private Integer promoId;

    /**
     * 购买商品的单价，若promoId非空，则表示秒杀商品价格
     */
    private BigDecimal itemPrice;

    private Integer amount;

    private BigDecimal orderPrice;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
    }

    public Integer getPromoId() {
        return promoId;
    }

    public void setPromoId(Integer promoId) {
        this.promoId = promoId;
    }
}
