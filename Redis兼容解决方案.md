# Redis兼容解决方案

## 问题描述

security-sso模块引入了web-core-sdk，需要兼容使用web-core-sdk的Redis功能，避免配置冲突。

## 解决方案

### 1. 修改web-core-sdk的Redis配置

**目标**: 让web-core-sdk的Redis配置更加灵活，避免与应用程序的配置冲突。

**修改内容**:
- 移除`@Primary`注解，避免强制覆盖应用程序的Redis配置
- 保持`@ConditionalOnMissingBean`注解，确保只在没有其他配置时才生效
- 保持`@ConditionalOnProperty`注解，确保只在配置了Redis时才生效

```java
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.redis.host")
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 配置RedisTemplate
    }

    @Bean
    @ConditionalOnProperty(name = "spring.redis.host")
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 配置StringRedisTemplate
    }
}
```

### 2. 修改SSO模块的缓存配置

**目标**: 让SSO模块的缓存配置与web-core-sdk兼容，并优先使用SSO的配置。

**修改内容**:
- 在CacheConfig中添加`@Primary`注解，确保SSO的缓存配置优先
- 配置Jackson ObjectMapper以支持Java 8时间类型
- 添加错误处理机制

```java
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置Jackson ObjectMapper以支持Java 8时间类型
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册Java 8时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 配置Instant的序列化和反序列化
        javaTimeModule.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        javaTimeModule.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
        
        objectMapper.registerModule(javaTimeModule);
        
        // 配置类型信息，用于反序列化
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }

    /**
     * 创建支持Java 8时间类型的Redis序列化器
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        try {
            return new GenericJackson2JsonRedisSerializer(createObjectMapper());
        } catch (Exception e) {
            // 如果配置失败，使用默认的序列化器
            return new GenericJackson2JsonRedisSerializer();
        }
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJsonSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withCacheConfiguration("oauth2-clients-by-id",
                        config.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration("oauth2-clients-by-client-id",
                        config.entryTtl(Duration.ofMinutes(60)))
                .withCacheConfiguration("token-blacklist",
                        config.entryTtl(Duration.ofDays(1)))
                .build();
    }
}
```

### 3. 在SSO模块中使用web-core-sdk的Redis功能

**目标**: 展示如何在SSO模块中使用web-core-sdk提供的Redis工具类。

**示例代码**:
```java
@Service
public class RedisServiceExample {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private JedisRedisClient jedisRedisClient;

    @Autowired(required = false)
    private LettuceRedisClient lettuceRedisClient;

    @Autowired(required = false)
    private RedissonLockUtil redissonLockUtil;

    @Autowired(required = false)
    private DistributedLockFactory distributedLockFactory;

    // 使用RedisTemplate
    public void storeWithRedisTemplate(String key, String value) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
        }
    }

    // 使用Jedis客户端
    public void storeWithJedis(String key, String value) {
        if (jedisRedisClient != null) {
            jedisRedisClient.set(key, value);
            jedisRedisClient.expire(key, 30);
        }
    }

    // 使用分布式锁
    public boolean tryLockWithRedisson(String lockKey, long waitTime, long leaseTime) {
        if (redissonLockUtil != null) {
            return redissonLockUtil.tryLock(lockKey, waitTime, leaseTime, TimeUnit.SECONDS);
        }
        return false;
    }
}
```

## 兼容性说明

### 1. 配置优先级
- SSO模块的缓存配置使用`@Primary`注解，优先级最高
- web-core-sdk的Redis配置使用`@ConditionalOnMissingBean`，只在没有其他配置时生效
- 两者可以共存，不会产生冲突

### 2. 功能使用
- SSO模块可以同时使用自己的缓存配置和web-core-sdk的Redis工具类
- web-core-sdk提供的工具类都是可选的（使用`@Autowired(required = false)`）
- 可以根据需要选择使用哪些Redis功能

### 3. 配置灵活性
- 如果SSO模块有自己的Redis配置，web-core-sdk的配置不会生效
- 如果SSO模块没有Redis配置，web-core-sdk的配置会自动生效
- 可以通过配置文件控制是否启用web-core-sdk的Redis功能

## 验证方法

1. **启动应用**: 启动security-sso应用，检查是否有Bean冲突错误
2. **检查日志**: 查看启动日志，确认Redis连接正常
3. **功能测试**: 测试OAuth2功能是否正常工作
4. **Redis功能测试**: 使用RedisServiceExample测试web-core-sdk的Redis功能

## 优势

1. **兼容性好**: SSO模块可以同时使用自己的配置和web-core-sdk的功能
2. **灵活性高**: 可以根据需要选择使用哪些Redis功能
3. **可维护性强**: 配置清晰，易于理解和维护
4. **可扩展性强**: 其他模块也可以采用相同的模式使用web-core-sdk

## 注意事项

1. **配置检查**: 确保Redis连接配置正确
2. **依赖管理**: 确保web-core-sdk的版本兼容
3. **功能选择**: 根据实际需求选择使用web-core-sdk的哪些功能
4. **性能考虑**: 合理使用Redis功能，避免过度依赖 