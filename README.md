# RoboFab

**Autor:** braco96

## ¿Qué es?
Un simulador de fábrica que ensambla robots usando **concurrencia en Java**. Utilizo un `ThreadPoolExecutor` con una `BlockingQueue` acotada para procesar órdenes de forma determinista y evitar saturación. El inventario se gestiona con `ConcurrentHashMap` y operaciones atómicas para **evitar indeterminación** y condiciones de carrera.

## Puntos clave
- **Backpressure**: la cola acotada evita que el sistema se inunde de trabajo.
- **Política de rechazo determinista** (`CallerRunsPolicy`): bajo presión, la tarea se ejecuta en el hilo llamante.
- **Reserva atómica**: uso de `compute/merge` para descontar piezas sin carreras.
- **Apagado ordenado**: `shutdown()` + `awaitTermination()`.

## Ejecutar
```bash
# Desde la carpeta del proyecto
bash run.sh
```

## Estructura
```
src/main/java/braco96/robofab/FactoryApp.java
README.md
MEMORIA.md
run.sh
```