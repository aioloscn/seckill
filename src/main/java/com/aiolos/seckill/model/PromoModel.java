package com.aiolos.seckill.model;

import org.joda.time.DateTime;

import java.math.BigDecimal;

/**
 * @author Aiolos
 * @date 2019-06-16 23:00
 */
public class PromoModel {

    private Integer id;

    /**
     * 秒杀活动状态 1：未开始 2：进行中 3：已结束
     */
    private Integer status;

    private String promoName;

    /**
     * 秒杀活动开始时间
     */
    private DateTime startTime;

    /**
     * 秒杀活动的结束时间
     */
    private DateTime endTime;

    /**
     * 秒杀活动的适用商品
     */
    private Integer itemId;

    /**
     * 秒杀活动的商品价格
     */
    private BigDecimal promoItemPrice;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPromoName() {
        return promoName;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(DateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getPromoItemPrice() {
        return promoItemPrice;
    }

    public void setPromoItemPrice(BigDecimal promoItemPrice) {
        this.promoItemPrice = promoItemPrice;
    }
}
