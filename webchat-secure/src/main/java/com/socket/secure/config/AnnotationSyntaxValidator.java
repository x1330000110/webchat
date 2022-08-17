package com.socket.secure.config;

import com.socket.secure.constant.SecureConstant;
import com.socket.secure.filter.anno.Encrypted;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * This class to check if {@linkplain com.socket.secure.filter.anno.Encrypted @Encrypted} marker location is legal
 */
@Order(0)
@Component
public class AnnotationSyntaxValidator implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        Encrypted anno = clazz.getAnnotation(Encrypted.class);
        // CHECK CLASS
        if (anno != null) {
            if (isSupportClass(clazz)) {
                return bean;
            }
            throw new BeanCreationException(beanName, "You can only mark @Encrypted on @Controller or @RestController");
        }
        // CHECK METHOD
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(Encrypted.class) != null && !isSupportMethod(method)) {
                throw new BeanCreationException(beanName, "You can only mark @Encrypted on @RequestMapping or derived annotations [method: " + method.getName() + "()]");
            }
        }
        return bean;
    }

    /**
     * Check if this method is protected by encryption
     */
    private boolean isSupportMethod(Method method) {
        return SecureConstant.SUPPORT_METHOD_ANNOS.stream().anyMatch(e -> method.getAnnotation(e) != null);
    }

    /**
     * Check if this controller is protected by encryption
     */
    private boolean isSupportClass(Class<?> clazz) {
        return SecureConstant.SUPPORT_CLASS_ANNOS.stream().anyMatch(e -> clazz.getAnnotation(e) != null);
    }
}
