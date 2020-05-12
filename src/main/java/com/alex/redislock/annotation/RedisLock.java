package com.alex.redislock.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @Title: RedisLock
 * @Description: redis分布式锁
 * @Auther:wangli
 * @Version: 1.0
 * @create 2019-11-21 15:58
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Component
public @interface RedisLock {

    /**
     * 锁的前缀
     * @return
     */
    String lockUniquePrefix() default "lock_";

    /**
     * 过期时间
     *
     * @return
     */
    long expire() default 30L;
}
