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

package ab;

import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.devices.PwmLed;

public class Main {
  public static void main(String[] args) throws InterruptedException {
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
