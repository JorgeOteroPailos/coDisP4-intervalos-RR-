package P4ComDis.Cliente;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Popup {
    /**
     * Muestra un popup con el mensaje especificado.
     *
     * @param title   Título de la ventana del popup.
     * @param message Mensaje a mostrar dentro del popup.
     * @param type    Tipo de alerta (INFO, WARNING, ERROR, etc.).
     */
    public static void show(String title, String message, AlertType type) {
        // Crear una alerta
        Alert alert = new Alert(type);

        // Configurar el título y contenido
        alert.setTitle(title);
        alert.setHeaderText(null); // Opcional, puedes añadir un subtítulo
        alert.setContentText(message);

        // Mostrar el popup y esperar que el usuario lo cierre
        alert.showAndWait();
    }
}

