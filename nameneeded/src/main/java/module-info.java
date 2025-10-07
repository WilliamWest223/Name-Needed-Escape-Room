module com.escaperoom {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.escaperoom to javafx.fxml;
    exports com.escaperoom;
    exports com.escapenexus;
}
