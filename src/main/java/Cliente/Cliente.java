package Cliente;

import static movidasDeMensajes.RabbitMQ.*;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Random;
import java.util.UUID;

public class Cliente{
    private Channel canle;
    private final String id;
    private final Controlador controlador;
    private final String nomeColaSuscripcions;

    public Cliente(Controlador controlador, String nomeColaSuscripcions, String IPservidor){
        id=xerarID();
        this.controlador=controlador;
        this.nomeColaSuscripcions=nomeColaSuscripcions;
        try{
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(IPservidor);

                try (Connection conexion = factory.newConnection()) {
                    canle = conexion.createChannel();
                }
        } catch (Exception e) {
            System.err.println("Erro na inicialización do servidor: " + e.getMessage());
        }
    }

    public void modificarTempoSuscripcion(int tempo){
        String mensaxe=id+" "+ tempo;
        try {
            enviar(canle, nomeColaSuscripcions, mensaxe);
        }catch (Exception e){
            System.err.println("Erro na renovación do tempo de suscripción: "+e.getMessage());
        }
    }

    public static String xerarID() {
        return UUID.randomUUID().toString();
    }

    private class FioCliente implements Runnable{
        public boolean sigho=true;
        private Channel canle;
        private final String id;
        private final Controlador controlador;

        private FioCliente(Channel canle, String id, Controlador controlador) {
            this.canle = canle;
            this.id = id;
            this.controlador = controlador;
        }

        @Override
        public void run() {
            while(sigho){
                try {
                    float dato= Float.parseFloat(recibirBloqueante(canle,id));
                    //TODO Controlador.mostrarDato(dato);
                } catch (Exception e) {
                    System.err.println("Erro na recepción dos datos: "+e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
