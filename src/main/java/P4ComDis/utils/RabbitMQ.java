package P4ComDis.utils;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class RabbitMQ {

    // Función para enviar a mensaxe á cola, reutilizando conexión e canle
    public static void enviar(Channel channel, String cola, String mensaje) throws Exception {
        channel.queueDeclare(cola, false, false, false, null);
        channel.basicPublish("", cola, null, mensaje.getBytes(StandardCharsets.UTF_8));
    }
    // Función para recibir mensaxes dunha cola, reutilizando conexión e canle
    public static String recibir(Channel channel, String cola) throws Exception {
        // Asegurarnos de que a cola existe
        channel.queueDeclare(cola, false, false, false, null);

        // Obter unha mensaxe sen bloquear
        GetResponse response = channel.basicGet(cola, true); // `true` para auto-acknowledge
        if (response == null) {
            // Se non hai mensaxes, devolvemos null
            return null;
        }
        // Convertimos a mensaxe a String e a devolvemos
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }

    /**
     * Método bloqueante para recibir unha mensaxe de RabbitMQ
     *
     * @param channel   A canle conectado ao servidor RabbitMQ
     * @param queueName O nome da cola dende donde recibir mensaxes
     * @return A mensaxe recibida como unha cadea
     * @throws IOException En caso de erro na recepción de mensaxes
     */
    public static String recibirBloqueante(Channel channel, String queueName) throws IOException {
        // Asegúrate de que a cola existe antes de consumir
        channel.queueDeclare(queueName, false, false, false, null);

        // Cola para bloquear ata que vhegue unha mensaje
        BlockingQueue<String> responseQueue = new ArrayBlockingQueue<>(1);

        // Define o consumidor (callback para mensaxes)
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                responseQueue.put(message); // Coloca a mensaxe na cola
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Erro ao manexar a mensaxe recibida", e);
            }
        };

        // Consumir unha mensaxe da cola
        String tag = channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

        // Espera bloqueante cun timeout
        String result = null;
        try {
            result = responseQueue.poll(10, TimeUnit.SECONDS); // Espera por 10 segundos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (result == null) {
            // Timeout: manexar o caso cando non se recibe ningunha mensaxe
            System.err.println("Timeout: Non se recibiu mensaxe da cola " + queueName);
        }

        // Cancela o consumidor para non seguir recibindo mensaxes
        channel.basicCancel(tag);

        return result;
    }


}