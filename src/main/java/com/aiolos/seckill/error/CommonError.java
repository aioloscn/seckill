package com.aiolos.seckill.error;

/**
 * @author Aiolos
 * @date 2019-06-13 20:14
 */
public interface CommonError {

    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);
}
