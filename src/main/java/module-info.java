module P4ComDis.main {

    // Dependencias externas
    requires javafx.controls;
    requires javafx.fxml;
    requires com.rabbitmq.client;
    requires java.management;

    // Exporta los paquetes necesarios
    exports P4ComDis.Cliente;
    exports P4ComDis.utils;
    exports P4ComDis.Servidor;

    // Abrir paquetes para frameworks como JavaFX
    opens P4ComDis.Cliente to javafx.fxml;

}