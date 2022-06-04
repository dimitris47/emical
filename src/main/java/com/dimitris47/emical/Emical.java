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

        durationLabel = new Label("Διάρκεια (ώρες)");
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

        intensityLabel = new Label("Ένταση");
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

        placeLabel = new Label("Εντοπισμός πόνου");
        radios = new ArrayList<>();
        ToggleGroup toggleGroup = new ToggleGroup();
        radLeft = new RadioButton("αριστερά");
        radCenter = new RadioButton("μπροστά");
        radRight = new RadioButton("δεξιά");
        radCombined = new RadioButton("συνδυασμός");
        radios.addAll(Arrays.asList(radLeft, radCenter, radRight, radCombined));
        for (var radio : radios)
            radio.setToggleGroup(toggleGroup);
        HBox radioBox = new HBox();
        radioBox.setSpacing(12);
        radioBox.getChildren().addAll(radLeft, radCenter, radRight, radCombined);

        symptomsLabel = new Label("Επιπλέον συμπτώματα");
        symptomBoxes = new ArrayList<>();
        aura = new CheckBox("αύρα");
        symptomBoxes.add(aura);
        photophobia = new CheckBox("φωτοφοβία");
        soundSens = new CheckBox("ηχοφοβία");
        vertigo = new CheckBox("ίλιγγος");
        nausea = new CheckBox("ναυτία/έμετος");
        symptomBoxes.addAll(Arrays.asList(aura, photophobia, soundSens, vertigo, nausea));
        for (var box : symptomBoxes)
            box.setAllowIndeterminate(false);
        HBox symptomBox = new HBox();
        symptomBox.setSpacing(12);
        symptomBox.getChildren().addAll(aura, photophobia, soundSens, vertigo, nausea);

        Separator sep1 = new Separator();
        sep1.setOrientation(Orientation.HORIZONTAL);
        sep1.setValignment(VPos.CENTER);

        factorsLabel = new Label("Επιβαρυντικοί παράγοντες");
        factorBoxes = new ArrayList<>();
        neckProblems = new CheckBox("αυχένας");
        badSleep = new CheckBox("κακός ύπνος");
        stress = new CheckBox("άγχος/στρες");
        fatigue = new CheckBox("κόπωση");
        factorBoxes.addAll(Arrays.asList(neckProblems, badSleep, stress, fatigue));
        for (var box : factorBoxes)
            box.setAllowIndeterminate(false);
        HBox circumBox = new HBox();
        circumBox.setSpacing(12);
        circumBox.getChildren().addAll(neckProblems, badSleep, stress, fatigue);

        Separator sep2 = new Separator();
        sep2.setOrientation(Orientation.HORIZONTAL);
        sep2.setValignment(VPos.CENTER);

        medicationsLabel = new Label("Φάρμακα");
        mediBoxes = new ArrayList<>();
        depon = new CheckBox("Depon");
        ponstan = new CheckBox("Ponstan");
        imigran = new CheckBox("Imigran");
        rizatriptan = new CheckBox("Rizatriptan");
        other = new CheckBox("Άλλο");
        mediBoxes.addAll(Arrays.asList(depon, ponstan, imigran, rizatriptan, other));
        for (var box : mediBoxes)
            box.setAllowIndeterminate(false);
        otherMed = new TextField();
        otherMed.setPromptText("διευκρινήστε");
        otherMed.setOnKeyTyped(e -> other.setSelected(!otherMed.getText().equals("")));

        HBox mediBox = new HBox();
        mediBox.setSpacing(12);
        mediBox.setAlignment(Pos.CENTER_LEFT);
        mediBox.getChildren().addAll(depon, ponstan, imigran, rizatriptan, other, otherMed);

        notesArea = new TextArea();
        notesArea.setPromptText("Γράψτε σημειώσεις εδώ");

        savedInfoLabel = new Label();

        saveEvent = new Button("Αποθήκευση συμβάντος");
        saveEvent.setOnAction(e -> {
            try {
                doIO(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        openJournal = new Button("Άνοιγμα ημερολογίου");
        openJournal.setOnAction(e -> {
            try {
                readJournal(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        stats = new Button("Στατιστικά");
        stats.setOnAction(e -> {
            try {
                getStats(stage);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        export = new Button("Εξαγωγή σε αρχείο");
        Tooltip tip = new Tooltip("Εξαγωγή του ημερολογίου και των στατιστικών σε αρχείο κειμένου");
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

        evPerMonthLabel = new Label("Συμβάντα ανά 30 ημέρες: ");
        evPerMonthLabel.setMinHeight(32 * sizeFactor);
        lastMonthEventsLabel = new Label("Γεγονότα τον τελευταίο μήνα: ");
        lastMonthEventsLabel.setMinHeight(64 * sizeFactor);
        durMeanLabel = new Label("Μέση διάρκεια: ");
        intMeanLabel = new Label("Μέση ένταση: ");
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

        themeLbl = new Label("Θέμα εμφάνισης:");
        lightBtn = new RadioButton("Ανοιχτό");
        darkBtn = new RadioButton("Σκούρο");
        group = new ToggleGroup();
        lightBtn.setToggleGroup(group);
        darkBtn.setToggleGroup(group);

        infoBtn = new Button("Πληροφορίες");
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
                alert.setTitle("Μη αποθηκευμένο συμβάν");
                alert.setHeaderText("Υπάρχει μη αποθηκευμένο συμβάν.");
                alert.setContentText("θέλετε να κλείσετε το πρόγραμμα χωρίς αποθήκευση του συμβάντος;");

                ButtonType no = new ButtonType("Όχι, επιστροφή");
                ButtonType yes = new ButtonType("Ναι, κλείσιμο");
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
        String textToExport = "Ημερολόγιο:" + textToExtract + '\n' +
                "\nΣτατιστικά:\n" + createStats();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Αποθήκευση αρχείου");
        fileChooser.setInitialFileName("migraine_report.txt");
        File exp = fileChooser.showSaveDialog(stage);

        if (exp != null) {
            Alert alert;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(exp, false))) {
                bw.write(textToExport);
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setResizable(true);
                alert.setTitle("Εξαγωγή αρχείου");
                alert.setHeaderText("Αποθήκευση αρχείου");
                alert.setContentText("Το αρχείο αποθηκεύτηκε με επιτυχία.");
                alert.initOwner(stage);
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                alert = new Alert(Alert.AlertType.WARNING);
                alert.setResizable(true);
                alert.setTitle("Εξαγωγή αρχείου");
                alert.setHeaderText("Αποτυχία δημιουργίας αρχείου");
                alert.setContentText("Δεν ήταν δυνατή η επιτυχής δημιουργία του αρχείου.");
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
        infoDialog.setTitle("Πληροφορίες προγράμματος");
        infoDialog.setHeaderText("Emical");
        infoDialog.setContentText(info);
        infoDialog.initOwner(stage);
        infoDialog.showAndWait();
    }

    private void readJournal(Stage stage) throws IOException {
        Stage journal = new Stage();
        journal.setTitle("Ημερολόγιο κεφαλαλγίας");
        journal.setResizable(true);

        Label label = new Label("Επεξεργασία ημερολογίου");
        ToggleButton button = new ToggleButton("Απενεργοποιημένη");
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
                    button.setText("Ενεργοποιημένη");
                    text.setEditable(true);
                } else {
                    button.setText("Απενεργοποιημένη");
                    text.setEditable(false);
                }
            });

            Button OK = new Button("Αποθήκευση αλλαγών και έξοδος");
            OK.setOnAction(e -> {
                try {
                    File f = new File(getUserDataDirectory() + "migraineCalendar.txt");
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
                        bw.write(text.getText());
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setResizable(true);
                    alert.setTitle("Σφάλμα");
                    alert.setHeaderText("Σφάλμα στην αποθήκευση");
                    alert.setContentText("Οι αλλαγές δεν μπόρεσαν να αποθηκευτούν." +
                            "\nΜήνυμα λάθους:\n" + ioException);
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

            Button Cancel = new Button("Έξοδος");
            Cancel.setOnAction(e -> {
                if (!textToDisplay.toString().equals(text.getText())) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Μη αποθηκευμένες αλλαγές");
                    alert.setHeaderText("Υπάρχουν μη αποθηκευμένες αλλαγές.");
                    alert.setContentText("θέλετε να κλείσετε το ημερολόγιο χωρίς αποθήκευση των αλλαγών;");

                    ButtonType no = new ButtonType("Όχι, επιστροφή");
                    ButtonType yes = new ButtonType("Ναι, κλείσιμο");
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
                if (line.startsWith("Συμβάν"))
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
                String calStarted = "\n(το ημερολόγιο ξεκίνησε στις " +
                        firstDate.getDayOfMonth() + "-" + firstDate.getMonthValue() + "-" + firstDate.getYear() + ")";
                evPerMonthNum = lines.size() * 30.0 / daysPassed;
                if (daysPassed < 30) {
                    evPerMonthLabel.setText("Συμβάντα έως σήμερα: " + lines.size() + calStarted);
                } else {
                    evPerMonthLabel.setText("Συμβάντα ανά 30 ημέρες: " + df.format(evPerMonthNum) + calStarted);
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
                    String[] durStr = evtStr[2].split(" ώρ");
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
                durMeanLabel.setText("Μέση διάρκεια: " + df.format(meanDuration) + " ώρες");
                meanIntensity = totalIntensities / lines.size();
                intMeanLabel.setText("Μέση ένταση: " + df.format(meanIntensity));

                String days;
                if (daysPassed < 30 && daysPassed > 1) {
                    days = "τις τελευταίες " + daysPassed + " ημέρες: ";
                } else {
                    days = "τον τελευταίο μήνα: ";
                }
                lastMonthReport = "\nΣυμβάντα " + days + lastMonthEvents;
                if (meanDuration > 1) {
                    lastMonthReport += "\nΜέση διάρκεια " + days + df.format(meanLastMonthDuration) + " ώρες";
                } else {
                    lastMonthReport += "\nΜέση διάρκεια " + days + "1 ώρα";
                }
                lastMonthReport += "\nΜέση ένταση " + days + df.format(meanLastMonthIntensity);
                lastMonthEventsLabel.setText(lastMonthReport);
            }
        }
    }

    private void getStats(Stage stage) throws IOException {
        File f = new File(getUserDataDirectory() + "migraineCalendar.txt");
        if (f.exists()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setResizable(true);
            alert.setTitle("Στατιστικά");
            alert.setHeaderText("Στατιστικά κεφαλαλγίας");
            alert.setContentText(createStats());
            alert.initOwner(stage);
            alert.showAndWait();
        } else {
            noCalendar(stage);
        }
    }

    private String createStats() throws IOException {
        double events = 0,
                left = 0, front = 0, right = 0, combination = 0,
                aura = 0, photo = 0, sound = 0, vertigo = 0, nausea = 0,
                neck = 0, sleep = 0, stress = 0, fatigue = 0;
        read();

        InputStream in = new FileInputStream(getUserDataDirectory() + "migraineCalendar.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Συμβάν"))
                events++;
            if (line.startsWith("-- αριστερά"))
                left++;
            if (line.startsWith("-- μπροστά"))
                front++;
            if (line.startsWith("-- δεξιά"))
                right++;
            if (line.startsWith("-- συνδυασμός"))
                combination++;
            if (line.startsWith("-- αύρα"))
                aura++;
            if (line.startsWith("-- φωτοφοβία"))
                photo++;
            if (line.startsWith("-- ηχοφοβία"))
                sound++;
            if (line.startsWith("-- ίλιγγος"))
                vertigo++;
            if (line.startsWith("-- ναυτία/έμετος"))
                nausea++;
            if (line.startsWith("-- αυχένας"))
                neck++;
            if (line.startsWith("-- κακός ύπνος"))
                sleep++;
            if (line.startsWith("-- άγχος/στρες"))
                stress++;
            if (line.startsWith("-- κόπωση"))
                fatigue++;
        }

        String report;

        if (daysPassed < 30) {
            report = "Συμβάντα έως σήμερα: " + (int) events +
                    " (" + df.format(events / daysPassed * 100) + "% των ημερών)\n";
        } else {
            report = "Συμβάντα ανά 30 ημέρες: " + df.format(evPerMonthNum) +
                    " (" + df.format(events / daysPassed * 100) + "% των ημερών)\n";
        }
        if (meanDuration > 1) {
            report += "Μέση διάρκεια: " + df.format(meanDuration) + " ώρες\n";
        } else {
            report += "Μέση διάρκεια: 1 ώρα\n";
        }
        report += "Μέση ένταση: " + df.format(meanIntensity) + '\n';

        if (left > 0 || front > 0 || right > 0 || combination > 0)
            report += "\nΕντοπισμός πόνου:\n";
        if (left > 0)
            report += "Αριστερά: " + df.format(left / events * 100L) + "%\n";
        if (front > 0)
            report += "Μπροστά: " + df.format(front / events * 100L) + "%\n";
        if (right > 0)
            report += "Δεξιά: " + df.format(right / events * 100L) + "%\n";
        if (combination > 0)
            report += "Συνδυασμός: " + df.format(combination / events * 100L) + "%\n";

        if (aura > 0 || photo > 0 || sound > 0 || vertigo > 0 || nausea > 0)
            report += "\nΕπιπλέον συμπτώματα:\n";
        if (aura > 0)
            report += "Αύρα: " + df.format(aura / events * 100L) + "%\n";
        if (photo > 0)
            report += "Φωτοφοβία: " + df.format(photo / events * 100L) + "%\n";
        if (sound > 0)
            report += "Ηχοφοβία: " + df.format(sound / events * 100L) + "%\n";
        if (vertigo > 0)
            report += "Ίλιγγος: " + df.format(vertigo / events * 100L) + "%\n";
        if (nausea > 0)
            report += "Ναυτία/έμετος: " + df.format(nausea / events * 100L) + "%\n";

        if (neck > 0 || sleep > 0 || stress > 0 || fatigue > 0)
            report += "\nΕπιβαρυντικοί παράγοντες:\n";
        if (neck > 0)
            report += "Αυχένας: " + df.format(neck / events * 100L) + "%\n";
        if (sleep > 0)
            report += "Κακός ύπνος: " + df.format(sleep / events * 100L) + "%\n";
        if (stress > 0)
            report += "Άγχος/στρες: " + df.format(stress / events * 100L) + "%\n";
        if (fatigue > 0)
            report += "Κόπωση: " + df.format(fatigue / events * 100L) + "%\n";

        report += lastMonthReport;

        return report;
    }

    private void noCalendar(Stage stage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Απουσία ημερολογίου");
        alert.setHeaderText("Δεν υπάρχει ημερολόγιο");
        alert.setContentText("Δεν έχει εισαχθεί ακόμη κάποιο συμβάν.\n" +
                "Για να γίνει εκκίνηση του ημερολογίου, πρέπει πρώτα να εισάγετε ένα συμβάν.");
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
                if (!otherMed.getText().isEmpty())
                    mediTexts.add(otherMed.getText());
                else
                    mediTexts.add("άλλο φάρμακο");
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
            alert.setTitle("Νέο συμβάν");
            alert.setHeaderText("Αποθήκευση συμβάντος");
            alert.setContentText("Το συμβάν αποθηκεύτηκε με επιτυχία.");
            alert.initOwner(stage);
            alert.showAndWait();
            calendar.setValue(LocalDate.now());
            spinner.getValueFactory().setValue(1);
            intensitySlider.setValue(1);
            return true;
        } catch (Exception exception) {
            savedInfoLabel.setText("Δεν καταγράφηκε κάποιο συμβάν");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setResizable(true);
            alert.setTitle("Νέο συμβάν");
            alert.setHeaderText("Αποτυχία αποθήκευσης");
            alert.setContentText("Δεν αποθηκεύτηκε κάποιο συμβάν.");
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
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
                bw.write("\nΣυμβάν: " + event.toString());
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
