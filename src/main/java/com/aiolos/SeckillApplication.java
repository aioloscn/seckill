package com.aiolos;

import com.aiolos.seckill.dao.UserDOMapper;
import com.aiolos.seckill.dataobject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Aiolos
 * @date 2019-06-01 15:40
 */
@SpringBootApplication(scanBasePackages = {"com.aiolos"})
@MapperScan("com.aiolos.seckill.dao")
@RestController
public class SeckillApplication {

    @Autowired
    private UserDOMapper userDOMapper;

    @GetMapping
    public String query() {

        UserDO userDO = userDOMapper.selectByPrimaryKey(1);
        if (userDO == null)
            return "null";
        return "ok";
    }

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
