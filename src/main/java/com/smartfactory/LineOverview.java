
package com.smartfactory;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class LineOverview {

    private static final String BG = "#030B18";
    private static final String CARD = "#0B1D35";
    private static final String BLUE = "#168BFF";

    public static void show(
            Stage stage,
            Scene controlScene,
            MachineData motor,
            MachineData pump,
            MachineData conveyor) {

        Label title = new Label("SMART FACTORY");
        title.setFont(Font.font("Arial", 30));
        title.setTextFill(Color.web("#168BFF"));

        Label subtitle = new Label("PRODUCTION LINE OVERVIEW");
        subtitle.setTextFill(Color.LIGHTGRAY);
        subtitle.setFont(Font.font("Arial", 14));

        VBox motorCard = createMachineCard(motor);
        VBox pumpCard = createMachineCard(pump);
        VBox conveyorCard = createMachineCard(conveyor);

        HBox machines = new HBox(
                20, motorCard, pumpCard, conveyorCard);

        machines.setAlignment(Pos.CENTER);

        Label activeLabel = new Label();
        activeLabel.setTextFill(Color.WHITE);
        activeLabel.setFont(Font.font("Arial", 16));

        updateActiveLabel(activeLabel, motor, pump, conveyor);

        Button refreshButton = new Button("REFRESH STATUS");

        refreshButton.setOnAction(e -> {
            motorCard.getChildren().setAll(
                    createMachineCard(motor).getChildren());

            pumpCard.getChildren().setAll(
                    createMachineCard(pump).getChildren());

            conveyorCard.getChildren().setAll(
                    createMachineCard(conveyor).getChildren());

            updateActiveLabel(
                    activeLabel, motor, pump, conveyor);
        });

        Button backButton = new Button("BACK TO CONTROL");

        backButton.setOnAction(e ->
                stage.setScene(controlScene));

        VBox root = new VBox(
                25,
                title,
                subtitle,
                machines,
                activeLabel,
                refreshButton,
                backButton
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(35));
        root.setStyle("-fx-background-color: " + BG + ";");

        Scene scene = new Scene(root, 1100, 700);

        scene.getStylesheets().add(
                LineOverview.class.getResource(
                        "/com/smartfactory/factory-style.css"
                ).toExternalForm()
        );

        stage.setTitle("Smart Factory - Line Overview");
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createMachineCard(MachineData machine) {

        Label name = new Label(machine.getName().toUpperCase());
        name.setFont(Font.font("Arial", 20));
        name.setTextFill(Color.web(BLUE));

        Label status = new Label(machine.getStatus());
        status.setFont(Font.font("Arial", 18));

        if (machine.isFault()) {
            status.setTextFill(Color.RED);
        } else if (machine.isRunning()) {
            status.setTextFill(Color.LIGHTGREEN);
        } else {
            status.setTextFill(Color.LIGHTGRAY);
        }

        Label speed = new Label(
                "Speed: " + (int) machine.getSpeed() + " RPM");

        Label temperature = new Label(
                "Temperature: "
                + (int) machine.getTemperature() + " °C");

        Label load = new Label(
                "Load: " + (int) machine.getLoad() + " %");

        for (Label label : new Label[]{speed, temperature, load}) {
            label.setTextFill(Color.WHITE);
            label.setFont(Font.font("Arial", 14));
        }

        VBox card = new VBox(
                18, name, status, speed, temperature, load);

        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));
        card.setMinWidth(260);

        card.setStyle(
                "-fx-background-color: " + CARD + ";"
                + "-fx-background-radius: 15;"
                + "-fx-border-color: #168BFF;"
                + "-fx-border-radius: 15;"
        );

        return card;
    }

    private static void updateActiveLabel(
            Label label,
            MachineData motor,
            MachineData pump,
            MachineData conveyor) {

        int active = 0;

        if (motor.isRunning()) active++;
        if (pump.isRunning()) active++;
        if (conveyor.isRunning()) active++;

        label.setText("ACTIVE MACHINES: " + active + " / 3");
    }
}
