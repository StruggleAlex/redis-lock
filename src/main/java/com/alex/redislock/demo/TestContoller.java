package com.alex.redislock.demo;

import com.alex.redislock.annotation.RedisLock;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Title: TestContoller
 * @Description: 测试类
 * @Auther:wangli
 * @Version: 1.0
 * @create 2020-05-12 9:30
 */
@Controller
@RequestMapping("/test")
public class TestContoller {


    @GetMapping("/lock")
    @RedisLock(lockUniquePrefix = "test_",expire = 10L)
    public void getMessage(@RequestParam(name = "test") String test) {
        System.out.println(test);
    }
}
