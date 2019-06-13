package com.aiolos.seckill.controller;

import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.response.CommonReturnType;
import com.aiolos.seckill.service.IUserService;
import com.aiolos.seckill.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Aiolos
 * @date 2019-06-12 22:52
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private IUserService userService;

    @GetMapping("/{id:\\d+}")
    @ResponseBody
    public CommonReturnType getUser(@PathVariable Integer id) {

        return CommonReturnType.create(convertFromModel(userService.getUserById(id)));
    }

    private UserVO convertFromModel(UserModel userModel) {

        if (userModel == null)
            return null;

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(userModel, vo);
        return vo;
    }

}
