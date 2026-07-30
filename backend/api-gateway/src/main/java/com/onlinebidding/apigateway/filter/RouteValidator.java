package com.onlinebidding.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RouteValidator {

    private final List<PublicEndpoint> publicEndpoints;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RouteValidator(@Value("${gateway.public-endpoints:}") String[] publicEndpoints) {
        this.publicEndpoints = Arrays.stream(publicEndpoints)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(PublicEndpoint::new)
                .collect(Collectors.toList());
    }

    public boolean isSecured(ServerHttpRequest request) {
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return false;
        }

        String requestPath = request.getURI().getPath();
        String requestMethod = request.getMethod().name();

        for (PublicEndpoint endpoint : publicEndpoints) {
            boolean pathMatches = pathMatcher.match(endpoint.pathPattern, requestPath);
            boolean methodMatches = endpoint.method == null || endpoint.method.equalsIgnoreCase(requestMethod);

            if (pathMatches && methodMatches) {
                return false;
            }
        }
        return true;
    }

    private static class PublicEndpoint {
        private final String method;
        private final String pathPattern;

        public PublicEndpoint(String rawEndpoint) {
            if (rawEndpoint.contains(":")) {
                String[] parts = rawEndpoint.split(":", 2);
                this.method = parts[0].trim();
                this.pathPattern = parts[1].trim();
            } else {
                this.method = null;
                this.pathPattern = rawEndpoint;
            }
        }
    }
}
