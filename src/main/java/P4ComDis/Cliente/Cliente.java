package P4ComDis.Cliente;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

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
        String macAddress = getMacAddress();
        long timestamp = System.currentTimeMillis();
        int pid = getProcessID();
        int randomValue = new Random().nextInt(1_000_000); // Número aleatorio entre 0 y 999999

        // Combina os componentes nun só identificador
        return String.format("%s-%d-%d-%06d", macAddress, timestamp, pid, randomValue);
    }

    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni != null && ni.getHardwareAddress() != null && ni.isUp()) {
                    byte[] mac = ni.getHardwareAddress();
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b)); // Convirte cada byte nun formato hexadecimal
                    }
                    return sb.toString();
                }
            }
        }catch (SocketException e) {
            System.err.println("Erro na obtención da dirección mac, xerando ID con dirección mac por defecto.");
        }
        return "UNKNOWN_MAC";
    }

    private static int getProcessID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        // O formato típico de `getName()` é "PID@hostname"
        try {
            return Integer.parseInt(processName.split("@")[0]);
        } catch (NumberFormatException e) {
            return new Random().nextInt(1_000); // Se non se pode obter o PID, usa un número aleatorio
        }
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
