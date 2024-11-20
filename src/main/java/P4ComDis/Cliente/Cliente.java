package P4ComDis.Cliente;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Random;

import static P4ComDis.utils.Outros.debugPrint;

public class Cliente {
    private Channel canle;
    private final String id;
    private final Controlador controlador;
    private final String nomeColaSuscripcions;
    private String nomeCola;

    public Cliente(Controlador controlador, String nomeColaSuscripcions, String IPservidor) {
        id = xerarID();
        this.controlador = controlador;
        this.nomeColaSuscripcions = nomeColaSuscripcions;

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IPservidor);

            Connection conexion = factory.newConnection();
            canle = conexion.createChannel();

            // Nome da cola do cliente
            nomeCola = "cliente_" + id;

            // Solicitar a suscripción ao servidor
            String mensaxe = id + " " + controlador.getTempoSuscripcion();
            canle.basicPublish("", nomeColaSuscripcions, null, mensaxe.getBytes(StandardCharsets.UTF_8));
            debugPrint("Solicitude de suscripción enviada: " + mensaxe);

            // Crear e configurar callback para recibir datos
            canle.queueDeclare(nomeCola, false, false, true, null);

            DeliverCallback callback = (consumerTag, delivery) -> {
                String mensaxeRecibida = new String(delivery.getBody(), StandardCharsets.UTF_8);
                float dato = Float.parseFloat(mensaxeRecibida);
                controlador.recibirDato(dato);
                debugPrint("Recibín o dato " + dato);
            };

            // Comezar a consumir datos
            canle.basicConsume(nomeCola, true, callback, consumerTag -> {});

        } catch (Exception e) {
            System.err.println("Erro na inicialización do cliente: " + e.getMessage());
            canle = null; // Evitar estado inconsistente
        }
    }

    /**
     * Modifica o tempo de suscripción do cliente enviando unha nova solicitude ao servidor.
     */
    public void modificarTempoSuscripcion() {
        if (canle == null) {
            System.err.println("Canle non inicializada. Non se pode modificar o tempo de suscripción.");
            return;
        }

        String mensaxe = id + " " + controlador.getTempoSuscripcion();
        try {
            canle.basicPublish("", nomeColaSuscripcions, null, mensaxe.getBytes(StandardCharsets.UTF_8));
            debugPrint("Tempo de suscripción modificado a " + controlador.getTempoSuscripcion() + " segundos.");
        } catch (IOException e) {
            System.err.println("Erro na modificación do tempo de suscripción: " + e.getMessage());
        }
    }

    /**
     * Finaliza a conexión do cliente co servidor e elimina recursos asociados.
     */
    public void rematar() {
        try {
            if (canle != null) {
                canle.queueDelete(nomeCola); // Eliminar a cola do cliente
                canle.close(); // Pechar a canle
            }
            debugPrint("Cliente rematado correctamente.");
        } catch (Exception e) {
            System.err.println("Erro ao rematar o cliente: " + e.getMessage());
        }
    }

    public static String xerarID() {
        String macAddress = getMacAddress();
        long timestamp = System.currentTimeMillis();
        int pid = getProcessID();
        int randomValue = new Random().nextInt(1_000_000);

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
                        sb.append(String.format("%02X", b));
                    }
                    return sb.toString();
                }
            }
        } catch (SocketException e) {
            System.err.println("Erro na obtención da dirección mac.");
        }
        return "UNKNOWN_MAC";
    }

    private static int getProcessID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        try {
            return Integer.parseInt(processName.split("@")[0]);
        } catch (NumberFormatException e) {
            return new Random().nextInt(1_000);
        }
    }
}
