package com.example.comdisp4;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class Send {
    // se define el nombre de la cola
    private final static String QUEUE_NAME = "hola";

    public static void main(String[] argv) throws Exception {
        // creamos la conexión al servidor
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            // deeclaramos una cola a la que enviar
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // el mensaje a enviar
            String message = "Hola Mundo!";
            // publicamos el mensaje en la cola
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            // imprimimos que haa sido enviado
            System.out.println(" [x] Enviado '" + message + "'");
        }
    }
}