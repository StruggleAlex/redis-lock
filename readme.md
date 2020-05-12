概述
为了防止分布式系统中的多个进程之间相互干扰，我们需要一种分布式协调技术来对这些进程进行调度。而这个分布式协调技术的核心就是来实现这个分布式锁。

 

为什么要使用分布式锁


成员变量 A 存在 JVM1、JVM2、JVM3 三个 JVM 内存中
成员变量 A 同时都会在 JVM 分配一块内存，三个请求发过来同时对这个变量操作，显然结果是不对的
不是同时发过来，三个请求分别操作三个不同 JVM 内存区域的数据，变量 A 之间不存在共享，也不具有可见性，处理的结果也是不对的
注：该成员变量 A 是一个有状态的对象
如果我们业务中确实存在这个场景的话，我们就需要一种方法解决这个问题，这就是分布式锁要解决的问题

分布式锁应该具备哪些条件
在分布式系统环境下，一个方法在同一时间只能被一个机器的一个线程执行
高可用的获取锁与释放锁
高性能的获取锁与释放锁
具备可重入特性（可理解为重新进入，由多于一个任务并发使用，而不必担心数据错误）
具备锁失效机制，防止死锁
具备非阻塞锁特性，即没有获取到锁将直接返回获取锁失败
分布式锁的实现有哪些
Memcached：利用 Memcached 的 add 命令。此命令是原子性操作，只有在 key 不存在的情况下，才能 add 成功，也就意味着线程得到了锁。
Redis：和 Memcached 的方式类似，利用 Redis 的 setnx 命令。此命令同样是原子性操作，只有在 key 不存在的情况下，才能 set 成功。
Zookeeper：利用 Zookeeper 的顺序临时节点，来实现分布式锁和等待队列。Zookeeper 设计的初衷，就是为了实现分布式锁服务的。
Chubby：Google 公司实现的粗粒度分布式锁服务，底层利用了 Paxos 一致性算法。

通过 Redis 分布式锁的具体实现

引入依赖包：

         <dependency>
              <groupId>cn.hutool</groupId>
              <artifactId>hutool-all</artifactId>
              <version>4.6.1</version>
          </dependency>
  
          <!--redis依赖包-->
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-data-redis</artifactId>
              <exclusions>
                  <exclusion>
                      <artifactId>log4j-to-slf4j</artifactId>
                      <groupId>org.apache.logging.log4j</groupId>
                  </exclusion>
                  <exclusion>
                      <artifactId>log4j-api</artifactId>
                      <groupId>org.apache.logging.log4j</groupId>
                  </exclusion>
              </exclusions>
          </dependency>
  
          <!--aop依赖包-->
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-aop</artifactId>
          </dependency>`    
      
1.自定义注解

package com.test.alex.annotation;
 
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
    String lockUniquePrefix () default "lock_";
 
    /**
     * 过期时间
     *
     * @return
     */
    long expire() default 30L;
}

2.Aop 中Around方法拦截方法 其中使用了hutool工具类，生成hash值。


@Aspect
@Component
public class ControllerLogAspect {
 
    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerLogAspect.class);
 
 
 
 
    // 分布式事务锁
    public static final String REDISLOCK = "@annotation(com.test.alex.annotation.RedisLock)";
 
 
    @Autowired
    private RedisTemplate redisTemplate;
 
    @Around(REDISLOCK)
    public Object redisLock(ProceedingJoinPoint pjp) throws Throwable {
        Object proceed=null;
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
                throw new BizException("请勿重复请求");
            }
        try {
            proceed = pjp.proceed();
        }finally {
            //判断是否是同一个线程才去删除，防止线程错误删除,死锁问题
            if (threadId.equals(redisTemplate.opsForValue().get(key))) {
                redisTemplate.delete(key);
            }
 
        }
 
 
        return proceed;
    }
 
    
}
3.具体使用 参考demo

 
    @GetMapping("/lock")
        @RedisLock(lockUniquePrefix = "test_",expire = 10L)
        public void getMessage(@RequestParam(name = "test") String test) {
            System.out.println(test);
      }
 

原文链接：https://blog.csdn.net/weixin_43757849/article/details/103205797