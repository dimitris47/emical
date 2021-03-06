/**
 * Copyright 2021 Dimitris Psathas <dimitrisinbox@gmail.com>
 *
 * This file is part of EmiCal.
 *
 * EmiCal is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License  as  published by  the  Free Software
 * Foundation,  either version 3 of the License,  or (at your option)  any later
 * version.
 *
 * EmiCal is distributed in the hope that it will be useful,  but  WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the  GNU General Public License  for more details.
 *
 * You should have received a copy of the  GNU General Public License along with
 * EmiCal. If not, see <http://www.gnu.org/licenses/>.
 */


package com.dimitris47.emical;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;

public class Emical extends Application {
    Preferences prefs;
    static DecimalFormat df;
    double sizeFactor, defWidth, defHeight;

    LocalDate selDate;
    int newEventDuration, newEventIntensity, daysPassed;
    double evPerMonthNum, meanDuration, meanIntensity;
    int lastMonthEvents;
    double lastMonthDurations, lastMonthIntensities, meanLastMonthDuration, meanLastMonthIntensity;
    String lastMonthReport;

    Label durationLabel, intensityLabel, placeLabel, symptomsLabel, factorsLabel, medicationsLabel,
            savedInfoLabel, evPerMonthLabel, lastMonthEventsLabel, durMeanLabel, intMeanLabel;

    DatePicker calendar;
    Spinner<Integer> spinner;
    Slider intensitySlider;
    RadioButton radLeft, radCenter, radRight, radCombined;
    CheckBox aura, photophobia, soundSens, vertigo, nausea,
            neckProblems, badSleep, stress, fatigue,
            depon, ponstan, imigran, rizatriptan, other;
    TextField otherMed;
    TextArea notesArea;
    Button saveEvent, openJournal, stats, export, infoBtn;

    ArrayList<RadioButton> radios;
    ArrayList<String> radioTexts, symptomTexts, boxTexts, mediTexts;
    ArrayList<CheckBox> symptomBoxes, factorBoxes, mediBoxes;

    MigraineEvent event;

    Label themeLbl;
    RadioButton lightBtn, darkBtn;
    ToggleGroup group;

