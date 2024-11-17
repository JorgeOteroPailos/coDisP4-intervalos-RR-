package P4ComDis.Cliente;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static P4ComDis.utils.Outros.debugPrint;

public class Controlador extends Application {
    private XYChart.Series<Number, Number> series;
    private NumberAxis xAxis;
    private int timeCounter = 0;

    private Cliente cliente; // Obxecto do cliente para interactuar co servidor
    private String serverName = "localhost"; // Nome do servidor por defecto
    private int subscriptionTime = 30; // Tempo de suscripción inicial por defecto
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage stage) {
        // Crear gráfico
        xAxis = new NumberAxis(0, 60, 5);
        xAxis.setLabel("Tiempo (s)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Datos");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Gráfico Dinámico");
        lineChart.setAnimated(false);

        // Serie de datos
        series = new XYChart.Series<>();
        series.setName("Valores");
        lineChart.getData().add(series);

        // Crear controis
        HBox controls = gethBox();

        // Layout principal
        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(lineChart);

        // Configurar escea
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("P4ComDis.Cliente con Gráfico");
        stage.setOnCloseRequest(event -> {
            if (cliente != null) {
                cliente.rematar(); // Deter as operaciones do cliente
            }
            executorService.shutdown(); // Apagar o executor
            System.exit(0); // Terminar o programa
        });
        stage.show();
    }

    private HBox gethBox() {
        TextField serverField = new TextField(serverName);
        serverField.setPromptText("Servidor");
        Button connectButton = new Button("Conectar");

        TextField subscriptionTimeField = new TextField(String.valueOf(subscriptionTime)); // Usar a variable subscriptionTime
        subscriptionTimeField.setPromptText("Tempo de suscripción (s)");
        Button renewButton = new Button("Renovar Suscripción");

        // Comportamento do botón "Conectar"
        connectButton.setOnAction(e -> {
            serverName = serverField.getText();
            if (cliente != null) {
                cliente.rematar();
            }
            cliente = new Cliente(this, "colaSuscripcions", serverName); // Conectarse al servidor
            try {
                subscriptionTime = Integer.parseInt(subscriptionTimeField.getText()); // Guardar el tiempo ingresado
                if (subscriptionTime <= 0) throw new NumberFormatException(); // Validar número positivo
                cliente.modificarTempoSuscripcion(subscriptionTime); // Renovar suscripción
            } catch (NumberFormatException ex) {
                Popup.show("Error", "Debe ingresar un tiempo válido (número entero positivo)", Alert.AlertType.ERROR);
            }
        });

        // Comportamentol do botón "Renovar Suscripción"
        renewButton.setOnAction(e -> {
            if (cliente != null) {
                try {
                    subscriptionTime = Integer.parseInt(subscriptionTimeField.getText()); // Actualizar la variable subscriptionTime
                    if (subscriptionTime <= 0) throw new NumberFormatException(); // Validar número positivo
                    cliente.modificarTempoSuscripcion(subscriptionTime); // Renovar suscripción
                    debugPrint("Suscripción renovada por " + subscriptionTime + " segundos.");
                } catch (NumberFormatException ex) {
                    Popup.show("Error", "Debe ingresar un tempo válido (número entero positivo)", Alert.AlertType.ERROR);
                }
            } else {
                Popup.show("Error", "Primero debe conectarse ao servidor", Alert.AlertType.ERROR);
            }
        });

        // Layout para controis
        HBox controls = new HBox(10, new Label("Servidor:"), serverField, connectButton,
                new Label("Tempo (s):"), subscriptionTimeField, renewButton);
        controls.setPadding(new Insets(10));
        return controls;
    }

    public void recibirDato(float dato) {
        executorService.execute(() -> {
            try {
                javafx.application.Platform.runLater(() -> {
                    series.getData().add(new XYChart.Data<>(timeCounter++, dato));
                    // Axustar eixo X para mostrar últimos 60 segundos
                    if (timeCounter > 60) {
                        xAxis.setLowerBound(timeCounter - 60);
                        xAxis.setUpperBound(timeCounter);
                    }
                });
            } catch (Exception e) {
                System.err.println("Error al recibir datos: " + e.getMessage());
            }
        });
    }

    // Nuevo método para obter o tempo de suscripción
    public int getTempoSuscripcion() {
        return subscriptionTime;
    }

    public static void main(String[] args) {
        launch();
    }
}
