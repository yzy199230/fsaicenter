# 事件驱动机制分析与 MQ 改造建议

## 当前实现分析

### 现状
项目使用 **Spring 内存事件机制**（ApplicationEventPublisher）实现异步计费和日志记录：

```
主流程 → DomainEventPublisher.publishEvent()
         ↓
    Spring ApplicationEventPublisher
         ↓
    @Async + @EventListener
         ↓
    BillingEventListener / RequestLogEventListener
```

### 优点
1. **实现简单**：无需引入外部 MQ 组件
2. **低延迟**：内存事件，无网络开销
3. **开发友好**：Spring 原生支持，易于调试
4. **轻量级**：适合中小规模应用

### 缺点（风险）
1. **数据丢失风险**：
   - 应用重启时，未处理的事件会丢失
   - JVM 崩溃时，内存中的事件全部丢失
   - 无持久化机制

2. **无法水平扩展**：
   - 事件只在单个 JVM 实例内传播
   - 多实例部署时，事件无法共享

3. **无重试机制**：
   - 事件处理失败后，依赖 Spring @Retryable
   - 无死信队列（DLQ）

4. **无监控能力**：
   - 无法查看事件积压情况
   - 无法追踪事件处理链路

---

## MQ 改造方案

### 推荐方案：RabbitMQ

#### 为什么选择 RabbitMQ？
- **轻量级**：相比 Kafka，更适合中小规模
- **功能完善**：支持死信队列、延迟队列、优先级队列
- **Spring 集成好**：Spring AMQP 开箱即用
- **运维成本低**：单机即可运行

#### 架构设计

```
主流程 → DomainEventPublisher
         ↓
    RabbitTemplate.convertAndSend()
         ↓
    RabbitMQ Exchange (Topic)
         ├─ billing.queue → BillingEventListener
         └─ log.queue → RequestLogEventListener

失败重试 → DLQ (Dead Letter Queue)
```

#### 队列设计

| 队列名称 | 路由键 | 消费者 | TTL | 死信队列 |
|---------|--------|--------|-----|---------|
| `billing.queue` | `event.billing` | BillingEventListener | 30s | `billing.dlq` |
| `log.queue` | `event.log` | RequestLogEventListener | 30s | `log.dlq` |

---

## 改造步骤

### 1. 添加依赖（pom.xml）

```xml
<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 2. 配置 RabbitMQ（application.yml）

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /fsaicenter
    listener:
      simple:
        # 并发消费者数量
        concurrency: 5
        max-concurrency: 10
        # 手动确认模式
        acknowledge-mode: manual
        # 预取数量
        prefetch: 10
        # 重试配置
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          multiplier: 2.0
```

### 3. 创建 RabbitMQ 配置类

```java
@Configuration
public class RabbitMQConfig {

    // 交换机
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange("fsaicenter.event.exchange", true, false);
    }

    // 计费队列
    @Bean
    public Queue billingQueue() {
        return QueueBuilder.durable("billing.queue")
                .withArgument("x-dead-letter-exchange", "fsaicenter.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "billing.dlq")
                .withArgument("x-message-ttl", 30000) // 30秒
                .build();
    }

    // 绑定
    @Bean
    public Binding billingBinding() {
        return BindingBuilder.bind(billingQueue())
                .to(eventExchange())
                .with("event.billing");
    }

    // 死信队列
    @Bean
    public Queue billingDLQ() {
        return new Queue("billing.dlq", true);
    }
}
```

### 4. 修改事件发布器

```java
@Component
public class DomainEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishBillingEvent(BillingEvent event) {
        rabbitTemplate.convertAndSend(
            "fsaicenter.event.exchange",
            "event.billing",
            event
        );
    }
}
```

### 5. 修改事件监听器

```java
@Component
public class BillingEventListener {

    @RabbitListener(queues = "billing.queue")
    public void handleBillingEvent(BillingEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            // 处理计费逻辑
            processBilling(event);

            // 手动确认
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("计费事件处理失败", e);
            // 拒绝并重新入队（会触发重试）
            channel.basicNack(tag, false, true);
        }
    }
}
```

---

## 改造成本评估

### 开发成本
- **代码改动量**：中等（约 5-8 个文件）
- **开发时间**：2-3 天
- **测试时间**：1-2 天

### 运维成本
- **新增组件**：RabbitMQ（单机或集群）
- **监控**：RabbitMQ Management UI
- **资源消耗**：内存 ~512MB，CPU 低

### 风险
- **引入复杂度**：需要维护 MQ 组件
- **网络延迟**：相比内存事件，延迟增加 1-5ms
- **消息顺序**：需要考虑消息顺序性（如果业务需要）

---

## 建议

### 短期（当前阶段）
**保持现有内存事件机制**，原因：
1. 项目处于开发阶段，快速迭代优先
2. 单实例部署，无水平扩展需求
3. 计费和日志允许少量丢失（可通过定时任务补偿）

**改进措施**：
- 添加 `@Retryable` 注解，增强重试能力
- 使用 `@Async` 配置 Virtual Thread，提升并发
- 添加监控指标（Micrometer）

### 中期（生产环境）
**引入 RabbitMQ**，触发条件：
1. 多实例部署（负载均衡）
2. 计费准确性要求提高
3. 需要审计和追溯能力

### 长期（大规模）
**考虑 Kafka**，触发条件：
1. 日请求量 > 100万
2. 需要事件溯源（Event Sourcing）
3. 需要实时数据分析

---

## 代码示例：增强现有机制

### 添加重试能力

```java
@Slf4j
@Component
public class BillingEventListener {

    @Async
    @EventListener
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional(rollbackFor = Exception.class)
    public void handleBillingEvent(BillingEvent event) {
        // 处理逻辑
    }

    @Recover
    public void recover(Exception e, BillingEvent event) {
        log.error("计费事件处理失败，已达最大重试次数: {}", event.getRequestId(), e);
        // 可以将失败事件写入数据库，后续人工处理
    }
}
```

### 添加监控指标

```java
@Component
public class DomainEventPublisher {

    @Autowired
    private MeterRegistry meterRegistry;

    public void publishBillingEvent(BillingEvent event) {
        try {
            eventPublisher.publishEvent(event);
            meterRegistry.counter("event.billing.published").increment();
        } catch (Exception e) {
            meterRegistry.counter("event.billing.failed").increment();
            throw e;
        }
    }
}
```

---

## 总结

| 方案 | 适用场景 | 优先级 |
|------|---------|--------|
| **保持内存事件 + 增强** | 开发阶段、单实例 | ⭐⭐⭐⭐⭐ |
| **引入 RabbitMQ** | 生产环境、多实例 | ⭐⭐⭐ |
| **迁移到 Kafka** | 大规模、高吞吐 | ⭐ |

**当前建议**：保持现有机制，添加 `@Retryable` 和监控指标，待生产环境再评估是否引入 MQ。
