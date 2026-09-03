package com.examiq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.path:./storage}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resource files (papers/notes) are intentionally NOT exposed as static
        // files: access is permission-gated and served through
        // FileAccessController (/api/papers/{id}/file) instead.
        String avatarLocation = "file:"
                + java.nio.file.Paths.get(storagePath).toAbsolutePath().normalize().toString() + "/avatars/";
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(avatarLocation);
    }
}
