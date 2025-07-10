package com.medco.HealthConnectProvider.config.interceptor;

import com.medco.HealthConnectProvider.annotation.RequiresApiKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("${api.key}")
    private String apiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiresApiKey requiresApiKey = handlerMethod.getMethodAnnotation(RequiresApiKey.class);

            if (requiresApiKey != null) {
                String providedApiKey = request.getHeader("X-API-Key");
                if (providedApiKey == null) {
                    providedApiKey = request.getParameter("X-API-Key");
                }
                if (providedApiKey == null || !providedApiKey.equals(apiKey)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid API Key");
                    return false;
                }
            }
        }
        return true;
    }
}
