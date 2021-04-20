package com.dimitris47.emical;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.prefs.Preferences;

public class Emical extends Application {
    Preferences prefs;
    static DecimalFormat df;
    double fontSize, sizeFactor, defWidth, defHeight;
    Font defFont;

    LocalDate selDate;
    int newEventDuration, newEventIntensity, daysPassed;
    double meanDuration, meanIntensity;

    Label lblDuration, lblIntensity;
    Label place, symptoms, factors, medications;
    Label savedInfo, evPerMonth, durMean, intMean;

    DatePicker calendar;
    Spinner<Integer> spinner;
    Slider intensitySlider;
    RadioButton radLeft, radCenter, radRight, radCombined;
    CheckBox aura, photophobia, soundSens, vertigo, nausea;
    CheckBox neck, badSleep, stress, fatigue;
    CheckBox depon, ponstan, algofren, imigran, other;
    TextField otherMed;
    TextArea notes;
    Button saveEvent, openJournal, stats, export, info;

    ArrayList<RadioButton> radios;
    ArrayList<String> radioTexts, symptomTexts, boxTexts, mediTexts;
    ArrayList<CheckBox> symptomBoxes, factorBoxes, mediBoxes;

    MigraineEvent migev;

    @Override
    public void start(Stage stage) throws IOException {
        String OS = System.getProperty("os.name");
        if (OS.contains("Windows"))
            stage.getIcons().add(new Image("file:EmiCal.ico"));
        else if (OS.contains("Linux"))
            stage.getIcons().add(new Image("file:EmiCal.png"));

        prefs = Preferences.userNodeForPackage(Emical.class);
        DecimalFormatSymbols dfSymbols = new DecimalFormatSymbols(Locale.getDefault());
        dfSymbols.setDecimalSeparator(',');
        dfSymbols.setGroupingSeparator('.');
        df = new DecimalFormat("#.#", dfSymbols);
        Rectangle2D screen = Screen.getPrimary().getBounds();
        if (screen.getWidth() >= 1920) {
            fontSize = 15;
            sizeFactor = 1.4;
        }
        else {
            fontSize = 11;
            sizeFactor = 1.2;
        }
        defFont = Font.font("Segoe UI", FontWeight.NORMAL, fontSize);
        defWidth = 460 * sizeFactor;
        defHeight = 528 * sizeFactor;
        Insets ins = new Insets(0, 0, 0, 8 * sizeFactor);

        calendar = new DatePicker();
        calendar.setValue(LocalDate.now());
        calendar.setConverter(new StringConverter<>() {
            final String pattern = "dd-MM-yyyy";
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
            { calendar.setPromptText(pattern.toLowerCase()); }
            @Override
            public String toString(LocalDate date) {
                if (date != null)
                    return dateFormatter.format(date);
                else
                    return "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty())
                    return LocalDate.parse(string, dateFormatter);
                else
                    return null;
            }
        });
        calendar.setOnAction(e -> selDate = calendar.getValue());
        calendar.getEditor().setFont(defFont);
        calendar.getEditor().setAlignment(Pos.CENTER);
        calendar.setPrefWidth(96 * sizeFactor);

        lblDuration = new Label("Διάρκεια (ώρες)");
        lblDuration.setMinWidth(80 * sizeFactor);
        lblDuration.setPadding(ins);
        lblDuration.setFont(defFont);
        spinner = new Spinner<>();
        spinner.setEditable(true);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1, 1));
        spinner.setPrefWidth(48 * sizeFactor);
        spinner.getEditor().setAlignment(Pos.CENTER);
        spinner.getEditor().setFont(defFont);
        spinner.getEditor().textProperty().addListener((observableValue, s, t1) -> {
            if (!t1.matches("\\d*"))
                spinner.getEditor().setText(t1.replaceAll("[^\\d]", ""));
        });
        spinner.setOnScroll((ScrollEvent e) -> {
            int deltaY = (int) e.getDeltaY();
            if (deltaY > 0)
                spinner.getValueFactory().setValue(spinner.getValue() + 1);
            else if (deltaY < 0)
                spinner.getValueFactory().setValue(spinner.getValue() - 1);
        });

        lblIntensity = new Label("Ένταση");
        lblIntensity.setMinWidth(48 * sizeFactor);
        lblIntensity.setPadding(ins);
        lblIntensity.setFont(defFont);
        intensitySlider = new Slider();
        intensitySlider.setShowTickLabels(true);
        intensitySlider.setMin(1);
        intensitySlider.setMax(10);
        HBox details = new HBox();
        details.setSpacing(4);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().addAll(calendar, lblDuration, spinner, lblIntensity, intensitySlider);

        place = new Label("Εντοπισμός πόνου");
        place.setFont(defFont);
        radios = new ArrayList<>();
        ToggleGroup toggleGroup = new ToggleGroup();
        radLeft = new RadioButton("αριστερά");
        radios.add(radLeft);
        radCenter = new RadioButton("μπροστά");
        radios.add(radCenter);
        radRight = new RadioButton("δεξιά");
        radios.add(radRight);
        radCombined = new RadioButton("συνδυασμός");
        radios.add(radCombined);
        for (var radio : radios) {
            radio.setToggleGroup(toggleGroup);
            radio.setFont(defFont);
        }
        HBox radioBox = new HBox();
        radioBox.setSpacing(12);
        radioBox.getChildren().addAll(radLeft, radCenter, radRight, radCombined);

        symptoms = new Label("Επιπλέον συμπτώματα");
        symptoms.setFont(defFont);
        symptomBoxes = new ArrayList<>();
        aura = new CheckBox("αύρα");
        symptomBoxes.add(aura);
        photophobia = new CheckBox("φωτοφοβία");
        symptomBoxes.add(photophobia);
        soundSens = new CheckBox("ηχοφοβία");
        symptomBoxes.add(soundSens);
        vertigo = new CheckBox("ίλιγγος");
        symptomBoxes.add(vertigo);
        nausea = new CheckBox("ναυτία/έμετος");
        symptomBoxes.add(nausea);
        for (var box : symptomBoxes) {
            box.setAllowIndeterminate(false);
            box.setFont(defFont);
        }
        HBox symptomBox = new HBox();
        symptomBox.setSpacing(12);
        symptomBox.getChildren().addAll(aura, photophobia, soundSens, vertigo, nausea);

        Separator sep1 = new Separator();
        sep1.setOrientation(Orientation.HORIZONTAL);
        sep1.setValignment(VPos.CENTER);

        factors = new Label("Επιβαρυντικοί παράγοντες");
        factors.setFont(defFont);
        factorBoxes = new ArrayList<>();
        neck = new CheckBox("αυχένας");
        factorBoxes.add(neck);
        badSleep = new CheckBox("κακός ύπνος");
        factorBoxes.add(badSleep);
        stress = new CheckBox("άγχος/στρες");
        factorBoxes.add(stress);
        fatigue = new CheckBox("κόπωση");
        factorBoxes.add(fatigue);
        for (var box : factorBoxes) {
            box.setAllowIndeterminate(false);
            box.setFont(defFont);
        }
        HBox circumBox = new HBox();
        circumBox.setSpacing(12);
        circumBox.getChildren().addAll(neck, badSleep, stress, fatigue);

        Separator sep2 = new Separator();
        sep2.setOrientation(Orientation.HORIZONTAL);
        sep2.setValignment(VPos.CENTER);

        medications = new Label("Φάρμακα");
        medications.setFont(defFont);
        mediBoxes = new ArrayList<>();
        depon = new CheckBox("Depon");
        mediBoxes.add(depon);
        ponstan = new CheckBox("Ponstan");
        mediBoxes.add(ponstan);
        algofren = new CheckBox("Algofren");
        mediBoxes.add(algofren);
        imigran = new CheckBox("Imigran");
        mediBoxes.add(imigran);
        other = new CheckBox("Άλλο");
        other.setFont(defFont);
        for (var box : mediBoxes) {
            box.setAllowIndeterminate(false);
            box.setFont(defFont);
        }
        otherMed = new TextField();
        otherMed.setPromptText("διευκρινήστε");
        otherMed.setFont(defFont);
        HBox mediBox = new HBox();
        mediBox.setSpacing(12);
        mediBox.setAlignment(Pos.CENTER_LEFT);
        mediBox.getChildren().addAll(depon, ponstan, algofren, imigran, other, otherMed);

        notes = new TextArea();
        notes.setFont(defFont);
        notes.setPromptText("Γράψτε σημειώσεις εδώ");

        savedInfo = new Label();
        savedInfo.setFont(defFont);

        saveEvent = new Button("Αποθήκευση συμβάντος");
        saveEvent.setFont(defFont);
        saveEvent.setOnAction(e -> {
            update();
            try { doIO(); }
            catch (IOException ioException) { ioException.printStackTrace(); }
        });

        openJournal = new Button("Άνοιγμα ημερολογίου");
        openJournal.setFont(defFont);
        openJournal.setOnAction(e -> {
            try { readJournal(stage); }
            catch (IOException ioException) { ioException.printStackTrace(); }
        });

        stats = new Button("Στατιστικά");
        stats.setFont(defFont);
        stats.setOnAction(e -> {
            try { getStats(stage); }
            catch (IOException ioException) { ioException.printStackTrace(); }
        });

        export = new Button("Εξαγωγή σε αρχείο");
        export.setFont(defFont);
        Tooltip tip = new Tooltip("Εξαγωγή του ημερολογίου και των στατιστικών σε αρχείο κειμένου");
        tip.setFont(defFont);
        export.setTooltip(tip);
        export.setOnAction(e -> {
            try { exportTxt(stage); }
            catch (IOException ioException) { ioException.printStackTrace(); }
        });

        HBox buttons = new HBox();
        buttons.setSpacing(8);
        buttons.getChildren().addAll(saveEvent, openJournal, stats, export);

        Separator sep3 = new Separator();
        sep3.setOrientation(Orientation.HORIZONTAL);
        sep3.setValignment(VPos.CENTER);

        evPerMonth = new Label("Συμβάντα ανά 30 ημέρες: ");
        evPerMonth.setMinHeight(32 * sizeFactor);
        evPerMonth.setFont(defFont);
        durMean = new Label("Μέσος όρος διάρκειας: ");
        durMean.setFont(defFont);
        intMean = new Label("Μέσος όρος έντασης: ");
        intMean.setFont(defFont);
        HBox means = new HBox();
        means.setSpacing(16);
        means.getChildren().addAll(durMean, intMean);

        VBox summary = new VBox();
        summary.setPadding(new Insets(4, 0, 0, 0));
        summary.setSpacing(4);
        summary.setFillWidth(true);
        summary.getChildren().addAll(evPerMonth, means);

        Separator sep4 = new Separator();
        sep4.setOrientation(Orientation.HORIZONTAL);
        sep4.setValignment(VPos.CENTER);

        info = new Button("Πληροφορίες");
        info.setFont(defFont);
        info.setOnAction(e -> aboutClicked(stage));

        HBox infoBox = new HBox();
        infoBox.getChildren().add(info);
        infoBox.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox();
        box.setPadding(new Insets(8));
        box.setSpacing(12);
        box.getChildren().addAll(details, place, radioBox, symptoms, symptomBox, sep1, factors, circumBox, sep2,
                medications, mediBox, notes, buttons, savedInfo, sep3, summary, sep4, infoBox);

        Scene scene = new Scene(box, defWidth, defHeight);
        stage.setScene(scene);
        stage.setMinWidth(defWidth);
        stage.setMinHeight(defHeight);
        stage.setTitle("Emical");

        getPrefs(stage);
        read();
        stage.setOnCloseRequest(e -> setPrefs(stage));
        stage.show();
    }

    private void exportTxt(Stage stage) throws IOException {
        File file = new File("migraineCalendar.txt");
        StringBuilder textToExtract = new StringBuilder();
        if (file.exists()) {
            InputStream in = new FileInputStream("migraineCalendar.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null)
                textToExtract.append(line.concat("\n"));
        }
        String textToExport = "Ημερολόγιο:" + textToExtract + "\n" +
                "Στατιστικά:\n" + createStats();

        File exp = new File("migraineReport.txt");
        Alert alert;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(exp, false))) {
            bw.write(textToExport);
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Εξαγωγή αρχείου");
            alert.setHeaderText("Αποθήκευση αρχείου");
            alert.setContentText("Το αρχείο αποθηκεύτηκε με επιτυχία.");
            alert.initOwner(stage);
            alert.showAndWait();
        }
        catch (IOException e) {
            e.printStackTrace();
            alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Εξαγωγή αρχείου");
            alert.setHeaderText("Αποτυχία δημιουργίας αρχείου");
            alert.setContentText("Δεν ήταν δυνατή η επιτυχής δημιουργία του αρχείου.");
            alert.initOwner(stage);
            alert.showAndWait();
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
        infoDialog.setTitle("Πληροφορίες προγράμματος");
        infoDialog.setHeaderText("Emical");
        infoDialog.setContentText(info);
        infoDialog.initOwner(stage);
        infoDialog.showAndWait();
    }

    private void readJournal(Stage stage) throws IOException {
        Dialog<String> journal = new Dialog<>();
        journal.setTitle("Ημερολόγιο κεφαλαλγίας");

        Label label = new Label("Επεξεργασία ημερολογίου");
        label.setFont(defFont);
        ToggleButton button = new ToggleButton("Απενεργοποιημένη");
        button.setFont(defFont);
        button.setSelected(false);
        HBox hBox = new HBox();
        hBox.getChildren().addAll(label, button);
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(8);

        TextArea text = new TextArea();
        text.setEditable(false);
        text.setMinHeight(256);
        text.setFont(defFont);

        File file = new File("migraineCalendar.txt");
        if (file.exists()) {
            InputStream in = new FileInputStream("migraineCalendar.txt");
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
            OK.setFont(defFont);
            OK.setOnAction(e -> {
                try {
                    File f = new File("migraineCalendar.txt");
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, false))) {
                        bw.write(text.getText());
                    }
                    read();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                journal.close();
            });

            VBox vBox = new VBox();
            vBox.getChildren().addAll(hBox, text, OK);
            vBox.setAlignment(Pos.CENTER);
            vBox.setSpacing(8);
            vBox.setPadding(new Insets(8, 8, 0, 8));

            journal.getDialogPane().setContent(vBox);
            ButtonType Cancel = new ButtonType("Έξοδος", ButtonBar.ButtonData.CANCEL_CLOSE);
            journal.getDialogPane().getButtonTypes().add(Cancel);
            journal.initModality(Modality.APPLICATION_MODAL);
            journal.initOwner(stage);
            journal.show();
        }
        else
            noCalendar(stage);
    }

    private void read() throws IOException {
        File f = new File("migraineCalendar.txt");
        if (f.exists()) {
            InputStream in = new FileInputStream("migraineCalendar.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            ArrayList<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null)
                if (line.startsWith("Συμβάν"))
                    lines.add(line);

            if (!lines.isEmpty()) {
                String firstLine = lines.get(0);
                String[] firstDateStr = firstLine.split(": ");
                String[] firstDateSplStr = firstDateStr[1].split("-");

                int firstYear = Integer.parseInt(firstDateSplStr[0]);
                int firstMonth = Integer.parseInt(firstDateSplStr[1]);
                int firstDay = Integer.parseInt(firstDateSplStr[2]);
                LocalDate firstDate = LocalDate.of(firstYear, firstMonth, firstDay);

                Period period = Period.between(firstDate, LocalDate.now());
                daysPassed = period.getYears() * 365 + period.getMonths() * 30 + period.getDays() + 1;
                String calStarted = "\n(το ημερολόγιο ξεκίνησε στις " +
                        firstDate.getDayOfMonth() + "-" + firstDate.getMonthValue() + "-" + firstDate.getYear() + ")";
                if (daysPassed < 30)
                    evPerMonth.setText("Συμβάντα έως σήμερα: " + lines.size() + calStarted);
                else
                    evPerMonth.setText("Συμβάντα ανά 30 ημέρες: " +
                            df.format(lines.size() * 30.0 / daysPassed) + calStarted);

                double totalDuration = 0;
                double totalIntensities = 0;

                for (var event : lines) {
                    String[] evtStr = event.split(": ");
                    String[] durStr = evtStr[2].split(" ώρ");
                    double dur = Double.parseDouble(durStr[0]);
                    totalDuration += dur;
                    double intns = Double.parseDouble(evtStr[3]);
                    totalIntensities += intns;
                }

                meanDuration = totalDuration / lines.size();
                durMean.setText("Μέσος όρος διάρκειας: " + df.format(meanDuration) + " ώρες");
                meanIntensity = totalIntensities / lines.size();
                intMean.setText("Μέσος όρος έντασης: " + df.format(meanIntensity));
            }
        }
    }

    private void getStats(Stage stage) throws IOException {
        File f = new File("migraineCalendar.txt");
        if (f.exists()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Στατιστικά");
            alert.setHeaderText("Στατιστικά κεφαλαλγίας");
            alert.setContentText(createStats());
            alert.initOwner(stage);
            alert.showAndWait();
        }
        else
            noCalendar(stage);
    }

    private String createStats() throws IOException {
        double events = 0;
        double left = 0, front = 0, right = 0, combination = 0;
        double aura = 0, photo = 0, sound = 0, vertigo = 0, nausea = 0;
        double neck = 0, sleep = 0, stress = 0, fatigue = 0;
        read();

        InputStream in = new FileInputStream("migraineCalendar.txt");
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

        return "Μέσος όρος διάρκειας: " + df.format(meanDuration) + " ώρες\n" +
                "Μέσος όρος έντασης: " + df.format(meanIntensity) + "\n" +
                "Αριστερά: " + df.format(left / events * 100L) + "%\n" +
                "Μπροστά: " + df.format(front / events * 100L) + "%\n" +
                "Δεξιά: " + df.format(right / events * 100L) + "%\n" +
                "Συνδυασμός: " + df.format(combination / events * 100L) + "%\n" +
                "Αύρα: " + df.format(aura / events * 100L) + "%\n" +
                "Φωτοφοβία: " + df.format(photo / events * 100L) + "%\n" +
                "Ηχοφοβία: " + df.format(sound / events * 100L) + "%\n" +
                "Ίλιγγος: " + df.format(vertigo / events * 100L) + "%\n" +
                "Ναυτία/έμετος: " + df.format(nausea / events * 100L) + "%\n" +
                "Αυχένας: " + df.format(neck / events * 100L) + "%\n" +
                "Κακός ύπνος: " + df.format(sleep / events * 100L) + "%\n" +
                "Άγχος/στρες: " + df.format(stress / events * 100L) + "%\n" +
                "Κόπωση: " + df.format(fatigue / events * 100L) + "%\n";
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

    private boolean update() {
        try {
            newEventDuration = Integer.parseInt(String.valueOf(spinner.getValue()));
            newEventIntensity = Integer.parseInt(String.valueOf(Math.round(intensitySlider.getValue())));
            if (selDate == null)
                selDate = LocalDate.now();
            migev = new MigraineEvent(selDate, newEventDuration, newEventIntensity);
            savedInfo.setText(migev.toFormattedString());
            return true;
        } catch (Exception exception) {
            savedInfo.setText("Δεν καταγράφηκε κάποιο γεγονός");
            return false;
        }
    }

    private void doIO() throws IOException {
        if (update()) {
            File f = new File("migraineCalendar.txt");
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
                    bw.write("\nΣυμβάν: " + migev.toString());
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
                    if (!notes.getText().isEmpty())
                        bw.write("\n-- " + notes.getText());
                } catch (IOException e) { e.printStackTrace(); }
            read();
        }
        else
            System.err.println("Update unsuccessful, no IO done.");
    }

    private void setPrefs(Stage stage) {
        final String locX = "locationX";
        prefs.put(locX, String.valueOf(stage.getX()));
        final String locY = "locationY";
        prefs.put(locY, String.valueOf(stage.getY()));
        final String stWidth = "width";
        String currWidth = String.valueOf(stage.getWidth());
        prefs.put(stWidth, currWidth);
        final String stHeight = "height";
        String currHeight = String.valueOf(stage.getHeight());
        prefs.put(stHeight, currHeight);
    }

    private void getPrefs(Stage stage) {
        final double savedX = Double.parseDouble(prefs.get("locationX", "128.0"));
        final double savedY = Double.parseDouble(prefs.get("locationY", "64.0"));
        stage.setX(savedX);
        stage.setY(savedY);
        final double savedWidth = Double.parseDouble(prefs.get("width", String.valueOf(defWidth)));
        final double savedHeight = Double.parseDouble(prefs.get("height", String.valueOf(defHeight)));
        stage.setWidth(savedWidth);
        stage.setHeight(savedHeight);
    }

    public static void main(String[] args) { launch(args); }
}
