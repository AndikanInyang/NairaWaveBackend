package com.Vencedora.NairaWave.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.Vencedora.NairaWave.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RequestCounter> limitMap = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final int MAX_REQUESTS = 5;
    // 15 Minutes in milliseconds (15 * 60 * 1000)
    private static final long TIME_WINDOW_MS = 15L * 60L * 1000L;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Rate limit sensitive authentication endpoints against brute force
        if (path.contains("/api/v1/auth/login") || path.contains("/api/v1/auth/register")) {
            String clientIp = getClientIp(request);
            long now = System.currentTimeMillis();

            RequestCounter counter = limitMap.computeIfAbsent(clientIp, k -> new RequestCounter(now));

            synchronized (counter) {
                // Reset counter if window has expired
                if (now - counter.windowStart > TIME_WINDOW_MS) {
                    counter.windowStart = now;
                    counter.count = 0;
                }

                counter.count++;

                // If exceeded maximum allowed requests within window, return HTTP 429
                if (counter.count > MAX_REQUESTS) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");

                    ApiResponse<Void> apiResponse = ApiResponse.error(
                            "Too many attempts from your IP. Please wait 15 minutes before retrying."
                    );
                    response.getWriter().write(mapper.writeValueAsString(apiResponse));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class RequestCounter {
        long windowStart;
        int count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }
    }
}