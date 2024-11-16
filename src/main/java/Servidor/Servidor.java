package Servidor;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static movidasDeMensajes.RabbitMQ.*;

public class Servidor implements Runnable {
    private Channel canle;
    private BufferedReader lectorArquivo;
    private HashMap<String, Integer> clientes;
    private String nomeColaSuscripcions;
    private int periodo; //periodo entre cada dato que envía

    public Servidor(String arquivo, int periodo, String IP, String nomeColaSuscripcions) {
        try {
            lectorArquivo = new BufferedReader(new InputStreamReader(new URL(arquivo).openStream()));
            clientes = new HashMap<>();
            this.nomeColaSuscripcions = nomeColaSuscripcions;
            this.periodo = periodo;

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IP);

            try (Connection conexion = factory.newConnection()) {
                canle = conexion.createChannel();
                canle.queueDeclare(nomeColaSuscripcions, false, true, false, null);
            }
        } catch (Exception e) {
            System.err.println("Erro na inicialización do servidor: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String dato = leerDato();
                Iterator<Map.Entry<String, Integer>> iterator = clientes.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> entry = iterator.next();
                    String clienteID = entry.getKey();
                    Integer tempoRestante = entry.getValue();

                    // Reducir el tiempo restante y comprobar si eliminarlo
                    tempoRestante--;
                    clientes.replace(clienteID,tempoRestante);
                    if (tempoRestante < 0) {
                        iterator.remove();
                        try {
                            canle.queueDelete(clienteID);
                        } catch (IOException e) {
                            System.err.println("Erro na eliminación da cola: "+e.getMessage());
                        }
                        continue;
                    }
                    try {
                        enviar(canle, "cliente_" + clienteID, dato);
                    } catch (Exception e) {
                        System.err.println("Erro no envío de mensaxes: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                }

                try {
                    dato = recibir(canle, nomeColaSuscripcions);
                    while (dato != null) {
                        String[] aux = dato.split(" ");
                        canle.queueDeclare(aux[0], false, true, false, null);
                        dato = recibir(canle, nomeColaSuscripcions);
                        clientes.put(aux[0], Integer.parseInt(aux[1]));
                    }
                } catch (Exception e) {
                    System.err.println("Erro na recepción de mensaxes: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }, 0, periodo); // Ejecutar cada segundo
    }

    private String leerDato() {
        try {
            return lectorArquivo.readLine();
        } catch (java.io.IOException e) {
            System.err.println("Erro naa lectura dos datos: " + e.getMessage());
            return "0";
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Uso: java Servidor <archivo> <periodo> <IP> <nombreCola>");
            System.exit(1);
        }

        String arquivo = args[0];
        int periodo = Integer.parseInt(args[1]);
        String IP = args[2];
        String nombreColaSuscripciones = args[3];

        // Crear y ejecutar el servidor
        Servidor servidor = new Servidor(arquivo, periodo, IP, nombreColaSuscripciones);
        new Thread(servidor).start();
    }
}
