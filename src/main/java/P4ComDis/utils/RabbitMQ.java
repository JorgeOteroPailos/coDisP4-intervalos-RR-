package P4ComDis.utils;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public abstract class RabbitMQ {


    // Función para recibir mensaxes dunha cola, reutilizando conexión e canle
    public static String recibir(Channel channel, String cola) throws Exception {
        // Obter unha mensaxe sen bloquear
        GetResponse response = channel.basicGet(cola, true); // `true` para auto-acknowledge
        if (response == null) {
            // Se non hai mensaxes, devolvemos null
            return null;
        }
        // Convertimos a mensaxe a String e a devolvemos
        return new String(response.getBody(), StandardCharsets.UTF_8);
    }


}