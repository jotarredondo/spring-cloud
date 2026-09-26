# Sistema Bancario con Microservicios, Spring Cloud y Apache Kafka

Proyecto desarrollado para la asignatura **Desarrollo Backend III – Duoc UC**.

La solución implementa una arquitectura basada en microservicios utilizando **Spring Boot, Spring Cloud, Eureka, Config Server, Resilience4j y Apache Kafka**, permitiendo comunicación síncrona y asíncrona entre los distintos servicios del sistema bancario.

Durante la Semana 6 se implementaron mecanismos de descubrimiento de servicios, configuración centralizada, seguridad y tolerancia a fallos.

Durante la Semana 7 se extendió la arquitectura incorporando comunicación asíncrona orientada a eventos mediante Apache Kafka.

---

## Objetivo

El objetivo del proyecto es implementar una arquitectura distribuida de microservicios que permita:

- Centralizar la configuración de los microservicios mediante Spring Cloud Config.
- Registrar y descubrir servicios utilizando Eureka.
- Implementar comunicación entre microservicios.
- Incorporar autenticación mediante Spring Security.
- Implementar tolerancia a fallos mediante Resilience4j.
- Incorporar una arquitectura orientada a eventos.
- Publicar y consumir eventos de manera asíncrona mediante Apache Kafka.
- Permitir escalabilidad mediante particiones y grupos de consumidores.

---

## Arquitectura general

El proyecto está compuesto por los siguientes componentes:

| Componente | Puerto | Función |
|---|---:|---|
| Eureka Server | 8761 | Registro y descubrimiento de microservicios |
| Config Server | 8888 | Configuración centralizada |
| cuenta-service | 8081 | Gestión de cuentas y consulta de movimientos |
| movimiento-service | 8082 | Gestión y consumo de movimientos |
| transaccion-service | 8083 | Producción de eventos de transacciones |
| Kafka UI | 8090 | Administración y visualización de Kafka |
| Kafka Broker 1 | 29092 | Broker Kafka |
| Kafka Broker 2 | 39092 | Broker Kafka |
| Kafka Broker 3 | 49092 | Broker Kafka |

---

## Estructura del proyecto

```text
proyecto/
│
├── eureka-server/
│
├── config-server/
│
├── config-repo/
│   ├── cuenta-service.yml
│   ├── movimiento-service.yml
│   └── transaccion-service.yml
│
├── cuenta-service/
│
├── movimiento-service/
│
├── transaccion-service/
│
├── kafka/
│   └── docker-compose.yml
│
├── docs/
│   └── arquitectura-eventos.png
│
└── README.md
```

---

# Spring Cloud

## Eureka Server

Eureka permite que los microservicios se registren dinámicamente y puedan localizarse utilizando su nombre lógico en lugar de depender directamente de direcciones IP o puertos.

El servidor Eureka se ejecuta en:

```text
http://localhost:8761
```

Los servicios registrados son:

```text
CUENTA-SERVICE
MOVIMIENTO-SERVICE
TRANSACCION-SERVICE
```

---

## Config Server

Spring Cloud Config Server centraliza la configuración de los microservicios.

Se ejecuta en:

```text
http://localhost:8888
```

Las configuraciones se encuentran en:

```text
config-repo/
```

Ejemplo:

```text
config-repo/movimiento-service.yml
```

Para verificar la configuración entregada a un servicio puede utilizarse:

```text
GET http://localhost:8888/movimiento-service/default
```

---

# Microservicios

## cuenta-service

Puerto:

```text
8081
```

Este microservicio permite gestionar cuentas y consultar sus movimientos.

Además, se comunica con `movimiento-service` mediante descubrimiento de servicios proporcionado por Eureka.

Ejemplo:

```http
GET http://localhost:8081/api/cuentas/103/movimientos
```

La comunicación utiliza el nombre lógico:

```text
MOVIMIENTO-SERVICE
```

evitando utilizar directamente una dirección física del servicio.

---

## movimiento-service

Puerto:

```text
8082
```

Este servicio administra los movimientos bancarios almacenados en MySQL.

Durante la Semana 7 se agregó además un consumidor Kafka encargado de escuchar eventos provenientes del topic:

```text
transacciones-bancarias
```

El consumidor pertenece al grupo:

```text
movimiento-group
```

