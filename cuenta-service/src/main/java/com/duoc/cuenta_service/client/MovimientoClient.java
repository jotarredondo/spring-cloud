package com.duoc.cuenta_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import com.duoc.cuenta_service.dto.MovimientoFallbackResponse;

import java.util.Arrays;

@Component
public class MovimientoClient {

    private final RestTemplate restTemplate;

    public MovimientoClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(
            name = "movimientoService",
            fallbackMethod = "movimientoFallback"
    )
    public Object getMovimientos(Long accountId) {

        Object[] response = restTemplate.getForObject(
                "http://MOVIMIENTO-SERVICE/api/movimientos/cuenta/" + accountId,
                Object[].class
        );

        return response != null
                ? Arrays.asList(response)
                : List.of();
    }

    public Object movimientoFallback(Long accountId, Throwable ex) {

        return new MovimientoFallbackResponse(
                "Movimiento Service no disponible",
                List.of()
        );
    }
}
