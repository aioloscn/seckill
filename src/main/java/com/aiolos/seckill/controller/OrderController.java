package com.aiolos.seckill.controller;

import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.model.UserModel;
import com.aiolos.seckill.mq.MqProducer;
import com.aiolos.seckill.response.CommonReturnType;
import com.aiolos.seckill.service.IItemService;
import com.aiolos.seckill.service.IOrderService;
import com.aiolos.seckill.service.IPromoService;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

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

    @Autowired
    private IPromoService promoService;

    private ExecutorService executorService;

    private RateLimiter createOrderRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);

        createOrderRateLimiter = RateLimiter.create(200);
    }

    @PostMapping("/createorder")
    public CommonReturnType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam(value = "promoId", required = false) Integer promoId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam("promoToken") String promoToken) throws BusinessException {

        if (!createOrderRateLimiter.tryAcquire()) {
            throw new BusinessException(EmBusinessError.RATELIMITER);
        }

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);

        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
        }

        // 检查秒杀令牌是否正确
        if (promoId != null) {

            String redisPromoToken = redisTemplate.opsForValue().get("promo_token_" + promoId + "_userid_" + userModel.getId() + "_itemid_" + itemId).toString();
            if (redisPromoToken == null) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }

            if (!StringUtils.equals(promoToken, redisPromoToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        // 同步调用线程池的submit方法
        // 拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                // 初始化库存流水
                String stockLogId = itemService.initStockLog(itemId, amount);

//        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
                if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId))
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");

                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonReturnType.create(null);
    }

    @PostMapping("/generatetoken")
    public CommonReturnType generateToken(@RequestParam("itemId") Integer itemId, @RequestParam("promoId") Integer promoId) throws BusinessException {

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
        }

        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);

        if (userModel == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登陆，不能下单");
        }

        String promoToken = promoService.generateSeckillToken(promoId, itemId, userModel.getId());
        if (promoToken == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }

        return CommonReturnType.create(promoToken);
    }
}
