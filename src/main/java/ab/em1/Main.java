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
import ab.gpio.Pin;
import ab.gpio.Pwm;
import ab.gpio.RotaryEncoder;
import ab.gpio.driver.BusyRunner;

import java.util.Arrays;

public class Main {

  public static final int[][] DEVICE_PINS = new int[][]{
      {1, 91, 1, 92, 1, 87, 1, 88, 1, 90, 1, 81, 1, 82, 1, 83}, // libretech,aml-s905x-cc, amlogic,s905x, amlogic,meson-gxl
      {0, 14, 0, 15, 0, 10, 0, 9, 0, 11, 0, 16, 0, 20, 0, 21}, // raspberrypi,4-model-b, brcm,bcm2711
      {0, 48, 0, 49, 0, 16, 0, 17, 0, 18, 0, 51, 0, 77, 0, 78}}; // meh
  // nvidia,p3542-0000+p3448-0003, nvidia,jetson-nano-2gb, nvidia,jetson-nano, nvidia,tegra210
  // TODO: 2025-04-09 let Nvidia hardware fail on GPIO_GET_LINEEVENT_IOCTL and not try to fix it
  public static final String[] DEVICE_NAMES = new String[]{
      "libretech,aml-s905x-cc", "raspberrypi,4-model-b", "nvidia,jetson-nano"};

  public static void main(String[] args) {
    int[] conf = GpioSystem.getByDevice(Arrays.asList(DEVICE_NAMES), Arrays.asList(DEVICE_PINS), null);

    Pin fanPin = new Pin(conf[0], conf[1]);
    Pin vuPin = new Pin(conf[2], conf[3]);
    Pin oa = new Pin(conf[4], conf[5], true);
    Pin ob = new Pin(conf[6], conf[7], true);
    Pin sw = new Pin(conf[8], conf[9], true);
    Pin clk = new Pin(conf[10], conf[11]);
    Pin cs = new Pin(conf[12], conf[13]);
    Pin din = new Pin(conf[14], conf[15]);
    // the remaining pins are for power source, enable them
    for (int i = 16; i < conf.length; i += 2) {
      Pin power = new Pin(conf[i], conf[i + 1]).open();
      power.set(true);
      power.close();
    }

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
