package com.nesposi3;

import com.nesposi3.Utils.CacheUtils;
import com.nesposi3.Utils.ClusteringUtils;
import com.nesposi3.Utils.SimilarityUtils;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;

public class GuiMain extends Application {
    private static final String ARROW = (
            " |\n" +
                    " |\n" +
                    " |\n" +
                    " |\n" +
                    "V\n"
    );

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Wikipedia Pathfinding");
        try {
            GraphNode[] nodes = CacheUtils.getAllNodes();
            ComboBox<GraphNode> endBox = new ComboBox<>();
            ComboBox<GraphNode> startBox = new ComboBox<>();
            Arrays.sort(nodes, new Comparator<GraphNode>() {
                @Override
                public int compare(GraphNode o1, GraphNode o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            endBox.setItems(FXCollections.observableArrayList(nodes));
            startBox.setItems(FXCollections.observableArrayList(nodes));
            Button btn = new Button();
            TextArea clusterArea = new TextArea();
            clusterArea.setMinHeight(400);
            clusterArea.setEditable(false);
            btn.setText("Enter");
            btn.setOnAction(actionEvent -> {
                clusterArea.setText("");
                clusterArea.setScrollTop(0);
                GraphNode source = startBox.getValue();
                GraphNode end = endBox.getValue();
                GraphNode res = SimilarityUtils.djikstra(source, end, nodes);
                if (res == null) {
                    clusterArea.setText("No path between " + source.getName() + " and " + end.getName());
                } else {
                    String[] path = SimilarityUtils.getNodePathInReverseOrder(res);
                    for (int i = path.length - 1; i >= 0; i--) {
                        clusterArea.appendText(path[i] + "\n" + ((i!=0)?ARROW:""));
                    }
                }
                // Reset the prev to null and distances
                for (GraphNode g : nodes
                ) {
                    g.clearDjikstraInfo();
                }
            });
            GridPane root = new GridPane();
            root.setAlignment(Pos.CENTER);
            GridPane text = new GridPane();
            text.setAlignment(Pos.CENTER);
            GridPane.setValignment(btn, VPos.CENTER);
            GridPane.setHalignment(btn, HPos.CENTER);
            GridPane.setConstraints(btn, 2, 0);
            GridPane.setConstraints(clusterArea, 0, 3);
            GridPane.setConstraints(endBox, 1, 0);
            GridPane.setConstraints(startBox, 0, 0);
            GridPane.setConstraints(text, 0, 0);
            text.getChildren().addAll(startBox, endBox, btn);
            root.getChildren().addAll(text, clusterArea);
            primaryStage.setScene(new Scene(root, 1000, 800));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
}
