module com.escaperoom {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;
    opens com.escaperoom to javafx.fxml, org.junit.platform.commons;
    exports com.escaperoom;
    exports com.escapenexus;
}
