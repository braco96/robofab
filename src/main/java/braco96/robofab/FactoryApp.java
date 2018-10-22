package braco96.robofab;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Proyecto: RoboFab
 * Autor: braco96
 *
 * Idea: una "fábrica" de robots que ensambla productos a partir de piezas.
 * Uso de un ThreadPool (ExecutorService) + BlockingQueue para garantizar orden
 * de entrada y evitar condiciones de carrera al acceder al inventario.
 */
public class FactoryApp {
    // Inventario concurrente y seguro
    private final ConcurrentMap<String, Integer> inventory = new ConcurrentHashMap<>();

    // Cola de tareas acotada: ayuda a controlar la presión (backpressure)
    private final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(64);

    // Pool de hilos con política de rechazo controlada
    private final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    4, // núcleos
                    8, // máximo
                    60, TimeUnit.SECONDS,
                    workQueue,
                    new ThreadPoolExecutor.CallerRunsPolicy() // fallback determinista bajo presión
            );

    private final Random rnd = new Random();

    public static void main(String[] args) throws Exception {
        new FactoryApp().runDemo();
    }

    private void runDemo() throws Exception {
        // Cargar inventario inicial de forma segura
        putIfAbsent("Tornillo", 100);
        putIfAbsent("Chapa", 100);
        putIfAbsent("Chip", 100);

        // Crear un conjunto de "órdenes" de ensamblaje
        List<AssemblyOrder> orders = List.of(
                new AssemblyOrder("Robo-A", 10, 5, 3),
                new AssemblyOrder("Robo-B", 8, 3, 6),
                new AssemblyOrder("Robo-C", 5, 4, 2),
                new AssemblyOrder("Robo-D", 12, 6, 5)
        );

        // Encolar tareas de forma determinista según el orden de llegada
        for (AssemblyOrder order : orders) {
            executor.execute(new AssemblyTask(order));
        }

        // Cerrar el pool de forma ordenada
        executor.shutdown();
        boolean ok = executor.awaitTermination(5, TimeUnit.MINUTES);
        if (!ok) {
            System.err.println("Tiempo agotado esperando a que termine el ensamblaje.");
        }

        System.out.println("\n=== Inventario final ===");
        inventory.forEach((k, v) -> System.out.printf("%s: %d%n", k, v));
        System.out.println("Demo finalizada.");
    }

    private void putIfAbsent(String material, int qty) {
        inventory.merge(material, qty, Integer::sum);
    }

    // Tarea que consume piezas y "ensambla" un robot
    private class AssemblyTask implements Runnable {
        private final AssemblyOrder order;

        AssemblyTask(AssemblyOrder order) {
            this.order = order;
        }

        @Override
        public void run() {
            // Bloque crítico: reserva de materiales
            boolean reserved = reserveMaterials(order);
            if (!reserved) {
                System.err.printf("No hay materiales para %s. Reintento controlado...%n", order.productName);
                // Backoff determinista
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                reserved = reserveMaterials(order);
                if (!reserved) {
                    System.err.printf("Fallo definitivo al reservar para %s%n", order.productName);
                    return;
                }
            }

            try {
                // "Proceso" de ensamblaje simulado
                int workTimeMs = 150 + rnd.nextInt(250);
                Thread.sleep(workTimeMs);
                System.out.printf("✔ Ensamblado %s en %d ms por %s%n",
                        order.productName, workTimeMs, Thread.currentThread().getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Reserva atómica de materiales usando operaciones de mapa atómicas
    private boolean reserveMaterials(AssemblyOrder order) {
        return inventory.compute("Tornillo", (k, v) -> {
            if (v == null || v < order.screws) return v;
            int afterScrews = v - order.screws;
            // Recalcular otras piezas de manera segura
            Integer chapa = inventory.get("Chapa");
            Integer chip = inventory.get("Chip");
            if (chapa == null || chip == null) return v; // aborta

            if (chapa < order.plates || chip < order.chips) return v; // sin stock

            // OK: aplicamos las restas
            inventory.put("Chapa", chapa - order.plates);
            inventory.put("Chip", chip - order.chips);
            return afterScrews;
        }) != null; // compute devuelve el nuevo valor o null
    }

    // Definición simple de una orden de ensamblaje
    private record AssemblyOrder(String productName, int screws, int plates, int chips) { }
}