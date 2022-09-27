package com.socket.secure.config;

import com.socket.secure.filter.anno.Encrypted;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This class to check if {@linkplain com.socket.secure.filter.anno.Encrypted @Encrypted} marker location is legal
 */
@Order(0)
@Component
public class AnnotationSyntax implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        if (AnnotationUtils.findAnnotation(clazz, Component.class) == null) {
            return bean;
        }
        Encrypted anno = clazz.getAnnotation(Encrypted.class);
        // CHECK CLASS
        if (anno != null) {
            if (AnnotationUtils.findAnnotation(clazz, Controller.class) != null) {
                return bean;
            }
            throw new BeanCreationException(beanName, "You should be mark @Encrypted on @Controller or @RestController");
        }
        // CHECK METHOD
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(Encrypted.class) != null && AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) {
                String params = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "));
                throw new BeanCreationException(beanName, "You should be mark @Encrypted on @RequestMapping or derived annotations [method: " + method.getName() + "(" + params + ")]");
            }
        }
        return bean;
    }
}
