package com.feng.engine.messaging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义消费者进程消息监听器
 *
 * @since 2025/3/6
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageListener {
    String groupId() default "";

    int concurrency() default 1;

    String topic();
}
