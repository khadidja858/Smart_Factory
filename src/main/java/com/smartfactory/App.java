package com.smartfactory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class App extends Application {
    private static final String BG = "#07111F";
    private static final String PANEL = "#101E31";
    private static final String PANEL2 = "#14263D";
    private static final String BLUE = "#238BFF";
    private static final String CYAN = "#19C5D8";
    private static final String MUTED = "#8EA4BC";

    private final LinkedHashMap<String, Machine> machines = new LinkedHashMap<>();
    private final List<String> alarmHistory = new ArrayList<>();
    private final LinkedList<String> timeLabels = new LinkedList<>();
    private final LinkedList<Number> motorTemps = new LinkedList<>();
    private final LinkedList<Number> pumpTemps = new LinkedList<>();
    private final LinkedList<Number> conveyorTemps = new LinkedList<>();

    private Stage stage;
    private BorderPane shell;
    private StackPane pageHost;
    private Label connectionStatus;
    private Label pageTitle;
    private Label pageSubtitle;
    private String currentPage = "Dashboard";
    private Machine selected;
    private ComboBox<String> machineSelect;
    private ComboBox<String> modeSelect;
    private ToggleButton powerToggle;
    private Slider speedSlider;
    private Label speedValue, stateValue, loadValue, tempValue, alarmValue;
    private ProgressIndicator loadIndicator;
    private ProgressIndicator connectionIndicator;
    private VBox alarmList;
    private LineChart<String, Number> temperatureChart;
    private BarChart<String, Number> loadChart;
    private PieChart statusChart;
    private Timeline simulation;
    private int tick = 0;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        machines.put("Motor", new Machine("Motor"));
        machines.put("Pump", new Machine("Pump"));
        machines.put("Conveyor", new Machine("Conveyor"));
        selected = machines.get("Motor");

        for (int i = 0; i < 8; i++) {
            timeLabels.add(String.format("%02d:%02d", 10, i * 5));
            motorTemps.add(35 + i);
            pumpTemps.add(37 + i);
            conveyorTemps.add(33 + i);
        }

        shell = new BorderPane();
        shell.getStyleClass().add("app-root");
        shell.setTop(createHeader());
        shell.setLeft(createSidebar());
        pageHost = new StackPane();
        pageHost.setPadding(new Insets(24));
        shell.setCenter(pageHost);

        Scene scene = new Scene(shell, 1440, 900);
        scene.getStylesheets().add(Objects.requireNonNull(
        		getClass().getResource("/com/factory-style.css")).toExternalForm());

        stage.setTitle("SMART FACTORY | Industrial Monitoring");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        showPage("Dashboard");
        stage.show();
        startSimulation();
    }

    private HBox createHeader() {
        Label logo = new Label("◆  SMART FACTORY");
        logo.getStyleClass().add("brand");
        Label sub = new Label("INDUSTRIAL OPERATIONS CENTER");
        sub.getStyleClass().add("brand-sub");
        VBox brand = new VBox(3, logo, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label liveDot = new Label("●");
        liveDot.getStyleClass().add("live-dot");
        connectionStatus = new Label("SYSTEM ONLINE");
        connectionStatus.getStyleClass().add("online-text");
        Label date = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                + "   |   LIVE SIMULATION");
        date.getStyleClass().add("muted-text");
        HBox right = new HBox(12, liveDot, connectionStatus, new Label("   "), date);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(brand, spacer, right);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 28, 18, 28));
        header.getStyleClass().add("top-header");
        return header;
    }

    private VBox createSidebar() {
        Label workspace = new Label("WORKSPACE");
        workspace.getStyleClass().add("side-heading");
        VBox nav = new VBox(10);
        nav.getChildren().addAll(
                navButton("▦", "Dashboard"),
                navButton("⚙", "Machine Control"),
                navButton("▤", "Production Line"),
                navButton("⚠", "Alarms")
        );
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label system = new Label("SYSTEM STATUS");
        system.getStyleClass().add("side-heading");
        Label simulated = new Label("●  Simulation active\n    JavaFX local environment");
        simulated.getStyleClass().add("side-status");
        VBox side = new VBox(18, workspace, nav, spacer, system, simulated);
        side.setPadding(new Insets(26, 16, 24, 16));
        side.setPrefWidth(235);
        side.getStyleClass().add("sidebar");
        return side;
    }

    private Button navButton(String icon, String name) {
        Button b = new Button(icon + "    " + name);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.getStyleClass().add("nav-button");
        b.setOnAction(e -> showPage(name));
        return b;
    }

    private void showPage(String page) {
        currentPage = page;
        pageHost.getChildren().clear();
        VBox content;
        switch (page) {
            case "Machine Control" -> content = machineControlPage();
            case "Production Line" -> content = productionPage();
            case "Alarms" -> content = alarmsPage();
            default -> content = dashboardPage();
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        pageHost.getChildren().add(scroll);
    }

    private VBox pageHeading(String title, String subtitle) {
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("page-subtitle");
        return new VBox(6, t, s);
    }

    private VBox dashboardPage() {
        VBox root = new VBox(20);
        root.getChildren().add(pageHeading("Factory Overview", "Real-time production line performance and equipment health."));
        HBox kpis = new HBox(14,
                kpiCard("MACHINES ONLINE", onlineCount() + " / 3", "Equipment running", "green"),
                kpiCard("AVERAGE TEMPERATURE", averageTemp() + " °C", "Across all equipment", "blue"),
                kpiCard("ACTIVE ALARMS", String.valueOf(activeFaults() + warningCount()), "Warnings and faults", "orange"),
                kpiCard("PRODUCTION MODE", "SIMULATED", "Local demonstration", "purple"));
        kpis.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        root.getChildren().add(kpis);

        HBox charts = new HBox(16);
        VBox trendCard = card("TEMPERATURE MONITORING", "Live temperature trend by machine", createTemperatureChart());
        VBox statusCard = card("EQUIPMENT AVAILABILITY", "Current operating state", createStatusChart());
        HBox.setHgrow(trendCard, Priority.ALWAYS);
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        charts.getChildren().addAll(trendCard, statusCard);
        root.getChildren().add(charts);

        HBox lower = new HBox(16);
        VBox load = card("MACHINE LOAD", "Current simulated load percentage", createLoadChart());
        VBox table = card("EQUIPMENT STATUS", "Live machine status", machineStatusTable());
        HBox.setHgrow(load, Priority.ALWAYS);
        HBox.setHgrow(table, Priority.ALWAYS);
        lower.getChildren().addAll(load, table);
        root.getChildren().add(lower);
        return root;
    }

    private static final Priority Priority_besar = Priority.ALWAYS;

    private VBox kpiCard(String label, String value, String note, String tone) {
        Label l = new Label(label); l.getStyleClass().add("kpi-label");
        Label v = new Label(value); v.getStyleClass().addAll("kpi-value", "tone-" + tone);
        Label n = new Label(note); n.getStyleClass().add("muted-text");
        VBox box = new VBox(10, l, v, n); box.getStyleClass().add("panel");
        box.setPadding(new Insets(20)); box.setMinHeight(125);
        return box;
    }

    private VBox card(String title, String subtitle, javafx.scene.Node body) {
        Label t = new Label(title); t.getStyleClass().add("card-title");
        Label s = new Label(subtitle); s.getStyleClass().add("muted-text");
        VBox box = new VBox(12, t, s, body);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(20));
        VBox.setVgrow(body, Priority.ALWAYS);
        return box;
    }

    private LineChart<String, Number> createTemperatureChart() {
        CategoryAxis x = new CategoryAxis(); x.setLabel("TIME");
        NumberAxis y = new NumberAxis(0, 120, 20); y.setLabel("°C");
        temperatureChart = new LineChart<>(x, y);
        temperatureChart.setAnimated(false);
        temperatureChart.setLegendVisible(true);
        temperatureChart.setCreateSymbols(false);
        temperatureChart.setPrefHeight(280);
        refreshTemperatureChart();
        return temperatureChart;
    }

    private void refreshTemperatureChart() {
        if (temperatureChart == null) return;
        temperatureChart.getData().clear();
        temperatureChart.getData().add(tempSeries("Motor", motorTemps));
        temperatureChart.getData().add(tempSeries("Pump", pumpTemps));
        temperatureChart.getData().add(tempSeries("Conveyor", conveyorTemps));
    }

    private XYChart.Series<String, Number> tempSeries(String name, List<Number> values) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(name);
        for (int i = 0; i < Math.min(timeLabels.size(), values.size()); i++)
            s.getData().add(new XYChart.Data<>(timeLabels.get(i), values.get(i)));
        return s;
    }

    private PieChart createStatusChart() {
        statusChart = new PieChart();
        statusChart.setLegendVisible(true);
        statusChart.setLabelsVisible(true);
        statusChart.setPrefHeight(280);
        refreshStatusChart();
        return statusChart;
    }

    private void refreshStatusChart() {
        if (statusChart == null) return;
        long running = machines.values().stream().filter(m -> m.running && !m.fault).count();
        long fault = machines.values().stream().filter(m -> m.fault).count();
        long stopped = 3 - running - fault;
        statusChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Running", running),
                new PieChart.Data("Stopped", stopped),
                new PieChart.Data("Fault", fault)));
    }

    private BarChart<String, Number> createLoadChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 20);
        y.setLabel("LOAD %");
        loadChart = new BarChart<>(x, y);
        loadChart.setLegendVisible(false);
        loadChart.setAnimated(false);
        loadChart.setPrefHeight(260);
        refreshLoadChart();
        return loadChart;
    }

    private void refreshLoadChart() {
        if (loadChart == null) return;
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (Machine m : machines.values()) s.getData().add(new XYChart.Data<>(m.name, m.load));
        loadChart.getData().setAll(s);
    }

    private TableView<Machine> machineStatusTable() {
        TableView<Machine> table = new TableView<>();
        table.setPrefHeight(260);
        TableColumn<Machine, String> name = new TableColumn<>("EQUIPMENT");
        name.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(v.getValue().name));
        TableColumn<Machine, String> state = new TableColumn<>("STATUS");
        state.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(v.getValue().status()));
        TableColumn<Machine, String> temp = new TableColumn<>("TEMP");
        temp.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty((int)v.getValue().temperature + " °C"));
        TableColumn<Machine, String> speed = new TableColumn<>("SPEED");
        speed.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty((int)v.getValue().speed + " RPM"));
        table.getColumns().addAll(name, state, temp, speed);
        table.setItems(FXCollections.observableArrayList(machines.values()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private VBox machineControlPage() {
        VBox root = new VBox(20);
        root.getChildren().add(pageHeading("Machine Control", "Select equipment, choose operating mode and adjust the machine setpoint."));
        machineSelect = new ComboBox<>(FXCollections.observableArrayList(machines.keySet()));
        machineSelect.setValue(selected.name);
        machineSelect.setOnAction(e -> {
            selected = machines.get(machineSelect.getValue());
            updateControlPage();
        });
        modeSelect = new ComboBox<>(FXCollections.observableArrayList("Manual", "Automatic"));
        modeSelect.setValue("Manual");
        modeSelect.setOnAction(e -> updateControlPage());

        HBox selectors = new HBox(14,
                labeled("SELECT MACHINE", machineSelect),
                labeled("OPERATING MODE", modeSelect));
        selectors.setAlignment(Pos.CENTER_LEFT);

        powerToggle = new ToggleButton();
        powerToggle.setOnAction(e -> {
            if (powerToggle.isSelected()) startMachine(selected);
            else stopMachine(selected);
            updateAll();
        });
        stateValue = new Label(); stateValue.getStyleClass().add("state-value");
        VBox state = card("MACHINE STATE", "Equipment power and operating status",
                new VBox(12, stateValue, powerToggle));

        speedSlider = new Slider(0, 3000, 0);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(1000);
        speedSlider.setMinorTickCount(4);
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (selected.running && !selected.fault && "Manual".equals(modeSelect.getValue())) {
                selected.speed = newV.doubleValue();
                speedValue.setText((int)selected.speed + " RPM");
            }
        });
        speedValue = new Label(); speedValue.getStyleClass().add("metric-number");
        VBox speed = card("SPEED SETPOINT", "Adjust between 0 and 3000 RPM",
                new VBox(14, speedValue, speedSlider));

        loadIndicator = new ProgressIndicator(0);
        loadIndicator.setPrefSize(92, 92);
        loadValue = new Label(); loadValue.getStyleClass().add("metric-number");

        // Indeterminate indicator: visual demonstration of a connection in progress.
        connectionIndicator = new ProgressIndicator(-1);
        connectionIndicator.setPrefSize(30, 30);
        Label connectingText = new Label("Connecting...");
        connectingText.getStyleClass().add("muted-text");
        HBox connectionDemo = new HBox(10, connectionIndicator, connectingText);
        connectionDemo.setAlignment(Pos.CENTER_LEFT);

        VBox loadContent = new VBox(12, loadIndicator, loadValue, connectionDemo);
        loadContent.setAlignment(Pos.CENTER_LEFT);
        VBox load = card("MACHINE LOAD", "Determinate load + indeterminate connection",
                loadContent);

        tempValue = new Label(); tempValue.getStyleClass().add("metric-number");
        alarmValue = new Label(); alarmValue.getStyleClass().add("alarm-normal");
        Button reset = new Button("RESET FAULT");
        reset.getStyleClass().add("secondary-button");
        reset.setOnAction(e -> resetFault(selected));
        VBox temp = card("TEMPERATURE & ALARMS", "Warning above 90°C · Automatic stop above 100°C",
                new VBox(12, tempValue, alarmValue, reset));

        Button emergency = new Button("EMERGENCY STOP");
        emergency.getStyleClass().add("danger-button");
        emergency.setOnAction(e -> confirmEmergencyStop());
        Button stopLine = new Button("STOP ENTIRE LINE");
        stopLine.getStyleClass().add("secondary-button");
        stopLine.setOnAction(e -> confirmStopLine());
        HBox actions = new HBox(12, emergency, stopLine);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.add(state, 0, 0); grid.add(speed, 1, 0);
        grid.add(load, 0, 1); grid.add(temp, 1, 1);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        root.getChildren().addAll(selectors, grid, actions);
        updateControlPage();
        return root;
    }

    private VBox labeled(String title, Control control) {
        Label l = new Label(title); l.getStyleClass().add("kpi-label");
        VBox b = new VBox(8, l, control); b.setMinWidth(180);
        return b;
    }

    private void updateControlPage() {
        if (powerToggle == null || speedSlider == null || selected == null) return;
        boolean running = selected.running && !selected.fault;
        powerToggle.setSelected(running);
        powerToggle.setText(running ? "ON  |  RUNNING" : (selected.fault ? "FAULT" : "OFF  |  STOPPED"));
        stateValue.setText(selected.status());
        speedSlider.setValue(selected.speed);
        speedSlider.setDisable(!running || "Automatic".equals(modeSelect.getValue()));
        speedValue.setText((int)selected.speed + " RPM");
        loadIndicator.setProgress(selected.load / 100.0);
        loadValue.setText((int)selected.load + " % LOAD");
        tempValue.setText((int)selected.temperature + " °C");
        if (selected.fault) {
            alarmValue.setText("FAULT — AUTOMATIC STOP");
            alarmValue.setStyle("-fx-text-fill:#FF5263;");
        } else if (selected.temperature > 90) {
            alarmValue.setText("WARNING — HIGH TEMPERATURE");
            alarmValue.setStyle("-fx-text-fill:#FFB547;");
        } else {
            alarmValue.setText("NORMAL — ALL PARAMETERS SAFE");
            alarmValue.setStyle("-fx-text-fill:#42D69A;");
        }
    }

    private VBox productionPage() {
        VBox root = new VBox(20);
        root.getChildren().add(pageHeading("Production Line", "Overview of all connected equipment and current operating conditions."));
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(16);
        int col = 0, row = 0;
        for (Machine m : machines.values()) {
            VBox card = machineCard(m);
            grid.add(card, col, row);
            col++; if (col == 3) { col = 0; row++; }
        }
        root.getChildren().add(grid);
        Button refresh = new Button("REFRESH STATUS");
        refresh.setOnAction(e -> showPage("Production Line"));
        Button control = new Button("OPEN MACHINE CONTROL");
        control.getStyleClass().add("secondary-button");
        control.setOnAction(e -> showPage("Machine Control"));
        root.getChildren().add(new HBox(12, refresh, control));
        return root;
    }

    private VBox machineCard(Machine m) {
        Label name = new Label(m.name.toUpperCase()); name.getStyleClass().add("card-title");
        Label status = new Label(m.status()); status.getStyleClass().add(m.fault ? "fault-text" : m.running ? "running-text" : "muted-text");
        Label metrics = new Label("Speed     " + (int)m.speed + " RPM\nLoad       " + (int)m.load + " %\nTemperature   " + (int)m.temperature + " °C");
        metrics.getStyleClass().add("machine-metrics");
        Button select = new Button("CONTROL THIS MACHINE");
        select.setOnAction(e -> {
            selected = m;
            showPage("Machine Control");
        });
        VBox box = new VBox(14, name, status, metrics, select);
        box.getStyleClass().add("panel");
        box.setPadding(new Insets(22));
        box.setMinWidth(220);
        return box;
    }

    private VBox alarmsPage() {
        VBox root = new VBox(18);
        root.getChildren().add(pageHeading("Alarm Center", "Warnings, equipment faults and operator actions."));
        HBox summary = new HBox(14,
                kpiCard("ACTIVE FAULTS", String.valueOf(activeFaults()), "Machines stopped by fault", "orange"),
                kpiCard("HIGH TEMPERATURE", String.valueOf(warningCount()), "Above 90°C", "orange"));
        alarmList = new VBox(10);
        refreshAlarmList();
        VBox listCard = card("EVENT LOG", "Newest events appear at the top", alarmList);
        Button refresh = new Button("REFRESH ALARMS");
        refresh.setOnAction(e -> refreshAlarmList());
        Button clear = new Button("CLEAR HISTORY");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Clear the alarm history? Current machine faults will remain active.", ButtonType.CANCEL, ButtonType.OK);
            a.setHeaderText("Clear event history");
            if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                alarmHistory.clear();
                refreshAlarmList();
            }
        });
        root.getChildren().addAll(summary, listCard, new HBox(12, refresh, clear));
        return root;
    }

    private void refreshAlarmList() {
        if (alarmList == null) return;
        alarmList.getChildren().clear();
        if (alarmHistory.isEmpty()) {
            Label none = new Label("No alarm events recorded. System operating normally.");
            none.getStyleClass().add("running-text");
            alarmList.getChildren().add(none);
        } else {
            for (String entry : alarmHistory) {
                Label l = new Label("⚠   " + entry);
                l.getStyleClass().add("alarm-entry");
                l.setMaxWidth(Double.MAX_VALUE);
                alarmList.getChildren().add(l);
            }
        }
    }

    private void startMachine(Machine m) {
        if (m.fault) {
            showInfo("Cannot start", m.name + " has an active FAULT. Use RESET FAULT first.");
            return;
        }
        m.running = true;
        if (m.speed < 1) m.speed = 1200;
        addEvent(m.name + " started by operator.");
        updateAll();
    }

    private void stopMachine(Machine m) {
        m.running = false;
        m.speed = 0;
        m.load = 0;
        addEvent(m.name + " stopped.");
        updateAll();
    }

    private void resetFault(Machine m) {
        if (!m.fault) {
            showInfo("No fault", m.name + " has no active fault to reset.");
            return;
        }
        m.fault = false; m.running = false; m.speed = 0;
        m.temperature = 35; m.load = 0;
        addEvent(m.name + " fault reset. Machine remains stopped.");
        updateAll();
    }

    private void confirmEmergencyStop() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Emergency stop");
        a.setHeaderText("Stop " + selected.name + " immediately?");
        a.setContentText("The selected machine will stop. This is a simulated control action.");
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            stopMachine(selected);
            addEvent("Emergency stop confirmed for " + selected.name + ".");
            updateAll();
        }
    }

    private void confirmStopLine() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Stop production line");
        a.setHeaderText("Stop all production equipment?");
        a.setContentText("Motor, Pump and Conveyor will all be stopped in this simulation.");
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            for (Machine m : machines.values()) {
                m.running = false; m.speed = 0; m.load = 0;
            }
            addEvent("Production line stopped by operator.");
            updateAll();
        }
    }

    private void startSimulation() {
        simulation = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            tick++;
            for (Machine m : machines.values()) {
                if (m.running && !m.fault) {
                    m.temperature = Math.min(110, m.temperature + 2.5 + Math.random() * 2.0);
                    m.load = Math.min(100, Math.max(15, m.speed / 3000.0 * 70 + Math.random() * 18));
                    if (m.temperature > 100) {
                        m.fault = true; m.running = false; m.speed = 0; m.load = 0;
                        addEvent("CRITICAL: " + m.name + " exceeded 100°C and stopped automatically.");
                    } else if (m.temperature > 90 && !m.warningLogged) {
                        m.warningLogged = true;
                        addEvent("WARNING: " + m.name + " temperature exceeded 90°C.");
                    }
                } else if (!m.running && !m.fault) {
                    m.load = 0;
                    m.temperature = Math.max(30, m.temperature - 0.5);
                    if (m.temperature <= 90) m.warningLogged = false;
                }
            }
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            timeLabels.add(time);
            motorTemps.add(machines.get("Motor").temperature);
            pumpTemps.add(machines.get("Pump").temperature);
            conveyorTemps.add(machines.get("Conveyor").temperature);
            while (timeLabels.size() > 12) {
                timeLabels.removeFirst(); motorTemps.removeFirst(); pumpTemps.removeFirst(); conveyorTemps.removeFirst();
            }
            updateAll();
        }));
        simulation.setCycleCount(Timeline.INDEFINITE);
        simulation.play();
    }

    private void addEvent(String message) {
        alarmHistory.add(0, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "  |  " + message);
        if (alarmHistory.size() > 100) alarmHistory.remove(alarmHistory.size() - 1);
        if (alarmList != null) refreshAlarmList();
    }

    private void updateAll() {
        if (currentPage.equals("Machine Control")) updateControlPage();
        if (currentPage.equals("Dashboard")) {
            showPage("Dashboard");
        } else if (currentPage.equals("Production Line")) {
            showPage("Production Line");
        } else if (currentPage.equals("Alarms")) {
            showPage("Alarms");
        }
    }

    private int onlineCount() {
        return (int) machines.values().stream().filter(m -> m.running && !m.fault).count();
    }
    private int activeFaults() {
        return (int) machines.values().stream().filter(m -> m.fault).count();
    }
    private int warningCount() {
        return (int) machines.values().stream().filter(m -> !m.fault && m.temperature > 90).count();
    }
    private int averageTemp() {
        return (int) machines.values().stream().mapToDouble(m -> m.temperature).average().orElse(0);
    }
    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    @Override
    public void stop() {
        if (simulation != null) simulation.stop();
    }

    private static class Machine {
        final String name;
        boolean running = false;
        boolean fault = false;
        boolean warningLogged = false;
        double speed = 0;
        double temperature = 35;
        double load = 0;
        Machine(String name) { this.name = name; }
        String status() { return fault ? "FAULT" : running ? "RUNNING" : "STOPPED"; }
    }

    public static void main(String[] args) { launch(args); }
}
