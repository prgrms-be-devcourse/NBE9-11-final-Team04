package com.team04.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Profile("local")
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(basePath).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
