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

import java.time.LocalDate;

public class MigraineEvent {
    LocalDate date;
    int hours;
    int intensity;
    String units;

    public MigraineEvent(LocalDate date, int hours, int intensity) {
        this.date = date;
        this.hours = hours;
        this.intensity = intensity;
        if (hours == 1) {
            units = " ώρα, ένταση: ";
        } else {
            units = " ώρες, ένταση: ";
        }
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
