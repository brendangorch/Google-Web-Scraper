package com.example.googlewebscraper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.awt.Desktop;

import javafx.scene.input.MouseButton;

public class MainController implements Initializable {

    // define FXML attributes
    @FXML
    private Button clearBtn;
    @FXML
    private TextField keywordTextField;
    @FXML
    private ComboBox<Integer> pageNumComboBox;
    @FXML
    private Button searchBtn;
    @FXML
    private TreeView<String> treeViewItem;
    @FXML
    private Label invalidSearchLabel;

    // define button CSS styles
    private static final String IDLE_BUTTON_STYLE = "-fx-background-color: darkorange; -fx-background-radius: 15;";
    private static final String HOVERED_BUTTON_STYLE = "-fx-background-color: goldenrod; -fx-background-radius: 12.5; " +
            "-fx-text-fill: beige; -fx-cursor: hand; -fx-border-color: darkorange; -fx-border-radius: 12.5;";

    // define text field and combo box CSS styles
    public static final String IDLE_FIELD_STYLE = "-fx-background-color: grey;";
    public static final String HOVERED_FIELD_STYLE = "-fx-background-color: lightgrey;";


    // initialize method
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // build the combo box
        buildComboBox();
        // take focus off text field when program opens
        keywordTextField.setFocusTraversable(false);
        // build the hover animations
        buildHoverAnimations();
    }

    // combo box builder method
    public void buildComboBox() {
        // create list and set items of combo box to the list of integers
        ObservableList<Integer> pageNums = FXCollections.observableArrayList();
        pageNums.addAll(1, 2, 3, 4, 5, 10);
        pageNumComboBox.setItems(pageNums);
    }

    // method for button, field, and combo box animations
    public void buildHoverAnimations() {
        // set idle styles
        searchBtn.setStyle(IDLE_BUTTON_STYLE);
        clearBtn.setStyle(IDLE_BUTTON_STYLE);
        keywordTextField.setStyle(IDLE_FIELD_STYLE);
        pageNumComboBox.setStyle(IDLE_FIELD_STYLE);

        // set on mouse entered styles
        searchBtn.setOnMouseEntered(e -> searchBtn.setStyle(HOVERED_BUTTON_STYLE));
        clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(HOVERED_BUTTON_STYLE));
        keywordTextField.setOnMouseEntered(e -> keywordTextField.setStyle(HOVERED_FIELD_STYLE));
        pageNumComboBox.setOnMouseEntered(e -> pageNumComboBox.setStyle(HOVERED_FIELD_STYLE));

        // set on mouse exited styles back to idle styles
        searchBtn.setOnMouseExited(e -> searchBtn.setStyle(IDLE_BUTTON_STYLE));
        clearBtn.setOnMouseExited(e -> clearBtn.setStyle(IDLE_BUTTON_STYLE));
        keywordTextField.setOnMouseExited(e -> keywordTextField.setStyle(IDLE_FIELD_STYLE));
        pageNumComboBox.setOnMouseExited(e -> pageNumComboBox.setStyle(IDLE_FIELD_STYLE));
    }

    // clear button method
    public void onClickClear() {
        // clear field and combo box
        keywordTextField.setText("");
        pageNumComboBox.getSelectionModel().clearSelection();
        // clear the tree view by setting root to an empty node
        TreeItem<String> emptyRoot = new TreeItem<String>("");
        treeViewItem.setRoot(emptyRoot);
        // clear all labels
        invalidSearchLabel.setText("");
    }

    // search button method
    public void onSearchClick() {
        // ensure all fields are filled before searching
        if (keywordTextField.getText().equals("") || pageNumComboBox.getValue() == null) {
            invalidSearchLabel.setText("FILL OUT ALL FIELDS BEFORE SEARCHING");
        } else {
            invalidSearchLabel.setText("");
            // get values entered
            int numOfPages = pageNumComboBox.getValue();
            // create a string for the keyword
            String[] keywords = keywordTextField.getText().toLowerCase().split(" ");
            // create a list of the websites scraped
            List<String> websites = scrapeWeb(keywords, numOfPages);
            System.out.println(websites.toString());

            // create a root node
            TreeItem<String> root = new TreeItem<String>("Websites");
            // set the root node to the tree view
            treeViewItem.setRoot(root);

            // add nodes for each website under the root node of the tree view
            for (String website : websites) {
                TreeItem<String> node = new TreeItem<String>(website);
                Hyperlink hyperlink = new Hyperlink(website);
                root.getChildren().add(node);
            }
        }
    }

    // method to make links search the web
    public void searchWeb() {
        // only search web if mouse is left click
        treeViewItem.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // create a String of the tree node that was clicked on
                try {
                    String searchUrl = treeViewItem.getSelectionModel().getSelectedItem().getValue();
                    try {
                        // search the web with the link
                        Desktop.getDesktop().browse(new URI(searchUrl));
                    } catch (IOException | URISyntaxException e) {
                        // Handle any exceptions
                    }
                } catch (Exception e) {
                    // Handle exceptions
                }
            }
        });
    }

    // method to copy link when clicking context menu item
    public void copyLink() {
        try {
            // get the link clicked on
            String link = treeViewItem.getSelectionModel().getSelectedItem().getValue();

            // copy the link to the clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(link);
            clipboard.setContent(content);
        } catch (Exception e) {
            // handle if copy is clicked on non-link item
        }
    }

    // web scraper method
    public static List<String> scrapeWeb(String[] keywords, int numPages) {
        List<String> websites = new ArrayList<>();
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36";
        String acceptLanguage = "en-US,en;q=0.9";
        String referer = "https://www.google.com/";

        try {
            // loop over the pages of the Google search
            for (int page = 0; page < numPages; page++) {
                String keyword = String.join(" ", keywords);
                // construct the Google search with the keyword and page number to search
                String url = "https://www.google.com/search?q=" + keyword + "&start=" + (page * 10);
                // connect to the Google search URL and retrieve the results page
                Document doc = Jsoup.connect(url).get();

                // extract the search result elements from the page
                Elements results = doc.select("div.g");

                // loop over each result
                for (Element result : results) {
                    // extract the link element
                    Element link = result.selectFirst("a[href]");
                    String href = link.attr("href");

                    link.text().toLowerCase();
                    result.text().toLowerCase();

                    // surround in try-catch block, if the block runs - good, if the block switches to catch - check other areas (not using the error href link)
                    try {
                        // extract the URL from the link
                        if (href.startsWith("http")) {
                            String connection = "keep-alive";
                            Document website = Jsoup.connect(href)
                                    .userAgent(userAgent)
                                    .header("Accept-Language", acceptLanguage)
                                    .header("Referer", referer)
                                    .header("Connection", connection).get();

                            // Search keyword in url and description
                            if (containsKeyword(href.toLowerCase(), keywords)
                                    || containsKeyword(website.select("meta[name=description]").attr("content").toLowerCase(), keywords)
                                    || containsKeyword(website.title().toLowerCase(), keywords)
                                    || containsKeyword(website.body().text().toLowerCase(), keywords)
                                    || containsKeyword(website.html().toLowerCase(), keywords)) {
                                // if the keyword is found in any of these places on the website, add it to the list
                                websites.add(href);
                            }
                        }

                    } catch (Exception e) {
                        // instead, check the link and the search result element for the keyword
                        if (containsKeyword(link.text().toLowerCase(), keywords) || containsKeyword(result.text().toLowerCase(), keywords)) {
                            // if the keyword is found in any of these alternative places on the website, add it to the list
                            websites.add(href);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // return the list of websites
        return websites;
    }

    // helper method
    private static boolean containsKeyword(String text, String[] keywords) {
        String keyword = String.join(" ", keywords);
        return text.contains(keyword) || text.contains(keyword.replace(" ", "-")) || text.contains(keyword.replace(" ", "+"));
    }
}
