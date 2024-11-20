package P4ComDis.Servidor;

import P4ComDis.utils.RabbitMQ;
import com.rabbitmq.client.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static P4ComDis.utils.Outros.debugPrint;
import static java.lang.System.exit;

public class Servidor{
    private Channel canle;
    private BufferedReader lectorArquivo;
    private final Map<String, Integer> clientes = new HashMap<>();
    private final String nomeColaSuscripcions;
    private ScheduledExecutorService scheduler;

    public Servidor(String arquivo, String IP, String nomeColaSuscripcions) {
        this.nomeColaSuscripcions = nomeColaSuscripcions;
        try {
            lectorArquivo = new BufferedReader(new InputStreamReader(new URL(arquivo).openStream()));

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IP);

            Connection conexion = factory.newConnection();
            canle = conexion.createChannel();

            // Declara o exchange de tipo fanout
            canle.exchangeDeclare("exchange_fanout", "fanout");

            // Declara a cola de suscripcións
            canle.queueDeclare(nomeColaSuscripcions, false, false, false, null);

            scheduler = Executors.newScheduledThreadPool(1);

        } catch (IOException | TimeoutException e) {
            System.err.println("Erro na inicialización do servidor: " + e.getMessage());
            exit(-1);
        }
    }

    private void xestionarSuscripcions() {
        try {
            // Procesar novas solicitudes de suscripción
            String dato = RabbitMQ.recibir(canle, nomeColaSuscripcions);
            while (dato != null) {
                String[] partes = dato.split(" ");
                String clienteID = partes[0];
                int tempo = Integer.parseInt(partes[1]);

                String nomeColaCliente = "cliente_" + clienteID;

                // Crear a cola se non existe e vinculala ao exchange
                canle.queueDeclare(nomeColaCliente, false, false, true, null);
                canle.queueBind(nomeColaCliente, "exchange_fanout", "");

                // Engadir ou renovar cliente
                clientes.put(clienteID, tempo);
                debugPrint("Suscripción aceptada para " + clienteID + ", tempo: " + tempo);
                dato = RabbitMQ.recibir(canle, nomeColaSuscripcions);
            }

            // Reducir o tempo restante dos clientes e eliminar os que expiraron
            Iterator<Map.Entry<String, Integer>> iterador = clientes.entrySet().iterator();
            while (iterador.hasNext()) {
                Map.Entry<String, Integer> entrada = iterador.next();
                String clienteID = entrada.getKey();
                int tempoRestante = entrada.getValue() - 1;

                if (tempoRestante < 0) {
                    // Tempo expirado: eliminar a cola e o cliente
                    String nomeColaCliente = "cliente_" + clienteID;
                    try {
                        canle.queueDelete(nomeColaCliente);
                        debugPrint("Cola eliminada para cliente " + clienteID);
                    } catch (IOException e) {
                        System.err.println("Erro eliminando cola para " + clienteID + ": " + e.getMessage());
                    }
                    iterador.remove();
                } else {
                    // Actualizar o tempo restante
                    entrada.setValue(tempoRestante);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro na xestión das suscripcións: " + e.getMessage());
        }
    }

    private void enviarDatos() {
        String dato = leerDato();
        try {
            // Publicar o dato no exchange de tipo fanout
            canle.basicPublish("exchange_fanout", "", null, dato.getBytes(StandardCharsets.UTF_8));
            debugPrint("Envío " + dato + " a todas as colas activas.");
        } catch (Exception e) {
            System.err.println("Erro enviando datos: " + e.getMessage());
        }
    }

    public void executar() {
        scheduler.scheduleAtFixedRate(() -> {
            xestionarSuscripcions();
            enviarDatos();
        }, 0, 1, TimeUnit.SECONDS);
    }

    private String leerDato() {
        try {
            return lectorArquivo.readLine();
        } catch (IOException e) {
            System.err.println("Erro na lectura dos datos: " + e.getMessage());
            return "0";
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Uso: java P4ComDis.Servidor.Servidor <arquivo> <IP> <nomeColaSuscripcions>");
            exit(1);
        }

        String arquivo = args[0];
        String IP = args[1];
        String nomeColaSuscripcions = args[2];

        new Servidor(arquivo,IP,nomeColaSuscripcions).executar();
    }
}
