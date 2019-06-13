package com.aiolos.seckill.service.impl;

import com.aiolos.seckill.dao.UserDOMapper;
import com.aiolos.seckill.dao.UserPasswordDOMapper;
import com.aiolos.seckill.dataobject.UserDO;
import com.aiolos.seckill.dataobject.UserPasswordDO;
import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.service.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Aiolos
 * @date 2019-06-12 22:44
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Override
    public UserModel getUserById(Integer id) {

        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if (userDO == null)
            return null;

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        return convertFromDataObject(userDO, userPasswordDO);
    }

    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {

        if (userDO == null)
            return null;

        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);

        if (userPasswordDO != null) {

            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }
        return userModel;
    }
}
