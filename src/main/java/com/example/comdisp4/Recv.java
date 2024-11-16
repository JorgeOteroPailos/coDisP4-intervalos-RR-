package com.example.comdisp4;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Recv {
    // se define el nombre de la cola
    private final static String QUEUE_NAME = "hola";

    public static void main(String[] argv) throws Exception {
        // creamos la conexión al servidor
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Esperando mensajes, para salir pulsa CTRL-C");
        // Almacena los objetos en un buffer hasta que sean utilizados
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Recibido '" + message + "'");
        };
        // consume mensaaje de la cola
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
        });
    }
}