package P4ComDis.utils;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class RabbitMQ {

    // Función para enviar a mensaxe á cola, reutilizando conexión e canle
    public static void enviar(Channel channel, String cola, String mensaje) throws Exception {
        channel.queueDeclare(cola, false, false, false, null);
        channel.basicPublish("", cola, null, mensaje.getBytes(StandardCharsets.UTF_8));
    }
    // Función para recibir mensaxes dunha cola, reutilizando conexión e canle
    public static String recibir(Channel channel, String cola) {
        try {
            // Asegurarnos de que a cola existe
            channel.queueDeclare(cola, false, false, false, null);

            // Obter un mensaje sin bloquear
            GetResponse response = channel.basicGet(cola, true); // `true` para auto-acknowledge
            if (response == null) {
                // Se non hai mensaxes, devolvemos null
                return null;
            }

            // Convertimos a mensaxe a String e a devolvemos
            return new String(response.getBody(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error al recibir mensaje de la cola: " + e.getMessage());
            // Aquí podes facer un retry, rexistrar el erro, ou devolver un valor por defecto
            return null; // Ou manexar o caso de erro de otra maneira
        } catch (Exception e) {
            System.err.println("Error inesperado al recibir mensaje de la cola: " + e.getMessage());
            return null;
        }
    }

    /**
     * Método bloqueante para recibir unha mensaxe de RabbitMQ
     *
     * @param channel   A canle conectado ao servidor RabbitMQ
     * @param queueName O nome da cola dende donde recibir mensaxes
     * @return A mensaxe recibida como unha cadena
     * @throws IOException En caso de erro na recepción de mensaxes
     * @throws InterruptedException En caso de interrupción durante o bloqueo
     */
    public static String recibirBloqueante(Channel channel, String queueName) throws IOException, InterruptedException {
        // Asegúrate de que a cola existe antes de consumir
        channel.queueDeclare(queueName, false, false, false, null);

        // Cola para bloquear hasta que chegue unha mensaxe
        BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(1);

        // Define o consumidor (callback para mensaxes)
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                responseQueue.put(message); // Coloca a mensaxe na cola
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Erro o manexar a mensaxe recibido", e);
            }
        };

        // Consumir unha mensaxe da cola
        String tag = channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

        // Espera bloqueante para recibir unha mensaxe
        String result = responseQueue.take();

        // Cancela o consumidor para non seguir recibiendo mensaxes
        channel.basicCancel(tag);

        return result;
    }

}