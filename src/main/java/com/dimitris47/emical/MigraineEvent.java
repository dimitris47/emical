package com.dimitris47.emical;

import java.time.LocalDate;

public class MigraineEvent {
    LocalDate date;
    int hours;
    int intensity;
    String units;
//    boolean isMajor;

    public MigraineEvent(LocalDate date, int hours, int intensity) {
        this.date = date;
        this.hours = hours;
        this.intensity = intensity;
        if (hours == 1) {
            units = " ώρα, ένταση: ";
        } else {
            units = " ώρες, ένταση: ";
        }
//        if (this.hours >= 6 || this.intensity >= 7)
//            this.isMajor = true;
    }

    @Override
    public String toString() {
        return this.date.toString() + ": " + this.hours + units + this.intensity;
    }

    public String toFormattedString() {
        return this.date.getDayOfMonth() + "-" + this.date.getMonthValue() + "-" + this.date.getYear() +
                ": " + Emical.df.format(this.hours) + units + Emical.df.format(this.intensity);
    }
}
