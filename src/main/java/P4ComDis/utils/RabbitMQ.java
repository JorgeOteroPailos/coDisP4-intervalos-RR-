package P4ComDis.utils;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class RabbitMQ {

    // Función para enviar el mensaje a la cola, reutilizando conexión y canal
    public static void enviar(Channel channel, String cola, String mensaje) throws Exception {
        channel.queueDeclare(cola, false, false, false, null);
        channel.basicPublish("", cola, null, mensaje.getBytes(StandardCharsets.UTF_8));
    }
    // Función para recibir mensajes de una cola, reutilizando conexión y canal
    public static String recibir(Channel channel, String cola) throws Exception {
        // Asegurarnos de que la cola existe
        channel.queueDeclare(cola, false, false, false, null);

        // Obtener un mensaje sin bloquear
        GetResponse response = channel.basicGet(cola, true); // `true` para auto-acknowledge
        if (response == null) {
            // Si no hay mensajes, devolvemos null
            return null;
        }
        // Convertimos el mensaje a String y lo devolvemos
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    /**
     * Método bloqueante para recibir un mensaje de RabbitMQ
     *
     * @param channel   El canal conectado al servidor RabbitMQ
     * @param queueName El nombre de la cola desde donde recibir mensajes
     * @return El mensaje recibido como una cadena
     * @throws IOException En caso de error en la recepción de mensajes
     * @throws InterruptedException En caso de interrupción durante el bloqueo
     */
    public static String recibirBloqueante(Channel channel, String queueName) throws IOException, InterruptedException {
        // Asegúrate de que la cola existe antes de consumir
        channel.queueDeclare(queueName, true, false, false, null);

        // Cola para bloquear hasta que llegue un mensaje
        BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(1);

        // Define el consumidor (callback para mensajes)
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            try {
                responseQueue.put(message); // Coloca el mensaje en la cola
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error al manejar el mensaje recibido", e);
            }
        };

        // Consumir un mensaje de la cola
        String tag = channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

        // Espera bloqueante para recibir un mensaje
        String result = responseQueue.take();

        // Cancela el consumidor para no seguir recibiendo mensajes
        channel.basicCancel(tag);

        return result;
    }

}