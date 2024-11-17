package P4ComDis.Cliente;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Popup {
    /**
     * Mostra un popup coa mensaxe especificada.
     *
     * @param title   Título da ventá do popup.
     * @param message Mensaxe a mostrar dentro do popup.
     * @param type    Tipo de alerta (INFO, WARNING, ERROR, etc.).
     */
    public static void show(String title, String message, AlertType type) {
        // Crear unha alerta
        Alert alert = new Alert(type);

        // Configurar o título e contido
        alert.setTitle(title);
        alert.setHeaderText(null); // Opcional, podes engadir un subtítulo
        alert.setContentText(message);

        // Mostrar o popup e esperar que o usuario o peche
        alert.showAndWait();
    }
}

