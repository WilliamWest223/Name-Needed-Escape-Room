module com.escaperoom {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;
    requires junit;
    opens com.escaperoom to javafx.fxml;
    exports com.escaperoom;
    exports com.escapenexus;
}
