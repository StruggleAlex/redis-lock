package com.alex.redislock.aspect;

import cn.hutool.core.util.HashUtil;
import cn.hutool.json.JSONObject;
import com.alex.redislock.annotation.RedisLock;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Title: ControllerLogAspect
 * @Description:
 * @Auther:wangli
 * @Version: 1.0
 * @create 2019-04-12 19:00
 */
@Aspect
@Component
public class ControllerLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogAspect.class);


    // 分布式事务锁
    public static final String REDISLOCK = "@annotation(com.alex.redislock.annotation.RedisLock)";


    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * @param pjp 拦截的对象
     * @return
     * @throws Throwable
     */
    @Around(REDISLOCK)
    public Object redisLock(ProceedingJoinPoint pjp) throws Throwable {
        Object proceed = null;
        //获取注解
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        RedisLock redisLock = signature.getMethod().getAnnotation(RedisLock.class);
        //获取请求参数
        Object[] args = pjp.getArgs();
        //转为哈希code
        int hashcode = HashUtil.elfHash(args[0].toString());

        //前缀+方法名称+请求参数的hash值
        String key = redisLock.lockUniquePrefix() + signature.getName() + hashcode;

        //线程id
        String threadId = String.valueOf(Thread.currentThread().getId());

        Boolean res = redisTemplate.opsForValue().setIfAbsent(key, threadId, redisLock.expire(), TimeUnit.SECONDS);
        if (!res) {
            throw new RuntimeException("请勿重复请求");
        }
        try {
            proceed = pjp.proceed();
        } finally {
            //判断是否是同一个线程才去删除，防止线程错误删除,死锁问题
            if (threadId.equals(redisTemplate.opsForValue().get(key))) {
                redisTemplate.delete(key);
            }

        }


        return proceed;
    }


}
