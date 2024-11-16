module com.example.comdisp4 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.rabbitmq.client;
    requires java.sql;  // Agregar este módulo para usar java.sql.Timestamp y otras clases SQL

    opens com.example.comdisp4 to javafx.fxml;
    exports com.example.comdisp4;
    exports Servidor;
    opens Servidor to javafx.fxml;
    exports Cliente;
    opens Cliente to javafx.fxml;
}
