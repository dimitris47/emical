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

public class Box {

    public double value;

    public Box() {
        this.value = 0;
    }

    public void addOne() {
        this.value++;
    }
}
