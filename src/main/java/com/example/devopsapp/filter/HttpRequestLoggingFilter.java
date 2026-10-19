package com.example.devopsapp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = getPath(request);

        // Evita gerar logs para cada consulta do Prometheus.
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.nanoTime();
        String method = request.getMethod();
        String path = getPath(request);
        boolean exceptionOccurred = false;

        try {
            filterChain.doFilter(request, response);

        } catch (IOException | ServletException | RuntimeException exception) {
            exceptionOccurred = true;

            int status = response.getStatus() >= 400
                    ? response.getStatus()
                    : 500;

            logRequest(
                    method,
                    path,
                    status,
                    System.nanoTime() - startTime,
                    exception
            );

            throw exception;

        } finally {
            if (!exceptionOccurred) {
                logRequest(
                        method,
                        path,
                        response.getStatus(),
                        System.nanoTime() - startTime,
                        null
                );
            }
        }
    }

    private void logRequest(
            String method,
            String path,
            int status,
            long elapsedNanos,
            Throwable exception
    ) {
        long durationMs =
                TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

        String outcome;

        if (exception != null) {
            outcome = "exception";
        } else if (status >= 500) {
            outcome = "server_error";
        } else if (status >= 400) {
            outcome = "client_error";
        } else {
            outcome = "success";
        }

        // Esses valores podem ser encaminhados como campos do GELF.
        MDC.put("http_method", method);
        MDC.put("http_path", path);
        MDC.put("http_status", String.valueOf(status));
        MDC.put("http_duration_ms", String.valueOf(durationMs));
        MDC.put("http_outcome", outcome);

        try {
            String message = """
                    http_request method=%s path=%s status=%d duration_ms=%d outcome=%s
                    """.strip().replace("\n", "");

            if (exception != null || status >= 500) {
                logger.error(
                        message.formatted(
                                method, path, status, durationMs, outcome
                        ),
                        exception
                );
            } else if (status >= 400) {
                logger.warn(
                        message.formatted(
                                method, path, status, durationMs, outcome
                        )
                );
            } else {
                logger.info(
                        message.formatted(
                                method, path, status, durationMs, outcome
                        )
                );
            }
        } finally {
            MDC.remove("http_method");
            MDC.remove("http_path");
            MDC.remove("http_status");
            MDC.remove("http_duration_ms");
            MDC.remove("http_outcome");
        }
    }

    private String getPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        return requestUri.substring(contextPath.length());
    }
}