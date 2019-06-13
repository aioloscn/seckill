package com.aiolos.seckill.service;

import com.aiolos.seckill.model.UserModel;

/**
 * @author Aiolos
 * @date 2019-06-12 22:44
 */
public interface IUserService {

    UserModel getUserById(Integer id);
}
