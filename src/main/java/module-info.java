module com.example.summer2023project {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.jsoup;


    opens com.example.googlewebscraper to javafx.fxml;
    exports com.example.googlewebscraper;
}