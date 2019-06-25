package com.aiolos.seckill.controller;

import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.response.CommonReturnType;
import com.aiolos.seckill.service.IUserService;
import com.aiolos.seckill.vo.UserVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Aiolos
 * @date 2019-06-12 22:52
 */
@RestController
@RequestMapping("/user")
// DEFAULT_ALLOWED_CREDENTIALS: 需配合前端设置xhrFields授信后使得跨域session共享
// DEFAULT_ALLOWED_HEADERS: 允许跨域传输所有的header参数，将用于使用token放入header域做session共享的跨域请求
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private IUserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

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

    @PostMapping("/getotp")
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam("telphone") String telphone) {

        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 100000;
        String otpCode = String.valueOf(randomInt);

        httpServletRequest.getSession().setAttribute(telphone, otpCode);

        System.out.println("telphone=" + telphone + "&otpCode=" + otpCode);
        return CommonReturnType.create(null);
    }

    @PostMapping("/register")
    @ResponseBody
    public CommonReturnType register(@RequestParam("telphone") String telphone,
                                     @RequestParam("otpCode") String otpCode,
                                     @RequestParam("name") String name,
                                     @RequestParam("gender") Integer gender,
                                     @RequestParam("age") Integer age,
                                     @RequestParam("password") String password
        ) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);

        if (!com.alibaba.druid.util.StringUtils.equals(otpCode, inSessionOtpCode)) {

            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码错误");
        }

        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender.byteValue());
        userModel.setAge(age);
        userModel.setRegisterMode("byphone");
        userModel.setTelphone(telphone);
        userModel.setEncryptPassword(this.EncodeByMd5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);
    }

    private String EncodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        // 确认计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();

        // 加密字符串
        String newStr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    @PostMapping("/login")
    public CommonReturnType login(@RequestParam("telphone") String telphone,
                                  @RequestParam("password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)) {

            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        UserModel userModel = userService.validateLogin(telphone, this.EncodeByMd5(password));

        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-", "");

        redisTemplate.opsForValue().set(uuidToken, userModel);
        redisTemplate.expire(uuidToken, 1, TimeUnit.HOURS);

        // 将登陆凭证加入到用户的session中
//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", userModel);
        return CommonReturnType.create(uuidToken);
    }
}
