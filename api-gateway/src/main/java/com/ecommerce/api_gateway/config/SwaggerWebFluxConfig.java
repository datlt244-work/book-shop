package com.ecommerce.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * WebFlux Router configuration for Swagger UI static resources.
 * Spring Cloud Gateway does not serve static resources by default,
 * so we need to explicitly configure routes for Swagger UI.
 */
@Configuration
public class SwaggerWebFluxConfig {

    private static final String SWAGGER_UI_VERSION = "5.21.0";

    /**
     * Redirect /swagger-ui.html to the actual Swagger UI index page.
     */
    @Bean
    public RouterFunction<ServerResponse> swaggerRedirectRouterFunction() {
        return RouterFunctions.route()
                .GET("/swagger-ui.html", request -> ServerResponse.temporaryRedirect(
                        URI.create("/webjars/swagger-ui/index.html")).build())
                .build();
    }

    /**
     * Serve webjars static resources.
     * Webjars are stored in META-INF/resources/webjars/{artifact}/{version}/
     */
    @Bean
    public RouterFunction<ServerResponse> webjarsRouterFunction() {
        return RouterFunctions.route()
                .GET("/webjars/**", this::serveWebjarResource)
                .build();
    }

    /**
     * Serve swagger-ui resources directly.
     */
    @Bean
    public RouterFunction<ServerResponse> swaggerUiRouterFunction() {
        return RouterFunctions.route()
                .GET("/swagger-ui/**", this::serveSwaggerUiResource)
                .build();
    }

    private Mono<ServerResponse> serveWebjarResource(ServerRequest request) {
        String path = request.path();
        // Format: /webjars/swagger-ui/{file} ->
        // META-INF/resources/webjars/swagger-ui/{version}/{file}

        if (path.startsWith("/webjars/swagger-ui/")) {
            String filePath = path.substring("/webjars/swagger-ui/".length());

            // Priority 1: Check custom resources in project (e.g., swagger-initializer.js)
            Resource customResource = new ClassPathResource("META-INF/resources/webjars/swagger-ui/" + filePath);
            if (customResource.exists()) {
                return serveResource(customResource, path);
            }

            // Priority 2: Fall back to webjars library with version
            String resourcePath = "META-INF/resources/webjars/swagger-ui/" + SWAGGER_UI_VERSION + "/" + filePath;
            Resource resource = new ClassPathResource(resourcePath);
            return serveResource(resource, path);
        } else {
            // For other webjars, keep the path as-is
            String resourcePath = "META-INF/resources" + path;
            Resource resource = new ClassPathResource(resourcePath);
            return serveResource(resource, path);
        }
    }

    private Mono<ServerResponse> serveSwaggerUiResource(ServerRequest request) {
        String path = request.path();
        // Remove leading /swagger-ui/ to get the file path
        String filePath = path.substring("/swagger-ui/".length());
        String resourcePath = "META-INF/resources/webjars/swagger-ui/" + SWAGGER_UI_VERSION + "/" + filePath;
        Resource resource = new ClassPathResource(resourcePath);
        return serveResource(resource, path);
    }

    private Mono<ServerResponse> serveResource(Resource resource, String path) {
        return Mono.fromCallable(resource::exists)
                .flatMap(exists -> {
                    if (exists) {
                        MediaType mediaType = getMediaType(path);
                        return ServerResponse.ok()
                                .contentType(mediaType)
                                .body(BodyInserters.fromResource(resource));
                    } else {
                        return ServerResponse.notFound().build();
                    }
                });
    }

    private MediaType getMediaType(String path) {
        if (path.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        } else if (path.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        } else if (path.endsWith(".js")) {
            return MediaType.valueOf("application/javascript");
        } else if (path.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (path.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (path.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        } else if (path.endsWith(".woff") || path.endsWith(".woff2")) {
            return MediaType.valueOf("font/woff2");
        } else if (path.endsWith(".ttf")) {
            return MediaType.valueOf("font/ttf");
        } else if (path.endsWith(".map")) {
            return MediaType.APPLICATION_JSON;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
