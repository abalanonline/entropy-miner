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

import ab.gpio.Max7219;
import ab.gpio.Pwm;
import ab.gpio.RotaryEncoder;

public class Miner implements AutoCloseable, Runnable {

  public static final int DUTY_CYCLE_T = 32;
  private final RotaryEncoder knob;
  private final Pwm fan;
  private final Max7219 display;
  private final Pwm vuPwm;
  private final Audio audio;
  public boolean open;
  private int speed;
  private int offLc;
  private int offRc;
  private int offTc;

  public Miner(RotaryEncoder knob, Pwm fan, Max7219 display, Pwm vu, Audio audio) {
    this.knob = knob;
    this.fan = fan;
    this.display = display;
    this.vuPwm = vu;
    this.audio = audio;
  }

  protected void update() {
    int speed = Math.min(Math.max(0, this.speed), DUTY_CYCLE_T);
    int brightness;
    fan.setDutyCycle(speed, DUTY_CYCLE_T);
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < DUTY_CYCLE_T; i++) s.append(i < speed ? '1' : '0');
    display.print(0, 7, s.toString(), 0);
//    display.setBrightness(brightness);
    display.update();
    this.speed = speed;
  }

  protected void keyListener(String s) {
    switch (s) {
      case "Left": speed--; update(); break;
      case "Right": speed++; update(); break;
      case "-": offLc++; break;
      case "+": offRc++; break;
      case "1": offLc = 0; offRc = 0; break;
      case "0":
        if (offRc > 0) offTc = 0;
        if (offLc > 0 && offRc == 0) offTc++;
        if (offTc >= 3) close();
        break;
    }
  }

  public static final double AMMETER_SCALE_CORRECTION = 30 / 23.0;
  @Override
  public void run() {
    final int dutyCycleT = 64;
    while (open) {
      vuPwm.setDutyCycle((int) (audio.ent() * dutyCycleT * AMMETER_SCALE_CORRECTION), dutyCycleT);
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public Miner open() {
    if (open) throw new IllegalStateException("not closed");
    open = true;
    audio.open();
    knob.open();
    fan.open();
    display.open();
    vuPwm.open();
    update();
    knob.setKeyListener(this::keyListener);
    return this;
  }

  @Override
  public void close() {
    knob.setKeyListener(null);
    knob.close();
    fan.setDutyCycle(0, 1);
    fan.close();
    display.close();
    vuPwm.setDutyCycle(0, 1);
    vuPwm.close();
    audio.close();
    open = false;
  }

}
