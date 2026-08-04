package com.grimoriopathfinder.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Vue application from the Spring Boot jar and supports history-mode
 * client-side routes when the browser requests them directly.
 */
@Configuration
public class SpaWebMvcConfiguration implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(new SpaResourceResolver());
  }

  static final class SpaResourceResolver extends PathResourceResolver {

    private static final String API_PREFIX = "api/";
    private static final String ACTUATOR_PREFIX = "actuator/";
    private static final String ASSETS_PREFIX = "assets/";
    private final Resource indexResource;

    SpaResourceResolver() {
      this(new ClassPathResource("static/index.html"));
    }

    SpaResourceResolver(Resource indexResource) {
      this.indexResource = indexResource;
    }

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {
      var resource = super.getResource(resourcePath, location);
      if (resource != null || !isSpaRoute(resourcePath)) {
        return resource;
      }
      return indexResource.exists() ? indexResource : null;
    }

    private boolean isSpaRoute(String resourcePath) {
      if (resourcePath == null || resourcePath.isBlank()) {
        return true;
      }
      var normalizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
      if (normalizedPath.startsWith(API_PREFIX) || normalizedPath.equals("api")) {
        return false;
      }
      if (normalizedPath.startsWith(ACTUATOR_PREFIX) || normalizedPath.equals("actuator")) {
        return false;
      }
      if (normalizedPath.startsWith(ASSETS_PREFIX)) {
        return false;
      }
      var lastSegment = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
      return !lastSegment.contains(".");
    }
  }
}
