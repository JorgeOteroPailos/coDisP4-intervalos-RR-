package Cliente;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class Controlador {
    @FXML
    private Label welcomeText;

    private Cliente cliente;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    public Controlador(String[] args) {

        if (args.length == 3) {

            cliente=new Cliente(this, args[1], args[0]);
        } else {
            System.err.println("Erro: argumentos esperados: IPservidor nomeColaSuscripcions");
        }


    }

    @FXML
    private void initialize(String[] args) {

    }
}