Cuando recibe un evento se procesa de manera asíncrona.

Ejemplo de salida:

```text
Evento recibido desde Kafka ->
Cuenta: 103 |
Tipo: RETIRO |
Monto: 2500 |
Fecha: 2026-09-26T10:01:26
```

---

## transaccion-service

Puerto:

```text
8083
```

Este microservicio fue incorporado como productor de eventos Kafka.

Recibe una transacción mediante una petición HTTP y publica un evento en Kafka.

Endpoint:

```http
POST http://localhost:8083/api/transacciones
```

Ejemplo de petición:

```json
{
  "cuentaId": 103,
  "tipo": "RETIRO",
  "monto": 2500
}
```

Respuesta:

```text
Transaccion enviada a Kafka
```

El servicio agrega automáticamente la fecha de la transacción antes de publicar el evento.

---

# Arquitectura orientada a eventos

La solución utiliza una arquitectura orientada a eventos basada en Apache Kafka.

`transaccion-service` actúa como **productor**, mientras que `movimiento-service` actúa como **consumidor**.

El flujo principal es:

```text
transaccion-service
        |
        | TransaccionEvent
        v
Apache Kafka
Topic: transacciones-bancarias
        |
        | movimiento-group
        v
movimiento-service
```

El diagrama completo de arquitectura se encuentra en:

```text
docs/arquitectura-eventos.png
```

![Arquitectura orientada a eventos](docs/arquitectura-eventos.png)

---

## Evento utilizado

Los eventos enviados a Kafka utilizan la siguiente estructura:

```json
{
  "cuentaId": 103,
  "tipo": "RETIRO",
  "monto": 2500,
  "fecha": "2026-09-26T10:01:26"
}
```

La clase utilizada para representar el evento es:

```text
TransaccionEvent
```

Sus principales atributos son:

```text
cuentaId
tipo
monto
fecha
```

---

# Apache Kafka

Kafka se ejecuta mediante Docker Compose.

La infraestructura utilizada contiene:

```text
3 ZooKeeper
3 Kafka Brokers
1 Kafka UI
```

Kafka UI se encuentra disponible en:

```text
http://localhost:8090
```

---

## Topic

El topic utilizado por el sistema es:

```text
transacciones-bancarias
```

Configuración:

```text
Partitions: 3
Replication Factor: 2
```

Esta configuración permite distribuir los mensajes entre diferentes particiones y mantener réplicas del contenido en distintos brokers.

---

## Creación del topic

El topic puede crearse ejecutando:

```bash
docker exec -it kafka-1 kafka-topics \
  --create \
  --topic transacciones-bancarias \
  --bootstrap-server kafka-1:9092 \
  --partitions 3 \
  --replication-factor 2
```

Para revisar su configuración:

```bash
docker exec -it kafka-1 kafka-topics \
  --describe \
  --topic transacciones-bancarias \
  --bootstrap-server kafka-1:9092
```

---

# Flujo de comunicación asíncrona

El flujo implementado funciona de la siguiente forma:

1. Un cliente realiza una petición HTTP a `transaccion-service`.

2. `transaccion-service` crea un objeto `TransaccionEvent`.

3. El evento se publica en:

```text
transacciones-bancarias
```

4. Kafka almacena el evento en una de las tres particiones disponibles.

5. `movimiento-service` permanece suscrito al topic.

6. El grupo:

```text
movimiento-group
```

recibe y procesa los eventos.

7. La comunicación ocurre de manera asíncrona, por lo que el productor y consumidor no necesitan ejecutarse de manera sincronizada.

---

# Tolerancia a fallos con Resilience4j

La comunicación síncrona entre `cuenta-service` y `movimiento-service` utiliza Resilience4j para proporcionar tolerancia a fallos.

Si `movimiento-service` no está disponible, `cuenta-service` utiliza un mecanismo de fallback.

Ejemplo:

```json
{
  "mensaje": "Movimiento Service no disponible",
  "movimientos": []
}
```

Esto permite que el servicio principal continúe respondiendo aunque una dependencia se encuentre temporalmente fuera de servicio.

---

# Seguridad

Los microservicios incorporan Spring Security.

Los endpoints protegidos utilizan autenticación HTTP Basic.

