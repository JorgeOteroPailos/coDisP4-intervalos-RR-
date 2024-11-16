package Cliente;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class helloAplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Obtener los parámetros de la línea de comandos
        String[] args = getParameters().getRaw().toArray(new String[0]);

        // Cargar el FXML
        FXMLLoader fxmlLoader = new FXMLLoader(Controlador.class.getResource("cliente-view.fxml"));

        // Crear el controlador con los argumentos
        Controlador controlador = new Controlador(args);

        // Asignar el controlador al loader
        fxmlLoader.setController(controlador);

        // Cargar la escena
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Cliente");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}