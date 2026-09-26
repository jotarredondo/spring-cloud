package com.duoc.transaccion_service.controller;

import com.duoc.transaccion_service.event.TransaccionEvent;
import com.duoc.transaccion_service.producer.TransaccionProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionProducer producer;

    public TransaccionController(TransaccionProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<String> crear(
            @RequestBody TransaccionEvent event) {

        event.setFecha(LocalDateTime.now());

        producer.enviarTransaccion(event);

        return ResponseEntity.ok(
                "Transaccion enviada a Kafka"
        );
    }
}
