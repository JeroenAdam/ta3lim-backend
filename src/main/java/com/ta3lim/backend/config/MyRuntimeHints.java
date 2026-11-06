package com.ta3lim.backend.config;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;

@Configuration
public class MyRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register method for reflection
        Method method = ReflectionUtils.findMethod(AbstractEnvironment.class, "getProperty", String.class);
        hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
        // Register resources
        hints.resources().registerPattern("application.properties");
    }
}
