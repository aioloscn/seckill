package com.aiolos.seckill.service.impl;

import com.aiolos.seckill.dao.UserDOMapper;
import com.aiolos.seckill.dao.UserPasswordDOMapper;
import com.aiolos.seckill.dataobject.UserDO;
import com.aiolos.seckill.dataobject.UserPasswordDO;
import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.service.IUserService;
import com.aiolos.seckill.validator.ValidationResult;
import com.aiolos.seckill.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Aiolos
 * @date 2019-06-12 22:44
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private ValidatorImpl validator;

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

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void register(UserModel userModel) throws BusinessException {

        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        ValidationResult validationResult = validator.validate(userModel);

        if (validationResult.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, validationResult.getErrorMsg());
        }

        UserDO userDO = convertFromModel(userModel);

        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号已被注册");
        }

        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;
    }

    @Override
    public UserModel validateLogin(String telphone, String encryptPassword) throws BusinessException {

        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if (userDO == null)
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);

        if (!StringUtils.equals(encryptPassword, userModel.getEncryptPassword())) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        return userModel;
    }

    private UserDO convertFromModel(UserModel userModel) {

        if (userModel == null)
            return null;

        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }

    private UserPasswordDO convertPasswordFromModel(UserModel userModel) {

        if (userModel == null)
            return null;

        UserPasswordDO userPasswordDO = new UserPasswordDO();
        BeanUtils.copyProperties(userModel, userPasswordDO);
        return userPasswordDO;
    }

    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {

        if (userDO == null)
            return null;

        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);

        if (userPasswordDO != null) {

            userModel.setEncryptPassword(userPasswordDO.getEncryptPassword());
        }
        return userModel;
    }
}
