package com.aiolos.seckill.controller;

import com.aiolos.seckill.error.BusinessException;
import com.aiolos.seckill.error.EmBusinessError;
import com.aiolos.seckill.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Aiolos
 * @date 2019-06-13 23:35
 */
public class BaseController {

    /**
     * 定义exceptionHandler解决未被controller层处理的exception
     * @param request
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object handlerException(HttpServletRequest request, Exception ex) {

        Map<String, Object> responseData = new HashMap<>();

        if (ex instanceof BusinessException) {

            BusinessException businessException = (BusinessException) ex;
            responseData.put("errCode", businessException.getErrCode());
            responseData.put("errMsg", businessException.getErrMsg());
        } else {

            responseData.put("errCOde", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
        }

        return CommonReturnType.create(responseData, "fail");
    }
}
