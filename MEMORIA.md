# Memoria técnica – RoboFab

Soy **braco96** y en este proyecto trabajé patrones concurrentes para controlar el flujo de trabajo y evitar indeterminación.

## Objetivo
Procesar órdenes de ensamblaje con seguridad sobre un inventario compartido.

## Decisiones de diseño
- **ExecutorService** con `ThreadPoolExecutor` (4–8 hilos) y cola acotada. El tamaño de la cola limita la latencia y estabiliza el sistema.
- **CallerRunsPolicy** como fallback: reduce el sesgo de planificación y suaviza picos.
- **Estructuras concurrentes** (`ConcurrentHashMap`, `BlockingQueue`). Las operaciones de reserva se realizan con `compute` y `merge` para asegurar atomicidad.
- **Sección crítica minimizada**: la parte que toca stock es breve; el “ensamblaje” se hace fuera para no bloquear.

## Evitando la indeterminación
- Todas las mutaciones de inventario son atómicas.
- El orden de entrada se conserva en la cola; no hay polling activo.
- Terminación controlada con `awaitTermination`.

## Posibles extensiones
- Métricas (JFR/Micrometer).
- Persistencia del stock.
- Reintentos con backoff exponencial.