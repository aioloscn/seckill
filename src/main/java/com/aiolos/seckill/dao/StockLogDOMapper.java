package com.aiolos.seckill.dao;

import com.aiolos.seckill.dataobject.StockLogDO;

public interface StockLogDOMapper {
    int deleteByPrimaryKey(String stockLogId);

    int insert(StockLogDO record);

    int insertSelective(StockLogDO record);

    StockLogDO selectByPrimaryKey(String stockLogId);

    int updateByPrimaryKeySelective(StockLogDO record);

    int updateByPrimaryKey(StockLogDO record);
}