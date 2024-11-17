package P4ComDis.Servidor;

import P4ComDis.utils.RabbitMQ;
import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static P4ComDis.utils.Outros.debugPrint;

public class Servidor implements Runnable {
    private Channel canle;
    private BufferedReader lectorArquivo;
    private HashMap<String, Integer> clientes;
    private String nomeColaSuscripcions;

    private ScheduledExecutorService scheduler;

    public Servidor(String arquivo, String IP, String nomeColaSuscripcions) {
        try {
            lectorArquivo = new BufferedReader(new InputStreamReader(new URL(arquivo).openStream()));
            clientes = new HashMap<>();
            this.nomeColaSuscripcions = nomeColaSuscripcions;

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IP);

            Connection conexion = factory.newConnection();
            canle = conexion.createChannel();
            canle.queueDeclare(nomeColaSuscripcions, false, false, false, null);

            scheduler = Executors.newScheduledThreadPool(1); // Inicializamos o programador

        } catch (Exception e) {
            System.err.println("Erro na inicialización do servidor: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        scheduler.scheduleAtFixedRate(() -> {
            String dato = leerDato();
            Iterator<Map.Entry<String, Integer>> iterator = clientes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
                String clienteID = entry.getKey();
                Integer tempoRestante = entry.getValue();

                // Reducir o tempo restante e comprobar se eliminalo
                tempoRestante--;
                clientes.replace(clienteID, tempoRestante);
                if (tempoRestante < 0) {
                    iterator.remove();
                    try {
                        canle.queueDelete(clienteID);
                        debugPrint("Cola eliminada. Quedan " + clientes.size() + " clientes.");
                    } catch (IOException e) {
                        System.err.println("Erro na eliminación da cola: " + e.getMessage());
                    }
                    continue;
                }
                try {
                    RabbitMQ.enviar(canle, "cliente_" + clienteID, dato);
                    debugPrint("Envío "+dato+" a "+clienteID);
                } catch (Exception e) {
                    System.err.println("Erro no envío de mensaxes: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            try {
                dato = RabbitMQ.recibir(canle, nomeColaSuscripcions);
                while (dato != null) {
                    String[] aux = dato.split(" ");
                    canle.queueDeclare("cliente_" + aux[0], false, false, false, null);
                    clientes.put(aux[0], Integer.parseInt(aux[1]));
                    debugPrint("Suscripción aceptada, hay "+clientes.size()+" clientes");
                    dato = RabbitMQ.recibir(canle, nomeColaSuscripcions);
                }
            } catch (Exception e) {
                System.err.println("Erro na recepción de mensaxes: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS); // Executa cada segundo
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
            System.out.println("Uso: java P4ComDis.Servidor.Servidor <archivo> <IP> <nombreCola>");
            System.exit(1);
        }

        String arquivo = args[0];
        String IP = args[1];
        String nombreColaSuscripciones = "colaSuscripcions";

        Servidor servidor = new Servidor(arquivo, IP, nombreColaSuscripciones);
        new Thread(servidor).start();
    }
}
