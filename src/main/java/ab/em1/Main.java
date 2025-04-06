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
  public static void main(String[] args) throws InterruptedException {
    GpioSystem.devicetreeCompatible().forEach(System.out::println);
    // power up the rotary encoder
    Dz rotaryPower = new Dz(1, 95).open();
    rotaryPower.set(true);
    rotaryPower.close();

    try (BusyRunner busyRunner = new BusyRunner().open(); // not sure who should open the runner
        Pwm fan = new Pwm(new Dz(1, 91), busyRunner);
        Pwm vu = new Pwm(new Dz(1, 92), busyRunner);
        Dz din = new Dz(1, 87);
        Dz cs = new Dz(1, 88);
        Dz clk = new Dz(1, 90);
        Max7219 display = new Max7219(din, cs, clk);
        Dz oa = new Dz(1, 83, true);
        Dz ob = new Dz(1, 82, true);
        Dz sw = new Dz(1, 81, true);
        RotaryEncoder knob = new RotaryEncoder(oa, ob, sw, busyRunner);
        Audio audio = new Audio();
        Miner miner = new Miner(knob, fan, display, vu, audio).open();
    ) {
      miner.run();
    }

  }
}
