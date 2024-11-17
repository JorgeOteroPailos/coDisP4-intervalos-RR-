package P4ComDis.Cliente;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.UUID;

import static P4ComDis.utils.Outros.debugPrint;
import static P4ComDis.utils.RabbitMQ.enviar;
import static P4ComDis.utils.RabbitMQ.recibirBloqueante;

public class Cliente{
    private Channel canle;
    private final String id;
    private final Controlador controlador;
    private final String nomeColaSuscripcions;

    private FioCliente fio;

    public Cliente(Controlador controlador, String nomeColaSuscripcions, String IPservidor){
        id=xerarID();
        this.controlador=controlador;
        this.nomeColaSuscripcions=nomeColaSuscripcions;
        try{
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IPservidor);

            Connection conexion = factory.newConnection();
            canle = conexion.createChannel();
            modificarTempoSuscripcion(controlador.getTempoSuscripcion());

        } catch (Exception e) {
            System.err.println("Erro na inicialización do servidor: " + e.getMessage());
            return;
        }
        run();
    }

    public void rematar(){
        if(fio!=null){
            fio.sigho=false;
        }
        modificarTempoSuscripcion(0);
    }

    private void modificarTempoSuscripcion(int tempo){
        String mensaxe=id+" "+ tempo;
        try {
            enviar(canle, nomeColaSuscripcions, mensaxe);
        }catch (Exception e){
            System.err.println("Erro na renovación do tempo de suscripción: "+e.getMessage());
        }
    }

    public void modificarTempoSuscripcion(){
        modificarTempoSuscripcion(controlador.getTempoSuscripcion());
    }

    public void run(){
        fio=new FioCliente(this);
        new Thread(fio).start();
    }

    public static String xerarID() {
        return UUID.randomUUID().toString();
    }

    private static class FioCliente implements Runnable{
        public boolean sigho=true;

        private final Cliente cliente;

        private FioCliente(Cliente cliente) {
            this.cliente=cliente;
        }

        @Override
        public void run() {
            while(sigho){
                try {
                    float dato= Float.parseFloat(recibirBloqueante(cliente.canle,"cliente_"+cliente.id));
                    cliente.controlador.recibirDato(dato);
                    debugPrint("Recibín o dato "+dato);
                } catch (Exception e) {
                    System.err.println("Erro na recepción dos datos: "+e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
