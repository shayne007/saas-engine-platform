package com.feng.engine.messaging;

import java.lang.reflect.Method;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * TODO
 *
 * @since 2025/7/23
 */
@Component
public class KafkaListenerInitializer implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        event.getApplicationContext().getBeansWithAnnotation(MessageListener.class).forEach((name, bean) -> {
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                MessageListener annotation = method.getAnnotation(MessageListener.class);
                if (annotation != null) {
                    registerListener(bean, method, annotation);
                }
            }
        });
    }

    private void registerListener(Object bean, Method method, MessageListener annotation) {
        // TODO
        System.out.println("Registering listener: " + bean.getClass().getName() + "." + method.getName());
    }
}
