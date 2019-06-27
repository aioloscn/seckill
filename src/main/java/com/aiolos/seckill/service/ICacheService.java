package com.aiolos.seckill.service;

/**
 * @author Aiolos
 * @date 2019-06-27 20:49
 */
public interface ICacheService {

    void setCommonCache(String key, Object value);

    Object getFromCommonCache(String key);
}
