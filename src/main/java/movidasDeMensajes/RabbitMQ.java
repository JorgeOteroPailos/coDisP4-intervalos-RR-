package movidasDeMensajes;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

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

    public static String recibirBloqueante(Channel channel, String cola) throws Exception {
        // Asegurarnos de que la cola existe
        channel.queueDeclare(cola, false, false, false, null);

        // CountDownLatch para bloquear hasta que llegue un mensaje
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] mensajeRecibido = new String[1];  // Usamos un arreglo para poder asignar el valor desde el callback

        // Crear un DeliverCallback que se ejecuta cuando un mensaje es recibido
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            // Cuando se recibe un mensaje, lo convertimos y lo almacenamos
            mensajeRecibido[0] = new String(delivery.getBody(), StandardCharsets.UTF_8);
            latch.countDown();  // Disminuimos el contador, permitiendo continuar la ejecución
        };

        // Empezamos a consumir la cola de forma bloqueante
        channel.basicConsume(cola, true, deliverCallback, consumerTag -> {});

        // Esperamos hasta que un mensaje sea recibido (el CountDownLatch se reduce a 0)
        latch.await();

        // Retornamos el mensaje recibido
        return mensajeRecibido[0];
    }
}