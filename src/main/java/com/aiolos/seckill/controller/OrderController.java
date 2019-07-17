package com.aiolos.seckill.controller;

import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.model.OrderModel;
import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.mq.MqProducer;
import com.aiolos.seckill.response.CommonReturnType;
import com.aiolos.seckill.service.IItemService;
import com.aiolos.seckill.service.IOrderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Aiolos
 * @date 2019-06-16 21:51
 */
@RestController
@RequestMapping("/order")
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class OrderController extends BaseController {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private IItemService itemService;

    @PostMapping("/createorder")
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam(value = "promoId", required = false) Integer promoId,
                                        @RequestParam("amount") Integer amount) throws BusinessException {

//        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if (isLogin == null || !isLogin.booleanValue()) {
//            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
//        }
//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);

        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
        }

        // 初始化库存流水
        itemService.initStockLog(itemId, amount);

//        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
        if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount))
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
        return CommonReturnType.create(null);
    }
}
