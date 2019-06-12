package com.aiolos.seckill.service;

import com.aiolos.seckill.dataobject.UserDO;

/**
 * @author Aiolos
 * @date 2019-06-12 22:44
 */
public interface IUserService {

    UserDO getUserById(Integer id);
}
