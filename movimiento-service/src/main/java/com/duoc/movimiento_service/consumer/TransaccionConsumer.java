package com.duoc.movimiento_service.consumer;


import com.duoc.movimiento_service.event.TransaccionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransaccionConsumer {

    @KafkaListener(
            topics = "transacciones-bancarias",
            groupId = "movimiento-group"
    )
    public void consumir(TransaccionEvent event) {

        System.out.println(
                "Evento recibido desde Kafka -> "
                        + "Cuenta: " + event.getCuentaId()
                        + " | Tipo: " + event.getTipo()
                        + " | Monto: " + event.getMonto()
                        + " | Fecha: " + event.getFecha()
        );
    }
}
