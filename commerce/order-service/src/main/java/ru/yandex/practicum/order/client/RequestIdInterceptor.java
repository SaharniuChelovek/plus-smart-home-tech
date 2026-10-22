package ru.yandex.practicum.order.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class RequestIdInterceptor implements RequestInterceptor {

    private static final String HEADER = "X-Request-Id";

    @Override
    public void apply(RequestTemplate template) {
        String id = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            id = attrs.getRequest().getHeader(HEADER);
        }
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        template.header(HEADER, id);
    }
}
