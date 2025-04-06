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
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.devices.PwmLed;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    Dz testVu = new Dz(1, 92).open();
    long nanoTime = System.nanoTime() + 10_000_000_000L;
    while (System.nanoTime() < nanoTime) {
      testVu.set(true);
      Thread.sleep(25);
      testVu.set(false);
      Thread.sleep(25);
    }
    testVu.close();
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
    System.exit(0);

    PwmLed pwmLed = new PwmLed(84);
    int i2cAddress = Integer.parseInt(args[0]);
    int i2cBus = Integer.parseInt(args[1]);
    I2CDeviceInterface device = I2CDevice.builder(i2cAddress).setController(i2cBus).build();
    for (int i = 0; i < 1000; i++) {
      device.writeByte((byte) 0);
      int v = device.readByte() & 0xFF;
      System.out.print(String.format("\r%8s", Integer.toBinaryString(v)));
      pwmLed.setValue(v / 255F);
      Thread.sleep(100);
    }
    //LcdSampleApp16x2PCF8574.main(args);
    pwmLed.pulse();
  }
}