Para efectos de desarrollo y pruebas académicas se utilizan credenciales configuradas localmente.

> Las credenciales y contraseñas reales no deben almacenarse públicamente en el repositorio.

Ejemplo en Postman:

```text
Authorization
Type: Basic Auth
```

Una petición sin credenciales válidas genera:

```text
401 Unauthorized
```

Mientras que una petición correctamente autenticada permite acceder al recurso.

---

# Base de datos

`movimiento-service` utiliza MySQL.

Base de datos utilizada:

```text
banco_bff
```

La configuración de conexión se administra mediante Config Server.

Ejemplo:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banco_bff
    username: root
```

La contraseña debe mantenerse fuera de repositorios públicos.

---

# Requisitos

Para ejecutar el proyecto es necesario disponer de:

```text
Java 21
Maven
Docker Desktop
MySQL 8
Git
Postman
```

También se recomienda utilizar:

```text
IntelliJ IDEA
```

---

# Ejecución del proyecto

## 1. Iniciar MySQL

Verificar que MySQL se encuentre ejecutándose y que exista la base de datos:

```text
banco_bff
```

---

## 2. Iniciar Kafka

Desde la carpeta:

```text
kafka/
```

ejecutar:

```bash
docker compose up -d
```

Verificar los contenedores:

```bash
docker ps
```

Luego ingresar a:

```text
http://localhost:8090
```

y comprobar que los tres brokers estén disponibles.

---

## 3. Crear el topic

Si el topic todavía no existe:

```bash
docker exec -it kafka-1 kafka-topics \
  --create \
  --topic transacciones-bancarias \
  --bootstrap-server kafka-1:9092 \
  --partitions 3 \
  --replication-factor 2
```

---

## 4. Iniciar Eureka Server

Ejecutar:

```text
eureka-server
```

Verificar:

```text
http://localhost:8761
```

---

## 5. Iniciar Config Server

Ejecutar:

```text
config-server
```

Verificar:

```text
http://localhost:8888
```

---

## 6. Iniciar movimiento-service

Ejecutar:

```text
movimiento-service
```

Puerto:

```text
8082
```

Al iniciar correctamente debería conectarse a Kafka utilizando:

```text
movimiento-group
```

---

## 7. Iniciar cuenta-service

Ejecutar:

```text
cuenta-service
```

Puerto:

```text
8081
```

---

## 8. Iniciar transaccion-service

Ejecutar:

```text
transaccion-service
```

Puerto:

```text
8083
```

---

# Prueba de Kafka

Realizar:

```http
POST http://localhost:8083/api/transacciones
```

Body:

```json
{
  "cuentaId": 103,
  "tipo": "RETIRO",
  "monto": 2500
}
```

El productor debe responder:

```text
Transaccion enviada a Kafka
```

Posteriormente, en la consola de `movimiento-service` debería visualizarse:

```text
Evento recibido desde Kafka ->
Cuenta: 103 |
Tipo: RETIRO |
Monto: 2500 |
Fecha: ...
```

El evento también puede visualizarse desde Kafka UI:

```text
Topics
→ transacciones-bancarias
→ Messages
```

---

# Prueba de tolerancia a fallos

Con todos los servicios funcionando:

```http
GET http://localhost:8081/api/cuentas/103/movimientos
```

La respuesta contiene los movimientos asociados a la cuenta.

Posteriormente se puede detener `movimiento-service` y repetir la petición.

Resilience4j activa el fallback:

```json
{
  "mensaje": "Movimiento Service no disponible",
  "movimientos": []
}
```

---

# Escalabilidad

La infraestructura Kafka utiliza:

```text
3 brokers
3 particiones
Replication Factor: 2
```

La utilización de particiones permite distribuir eventos y posibilita que varios consumidores pertenecientes al mismo consumer group puedan procesar mensajes en paralelo.

Kafka permite además mantener diferentes consumer groups para que múltiples microservicios puedan reaccionar independientemente ante un mismo evento.

---

# Tecnologías utilizadas

- Java 21
- Spring Boot 4
- Spring Cloud
- Spring Cloud Config
- Netflix Eureka
- Spring Security
- Resilience4j
- Apache Kafka
- Spring Kafka
- Docker
- Docker Compose
- Kafka UI
- MySQL
- Maven
- Postman
- Git
- GitHub

---