    @Override
    public void start(Stage stage) throws IOException {
        prefs = Preferences.userNodeForPackage(Emical.class);
        DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(Locale.getDefault());
        dfSymbols.setDecimalSeparator(',');
        dfSymbols.setGroupingSeparator('.');
        df = new DecimalFormat("#.#", dfSymbols);
        Rectangle2D screen = Screen.getPrimary().getBounds();

        if (screen.getWidth() >= 1920) {
            sizeFactor = 1.4;
        } else {
            sizeFactor = 1.2;
        }
        defWidth = 480 * sizeFactor;
        defHeight = 560 * sizeFactor;
        Insets ins = new Insets(0, 0, 0, 8 * sizeFactor);

        calendar = new DatePicker();
        calendar.setValue(LocalDate.now());
        calendar.setConverter(new StringConverter<>() {
            final String pattern = "dd-MM-yyyy";
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
            { calendar.setPromptText(pattern.toLowerCase()); }
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });
        calendar.setOnAction(e -> selDate = calendar.getValue());
        calendar.getEditor().setAlignment(Pos.CENTER);
        calendar.setPrefWidth(96 * sizeFactor);

        durationLabel = new Label("???????????????? (????????)");
        durationLabel.setMinWidth(80 * sizeFactor);
        durationLabel.setPadding(ins);
        spinner = new Spinner<>();
        spinner.setEditable(true);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1, 1));
        spinner.setPrefWidth(48 * sizeFactor);
        spinner.getEditor().setAlignment(Pos.CENTER);
        spinner.getEditor().textProperty().addListener((observableValue, s, t1) -> {
            if (!t1.matches("\\d*"))
                spinner.getEditor().setText(t1.replaceAll("[^\\d]", ""));
        });
        spinner.setOnScroll((ScrollEvent e) -> {
            int deltaY = (int) e.getDeltaY();
            if (deltaY > 0) {
                spinner.getValueFactory().setValue(spinner.getValue() + 1);
            } else if (deltaY < 0) {
                spinner.getValueFactory().setValue(spinner.getValue() - 1);
            }
        });

        intensityLabel = new Label("????????????");
        intensityLabel.setMinWidth(48 * sizeFactor);
        intensityLabel.setPadding(ins);
        intensitySlider = new Slider();
        intensitySlider.setShowTickLabels(true);
        intensitySlider.setMin(1);
        intensitySlider.setMax(10);
        HBox details = new HBox();
        details.setSpacing(4);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().addAll(calendar, durationLabel, spinner, intensityLabel, intensitySlider);

        placeLabel = new Label("???????????????????? ??????????");
        radios = new ArrayList<>();
        ToggleGroup toggleGroup = new ToggleGroup();
        radLeft = new RadioButton("????????????????");
        radCenter = new RadioButton("??????????????");
        radRight = new RadioButton("??????????");
        radCombined = new RadioButton("????????????????????");
        radios.addAll(Arrays.asList(radLeft, radCenter, radRight, radCombined));
        for (var radio : radios)
            radio.setToggleGroup(toggleGroup);
        HBox radioBox = new HBox();
        radioBox.setSpacing(12);
        radioBox.getChildren().addAll(radLeft, radCenter, radRight, radCombined);

        symptomsLabel = new Label("???????????????? ????????????????????");
        symptomBoxes = new ArrayList<>();
        aura = new CheckBox("????????");
        symptomBoxes.add(aura);
        photophobia = new CheckBox("??????????????????");
        soundSens = new CheckBox("????????????????");
        vertigo = new CheckBox("??????????????");
        nausea = new CheckBox("????????????/????????????");
        symptomBoxes.addAll(Arrays.asList(aura, photophobia, soundSens, vertigo, nausea));
        for (var box : symptomBoxes)
            box.setAllowIndeterminate(false);
        HBox symptomBox = new HBox();
        symptomBox.setSpacing(12);
        symptomBox.getChildren().addAll(aura, photophobia, soundSens, vertigo, nausea);

        Separator sep1 = new Separator();
        sep1.setOrientation(Orientation.HORIZONTAL);
        sep1.setValignment(VPos.CENTER);

        factorsLabel = new Label("?????????????????????????? ????????????????????");
        factorBoxes = new ArrayList<>();
        neckProblems = new CheckBox("??????????????");
        badSleep = new CheckBox("?????????? ??????????");
        stress = new CheckBox("??????????/??????????");
        fatigue = new CheckBox("????????????");
        factorBoxes.addAll(Arrays.asList(neckProblems, badSleep, stress, fatigue));
        for (var box : factorBoxes)
            box.setAllowIndeterminate(false);
        HBox circumBox = new HBox();
        circumBox.setSpacing(12);
        circumBox.getChildren().addAll(neckProblems, badSleep, stress, fatigue);

        Separator sep2 = new Separator();
        sep2.setOrientation(Orientation.HORIZONTAL);
        sep2.setValignment(VPos.CENTER);

        medicationsLabel = new Label("??????????????");
        mediBoxes = new ArrayList<>();
        depon = new CheckBox("Depon");
        ponstan = new CheckBox("Ponstan");
        imigran = new CheckBox("Imigran");
        rizatriptan = new CheckBox("Rizatriptan");
        other = new CheckBox("????????");
        mediBoxes.addAll(Arrays.asList(depon, ponstan, imigran, rizatriptan, other));
        for (var box : mediBoxes)
            box.setAllowIndeterminate(false);
        otherMed = new TextField();
        otherMed.setPromptText("????????????????????????");
        otherMed.setOnKeyTyped(e -> other.setSelected(!otherMed.getText().equals("")));

        HBox mediBox = new HBox();
        mediBox.setSpacing(12);
        mediBox.setAlignment(Pos.CENTER_LEFT);
        mediBox.getChildren().addAll(depon, ponstan, imigran, rizatriptan, other, otherMed);

        notesArea = new TextArea();
        notesArea.setPromptText("???????????? ???????????????????? ??????");

        savedInfoLabel = new Label();

        saveEvent = new Button("???????????????????? ??????????????????");
        saveEvent.setOnAction(e -> {
            try {
                doIO(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        openJournal = new Button("?????????????? ??????????????????????");
        openJournal.setOnAction(e -> {
            try {
                readJournal(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        stats = new Button("????????????????????");
        stats.setOnAction(e -> {
            try {
                getStats(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        export = new Button("?????????????? ???? ????????????");
        Tooltip tip = new Tooltip("?????????????? ?????? ?????????????????????? ?????? ?????? ?????????????????????? ???? ???????????? ????????????????");
        export.setTooltip(tip);
        export.setOnAction(e -> {
            try {
                exportTxt(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        HBox buttons = new HBox();
        buttons.setSpacing(8);
        buttons.getChildren().addAll(saveEvent, openJournal, stats, export);

        Separator sep3 = new Separator();
        sep3.setOrientation(Orientation.HORIZONTAL);
        sep3.setValignment(VPos.CENTER);

        evPerMonthLabel = new Label("???????????????? ?????? 30 ????????????: ");
        evPerMonthLabel.setMinHeight(32 * sizeFactor);
        lastMonthEventsLabel = new Label("???????????????? ?????? ?????????????????? ????????: ");
        lastMonthEventsLabel.setMinHeight(64 * sizeFactor);
        durMeanLabel = new Label("???????? ????????????????: ");
        intMeanLabel = new Label("???????? ????????????: ");
        HBox means = new HBox();
        means.setSpacing(16);
        means.getChildren().addAll(durMeanLabel, intMeanLabel);

        VBox summary = new VBox();
        summary.setPadding(new Insets(4, 0, 0, 0));
        summary.setSpacing(4);
        summary.setFillWidth(true);
        summary.getChildren().addAll(evPerMonthLabel, means, lastMonthEventsLabel);

        Separator sep4 = new Separator();
        sep4.setOrientation(Orientation.HORIZONTAL);
        sep4.setValignment(VPos.CENTER);

        themeLbl = new Label("???????? ??????????????????:");
        lightBtn = new RadioButton("??????????????");
        darkBtn = new RadioButton("????????????");
        group = new ToggleGroup();
        lightBtn.setToggleGroup(group);
        darkBtn.setToggleGroup(group);

        infoBtn = new Button("??????????????????????");
        infoBtn.setOnAction(e -> aboutClicked(stage));

        HBox infoBox = new HBox();
        infoBox.getChildren().addAll(themeLbl, lightBtn, darkBtn, infoBtn);
        infoBox.setAlignment(Pos.CENTER_RIGHT);
        infoBox.setSpacing(8);

        VBox box = new VBox();
        box.setPadding(new Insets(8));
        box.setSpacing(12);
        box.getChildren().addAll(details, placeLabel, radioBox, symptomsLabel, symptomBox, sep1,
                factorsLabel, circumBox, sep2, medicationsLabel, mediBox, notesArea, buttons, savedInfoLabel, sep3,
                summary, sep4, infoBox);

        Scene scene = new Scene(box, defWidth, defHeight);
        stage.setScene(scene);
        stage.setMinWidth(defWidth);
        stage.setMinHeight(defHeight);
        stage.setTitle("EmiCal");
        stage.getIcons().add(new Image("EmiCal.png"));
        scene.getStylesheets().add("application.css");

        lightBtn.setOnAction(e -> scene.getStylesheets().remove("dark-theme.css"));
        darkBtn.setOnAction(e -> scene.getStylesheets().add("dark-theme.css"));

        getPrefs(stage);
        read();
        stage.setOnCloseRequest(e -> {
            if (checkSelected()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.initOwner(stage);
                alert.setTitle("???? ???????????????????????? ????????????");
                alert.setHeaderText("?????????????? ???? ???????????????????????? ????????????.");
                alert.setContentText("???????????? ???? ???????????????? ???? ?????????????????? ?????????? ???????????????????? ?????? ??????????????????;");

                ButtonType no = new ButtonType("??????, ??????????????????");
                ButtonType yes = new ButtonType("??????, ????????????????");
                alert.getButtonTypes().setAll(no, yes);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.orElseThrow() == no)
                    e.consume();
            }
            setPrefs(stage);
        });
        stage.show();
    }

    private boolean checkSelected() {
        if (!Objects.equals(calendar.getValue(), LocalDate.now()) ||
                spinner.getValue() > 1 ||
                intensitySlider.getValue() > 1 ||
                !notesArea.getText().isEmpty())
            return true;
        for (var btn : radios)
            if (btn.isSelected())
                return true;
        for (var b : Arrays.asList(symptomBoxes, factorBoxes, mediBoxes))
            for (var box : b)
                if (box.isSelected())
                    return true;
        return false;
    }

    private void exportTxt(Stage stage) throws IOException {
        File file = new File(getUserDataDirectory() + "migraineCalendar.txt");
        StringBuilder textToExtract = new StringBuilder();
        if (file.exists()) {
            InputStream in = new FileInputStream(getUserDataDirectory() + "migraineCalendar.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null)
                textToExtract.append(line.concat("\n"));
        }
        String textToExport = "????????????????????:" + textToExtract + '\n' +
                "\n????????????????????:\n" + createStats();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("???????????????????? ??????????????");
        fileChooser.setInitialFileName("migraine_report.txt");
        File exp = fileChooser.showSaveDialog(stage);

        if (exp != null) {
            Alert alert;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(exp, StandardCharsets.UTF_8, false))) {
                bw.write(textToExport);
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setResizable(true);
                alert.setTitle("?????????????? ??????????????");
                alert.setHeaderText("???????????????????? ??????????????");
                alert.setContentText("???? ???????????? ???????????????????????? ???? ????????????????.");
                alert.initOwner(stage);
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                alert = new Alert(Alert.AlertType.WARNING);
                alert.setResizable(true);
                alert.setTitle("?????????????? ??????????????");
                alert.setHeaderText("???????????????? ?????????????????????? ??????????????");
                alert.setContentText("?????? ???????? ???????????? ?? ???????????????? ???????????????????? ?????? ??????????????.");
                alert.initOwner(stage);
                alert.showAndWait();
            }
        }
    }

    private void aboutClicked(Stage stage) {
        String info = """
                Program created by Dimitris Psathas

                Written in Java, utilizing the JavaFX toolkit

                Published under the GPLv3 License
                
                Application icon by freepik.com

                \u00A9 2021 Dimitris Psathas""";

        Alert infoDialog = new Alert(Alert.AlertType.INFORMATION);
        infoDialog.setResizable(true);
        infoDialog.setTitle("?????????????????????? ????????????????????????");
        infoDialog.setHeaderText("Emical");
        infoDialog.setContentText(info);
        infoDialog.initOwner(stage);
        infoDialog.showAndWait();
    }

    private void readJournal(Stage stage) throws IOException {
        Stage journal = new Stage();
        journal.setTitle("???????????????????? ??????????????????????");
        journal.setResizable(true);

        Label label = new Label("?????????????????????? ??????????????????????");
        ToggleButton button = new ToggleButton("????????????????????????????????");
        button.setSelected(false);
        HBox hBox = new HBox();
        hBox.getChildren().addAll(label, button);
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(8);

        TextArea text = new TextArea();
        text.setEditable(false);
        text.setMinHeight(256);

        File file = new File(getUserDataDirectory() + "migraineCalendar.txt");
        if (file.exists()) {
            InputStream in = new FileInputStream(getUserDataDirectory() + "migraineCalendar.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            StringBuilder textToDisplay = new StringBuilder();
            while ((line = reader.readLine()) != null)
                textToDisplay.append(line.concat("\n"));
            text.setText(textToDisplay.toString());

            button.setOnAction(e -> {
                if (button.isSelected()) {
                    button.setText("????????????????????????????");
                    text.setEditable(true);
                } else {
                    button.setText("????????????????????????????????");
                    text.setEditable(false);
                }
            });

            Button OK = new Button("???????????????????? ?????????????? ?????? ????????????");
            OK.setOnAction(e -> {
                try {
                    File f = new File(getUserDataDirectory() + "migraineCalendar.txt");
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, StandardCharsets.UTF_8, false))) {
                        final String txt = text.getText();
                        bw.write(txt);
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setResizable(true);
                    alert.setTitle("????????????");
                    alert.setHeaderText("???????????? ???????? ????????????????????");
                    alert.setContentText("???? ?????????????? ?????? ???????????????? ???? ????????????????????????." +
                            "\n???????????? ????????????:\n" + ioException);
                    alert.initOwner(stage);
                    alert.showAndWait();
                }
                journal.close();
                try {
                    read();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            Button Cancel = new Button("????????????");
            Cancel.setOnAction(e -> {
                if (!textToDisplay.toString().equals(text.getText())) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("???? ?????????????????????????? ??????????????");
                    alert.setHeaderText("???????????????? ???? ?????????????????????????? ??????????????.");
                    alert.setContentText("???????????? ???? ???????????????? ???? ???????????????????? ?????????? ???????????????????? ?????? ??????????????;");

                    ButtonType no = new ButtonType("??????, ??????????????????");
                    ButtonType yes = new ButtonType("??????, ????????????????");
                    alert.getButtonTypes().setAll(no, yes);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.orElseThrow() == no) {
                        e.consume();
                    } else {
                        journal.close();
                    }
                } else {
                    journal.close();
                }
            });

            VBox vBox = new VBox();
            vBox.getChildren().addAll(hBox, text, OK, Cancel);
            vBox.setAlignment(Pos.CENTER);
            vBox.setSpacing(8);
            vBox.setPadding(new Insets(8, 8, 8, 8));

            Scene scene = new Scene(vBox);
            journal.setScene(scene);
            scene.getStylesheets().add("application.css");
            if (darkBtn.isSelected())
                scene.getStylesheets().add("dark-theme.css");
            journal.initModality(Modality.APPLICATION_MODAL);
            journal.initOwner(stage);
            journal.show();
        } else {
            noCalendar(stage);
        }
    }

    private void read() throws IOException {
        File f = new File(getUserDataDirectory() + "migraineCalendar.txt");
        if (f.exists()) {
            InputStream in = new FileInputStream(getUserDataDirectory() + "migraineCalendar.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            ArrayList<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null)
                if (line.startsWith("????????????"))
                    lines.add(line);

            if (!lines.isEmpty()) {
                String firstLine = lines.get(0);
                String[] firstDateSplStr = firstLine.split(": ")[1].split("-");

                int firstYear = Integer.parseInt(firstDateSplStr[0]);
                int firstMonth = Integer.parseInt(firstDateSplStr[1]);
                int firstDay = Integer.parseInt(firstDateSplStr[2]);
                LocalDate firstDate = LocalDate.of(firstYear, firstMonth, firstDay);

                Period period = Period.between(firstDate, LocalDate.now());
                daysPassed = period.getYears() * 365 + period.getMonths() * 30 + period.getDays() + 1;
                String calStarted = "\n(???? ???????????????????? ???????????????? ???????? " +
                        firstDate.getDayOfMonth() + "-" + firstDate.getMonthValue() + "-" + firstDate.getYear() + ")";
                evPerMonthNum = lines.size() * 30.0 / daysPassed;
                if (daysPassed < 30) {
                    evPerMonthLabel.setText("???????????????? ?????? ????????????: " + lines.size() + calStarted);
                } else {
                    evPerMonthLabel.setText("???????????????? ?????? 30 ????????????: " + df.format(evPerMonthNum) + calStarted);
                }

                double totalDurations = 0;
                double totalIntensities = 0;

                lastMonthEvents = 0;
                lastMonthDurations = 0;
                lastMonthIntensities = 0;

                for (var event : lines) {
                    String[] dateSplStr = event.split(": ")[1].split("-");
                    int evtYear = Integer.parseInt(dateSplStr[0]);
                    int evtMonth = Integer.parseInt(dateSplStr[1]);
                    int evtDay = Integer.parseInt(dateSplStr[2]);
                    LocalDate evtDate = LocalDate.of(evtYear, evtMonth, evtDay);

                    String[] evtStr = event.split(": ");
                    String[] durStr = evtStr[2].split(" ????");
                    double dur = Double.parseDouble(durStr[0]);
                    totalDurations += dur;
                    double intns = Double.parseDouble(evtStr[3]);
                    totalIntensities += intns;

                    Period lastMonth = Period.between(evtDate, LocalDate.now());
                    if (lastMonth.getMonths() < 1) {
                        lastMonthEvents++;
                        lastMonthDurations += dur;
                        lastMonthIntensities += intns;
                    }
                }

                meanLastMonthDuration = lastMonthDurations / lastMonthEvents;
                meanLastMonthIntensity = lastMonthIntensities / lastMonthEvents;

                meanDuration = totalDurations / lines.size();
                durMeanLabel.setText("???????? ????????????????: " + df.format(meanDuration) + " ????????");
                meanIntensity = totalIntensities / lines.size();
                intMeanLabel.setText("???????? ????????????: " + df.format(meanIntensity));

                String days;
                if (daysPassed < 30 && daysPassed > 1) {
                    days = "?????? ???????????????????? " + daysPassed + " ????????????: ";
                } else {
                    days = "?????? ?????????????????? ????????: ";
                }
                lastMonthReport = "\n???????????????? " + days + lastMonthEvents;
                if (meanDuration > 1) {
                    lastMonthReport += "\n???????? ???????????????? " + days + df.format(meanLastMonthDuration) + " ????????";
                } else {
                    lastMonthReport += "\n???????? ???????????????? " + days + "1 ??????";
                }
                lastMonthReport += "\n???????? ???????????? " + days + df.format(meanLastMonthIntensity);
                lastMonthEventsLabel.setText(lastMonthReport);
            }
        }
    }

    private void getStats(Stage stage) throws IOException {
        File f = new File(getUserDataDirectory() + "migraineCalendar.txt");
        if (f.exists()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setResizable(true);
            alert.setTitle("????????????????????");
            alert.setHeaderText("???????????????????? ??????????????????????");
            alert.setContentText(createStats());
            alert.initOwner(stage);
            alert.showAndWait();
        } else {
            noCalendar(stage);
        }
    }

    private String createStats() throws IOException {
        Box events = new Box();
        Box left = new Box();
        Box front = new Box();
        Box right = new Box();
        Box combination = new Box();
        Box aura = new Box();
        Box photo = new Box();
        Box sound = new Box();
        Box vertigo = new Box();
        Box nausea = new Box();
        Box neck = new Box();
        Box sleep = new Box();
        Box stress = new Box();
        Box fatigue = new Box();

        read();

        InputStream in = new FileInputStream(getUserDataDirectory() + "migraineCalendar.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        String[] lineStarts = {
                "????????????", "-- ????????????????", "-- ??????????????", "-- ??????????", "-- ????????????????????",
                "-- ????????", "-- ??????????????????", "-- ????????????????", "-- ??????????????", "-- ????????????/????????????",
                "-- ??????????????", "-- ?????????? ??????????", "-- ??????????/??????????", "-- ????????????"};
        Box[] boxes = {
                events, left, front, right, combination,
                aura, photo, sound, vertigo, nausea,
                neck, sleep, stress, fatigue};

        String line;
        while ((line = reader.readLine()) != null)
            for (int i = 0; i < lineStarts.length; i++)
                if (line.startsWith(lineStarts[i]))
                    boxes[i].addOne();

        StringBuilder report;
        if (daysPassed < 30) {
            report = new StringBuilder("???????????????? ?????? ????????????: " + (int) events.value +
                    " (" + df.format(events.value / daysPassed * 100) + "% ?????? ????????????)\n");
        } else {
            report = new StringBuilder("???????????????? ?????? 30 ????????????: " + df.format(evPerMonthNum) +
                    " (" + df.format(events.value / daysPassed * 100) + "% ?????? ????????????)\n");
        }
        if (meanDuration > 1) {
            report.append("???????? ????????????????: ").append(df.format(meanDuration)).append(" ????????\n");
        } else {
            report.append("???????? ????????????????: 1 ??????\n");
        }
        report.append("???????? ????????????: ").append(df.format(meanIntensity)).append('\n');

        if (left.value > 0 || front.value > 0 || right.value > 0 || combination.value > 0) {
            report.append("\n???????????????????? ??????????:\n");
            for (int i = 1; i < 5; i++)
                if (boxes[i].value > 0)
                    report.append(lineStarts[i].split("-- ")[1]).append(": ")
                            .append(df.format(boxes[i].value / events.value * 100L)).append("%\n");
        }
        if (aura.value > 0 || photo.value > 0 || sound.value > 0 || vertigo.value > 0 || nausea.value > 0) {
            report.append("\n?????????????????????????? ????????????????????:\n");
            for (int j = 5; j < 10; j++)
                if (boxes[j].value > 0)
                    report.append(lineStarts[j].split("-- ")[1]).append(": ")
                            .append(df.format(boxes[j].value / events.value * 100L)).append("%\n");
        }
        if (neck.value > 0 || sleep.value > 0 || stress.value > 0 || fatigue.value > 0) {
            report.append("\n?????????????????????????? ????????????????????:\n");
            for (int k = 10; k < 14; k++)
                if (boxes[k].value > 0)
                    report.append(lineStarts[k].split("-- ")[1]).append(": ")
                            .append(df.format(boxes[k].value / events.value * 100L)).append("%\n");
        }

        report.append(lastMonthReport);

        return report.toString();
    }

    private void noCalendar(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("?????????????? ??????????????????????");
        alert.setHeaderText("?????? ?????????????? ????????????????????");
        alert.setContentText("?????? ???????? ???????????????? ?????????? ???????????? ????????????.\n" +
                "?????? ???? ?????????? ???????????????? ?????? ??????????????????????, ???????????? ?????????? ???? ???????????????? ?????? ????????????.");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private boolean isChecked(String kind) {
        radioTexts = new ArrayList<>();
        symptomTexts = new ArrayList<>();
        boxTexts = new ArrayList<>();
        mediTexts = new ArrayList<>();
        if (kind.equals("radio")) {
            for (var radio : radios)
                if (radio.isSelected())
                    radioTexts.add(radio.getText());
            return radioTexts != null;
        }
        if (kind.equals("symptom")) {
            for (var box : symptomBoxes)
                if (box.isSelected())
                    symptomTexts.add(box.getText());
            return symptomTexts != null;
        }
        if (kind.equals("box")) {
            for (var box : factorBoxes)
                if (box.isSelected())
                    boxTexts.add(box.getText());
            return boxTexts != null;
        }
        if (kind.equals("medi")) {
            for (var box : mediBoxes) {
                if (box.isSelected())
                    mediTexts.add(box.getText());
            }
            if (other.isSelected())
                if (!otherMed.getText().isEmpty()) {
                    mediTexts.add(otherMed.getText());
                } else {
                    mediTexts.add("???????? ??????????????");
                }
            return mediTexts != null;
        }
        return false;
    }

    private boolean update(Stage stage) {
        try {
            newEventDuration = Integer.parseInt(String.valueOf(spinner.getValue()));
            newEventIntensity = Integer.parseInt(String.valueOf(Math.round(intensitySlider.getValue())));
            if (selDate == null)
                selDate = LocalDate.now();
            event = new MigraineEvent(selDate, newEventDuration, newEventIntensity);
            savedInfoLabel.setText(event.toFormattedString());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setResizable(true);
            alert.setTitle("?????? ????????????");
            alert.setHeaderText("???????????????????? ??????????????????");
            alert.setContentText("???? ???????????? ???????????????????????? ???? ????????????????.");
            alert.initOwner(stage);
            alert.showAndWait();
            calendar.setValue(LocalDate.now());
            spinner.getValueFactory().setValue(1);
            intensitySlider.setValue(1);
            return true;
        } catch (Exception exception) {
            savedInfoLabel.setText("?????? ?????????????????????? ???????????? ????????????");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setResizable(true);
            alert.setTitle("?????? ????????????");
            alert.setHeaderText("???????????????? ??????????????????????");
            alert.setContentText("?????? ???????????????????????? ???????????? ????????????.");
            alert.initOwner(stage);
            alert.showAndWait();
            return false;
        }
    }

    public static String getUserDataDirectory() {
        return System.getProperty("user.home") + File.separator +
                ".dp_software" + File.separator + "EmiCal" + File.separator;
    }

    private void doIO(Stage stage) throws IOException {
        if (update(stage)) {
            File f = new File(getUserDataDirectory() + "migraineCalendar.txt");
            if (!f.exists()) {
                Path path = Paths.get(getUserDataDirectory());
                Files.createDirectories(path);
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, StandardCharsets.UTF_8, true))) {
                bw.write("\n????????????: " + event.toString());
                if (isChecked("radio"))
                    for (var radioText : radioTexts)
                        bw.write("\n-- " + radioText);
                if (isChecked("symptom"))
                    for (var symptomText : symptomTexts)
                        bw.write("\n-- " + symptomText);
                if (isChecked("box"))
                    for (var boxText : boxTexts)
                        bw.write("\n-- " + boxText);
                if (isChecked("medi"))
                    for (var mediText : mediTexts)
                        bw.write("\n-- " + mediText);
                if (!notesArea.getText().isEmpty())
                    bw.write("\n-- " + notesArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
            read();
            for (var btn : radios)
                if (btn.isSelected())
                    btn.setSelected(false);
            for (var b : Arrays.asList(symptomBoxes, factorBoxes, mediBoxes))
                for (var box : b)
                    if (box.isSelected())
                        box.setSelected(false);
            otherMed.clear();
            notesArea.clear();
        } else {
            System.err.println("Update unsuccessful, no IO done.");
        }
    }

    private void setPrefs(Stage stage) {
        prefs.put("locationX", String.valueOf(stage.getX()));
        prefs.put("locationY", String.valueOf(stage.getY()));
        prefs.put("width", String.valueOf(stage.getWidth()));
        prefs.put("height", String.valueOf(stage.getHeight()));
        if (lightBtn.isSelected()) {
            prefs.put("theme", "light");
        } else {
            prefs.put("theme", "dark");
        }
    }

    private void getPrefs(Stage stage) {
        final String theme = prefs.get("theme", "light");
        if (Objects.equals(theme, "dark")) {
            darkBtn.setSelected(true);
            stage.getScene().getStylesheets().add("dark-theme.css");
        } else {
            lightBtn.setSelected(true);
            stage.getScene().getStylesheets().remove("dark-theme.css");
        }
        final double savedX = Double.parseDouble(prefs.get("locationX", "128.0"));
        final double savedY = Double.parseDouble(prefs.get("locationY", "64.0"));
        stage.setX(savedX);
        stage.setY(savedY);
        final double savedWidth = Double.parseDouble(prefs.get("width", String.valueOf(defWidth)));
        final double savedHeight = Double.parseDouble(prefs.get("height", String.valueOf(defHeight)));
        stage.setWidth(savedWidth);
        stage.setHeight(savedHeight);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
