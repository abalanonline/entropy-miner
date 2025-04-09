/*
 * Copyright (C) 2025 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.em1;

import ab.gpio.GpioSystem;
import ab.gpio.Max7219;
import ab.gpio.Pwm;
import ab.gpio.RotaryEncoder;
import ab.gpio.driver.BusyRunner;
import ab.gpio.driver.Dz;

public class Main {
  public static void main(String[] args) {
    int[] cnf = {1, 91, 1, 92, 1, 87, 1, 88, 1, 90, 1, 81, 1, 82, 1, 83};
    Dz fanPin = new Dz(cnf[0], cnf[1]);
    Dz vuPin = new Dz(cnf[2], cnf[3]);
    Dz oa = new Dz(cnf[4], cnf[5], true);
    Dz ob = new Dz(cnf[6], cnf[7], true);
    Dz sw = new Dz(cnf[8], cnf[9], true);
    Dz clk = new Dz(cnf[10], cnf[11]);
    Dz cs = new Dz(cnf[12], cnf[13]);
    Dz din = new Dz(cnf[14], cnf[15]);
    // the remaining pins are for power source, enable them
    for (int i = 16; i < cnf.length; i += 2) {
      Dz power = new Dz(cnf[i], cnf[i + 1]).open();
      power.set(true);
      power.close();
    }

    GpioSystem.devicetreeCompatible().forEach(System.out::println);

    try (BusyRunner busyRunner = new BusyRunner().open(); // not sure who should open the runner
        Pwm fan = new Pwm(fanPin, busyRunner);
        Pwm vu = new Pwm(vuPin, busyRunner);
        Max7219 display = new Max7219(din, cs, clk);
        RotaryEncoder knob = new RotaryEncoder(oa, ob, sw, busyRunner);
        Audio audio = new Audio();
        Miner miner = new Miner(knob, fan, display, vu, audio).open();
    ) {
      miner.run();
    }

  }
}
