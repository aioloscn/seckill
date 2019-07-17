package com.aiolos.seckill.dao;

import com.aiolos.seckill.dataobject.StockLogDO;

public interface StockLogDOMapper {
    int insert(StockLogDO record);

    int insertSelective(StockLogDO record);
}