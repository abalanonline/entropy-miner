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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class Miner implements AutoCloseable, Runnable {

  public static final int DUTY_CYCLE_T = 32;
  private final RotaryEncoder knob;
  private final Pwm fan;
  private final Max7219 display;
  private final Pwm vuPwm;
  private final Audio audio;
  public boolean open;
  private int speed;
  private int brightness;
  private int offLc;
  private int offRc;
  private int offTc;
  private final EntRandom ent;

  public Miner(RotaryEncoder knob, Pwm fan, Max7219 display, Pwm vu, Audio audio) {
    this.knob = knob;
    this.fan = fan;
    this.display = display;
    this.vuPwm = vu;
    this.audio = audio;
    this.ent = new EntRandom();
  }

  synchronized protected void update() {
    if (!open) return;
    int speed = Math.min(Math.max(0, this.speed), DUTY_CYCLE_T);
    int brightness = Math.min(Math.max(0, this.brightness), 15);
    fan.setDutyCycle(speed, DUTY_CYCLE_T);
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < DUTY_CYCLE_T; i++) s.append(i < speed ? '1' : '0');
    display.print(0, 7, s.toString(), 0);
    display.setBrightness(brightness);
    display.update();
    this.speed = speed;
    this.brightness = brightness;
  }

  protected void keyListener(String s) {
    switch (s) {
      case "Left": speed--; break;
      case "Right": speed++; break;
      case "-": brightness--; offLc++; break;
      case "+": brightness++; offRc++; break;
      case "1": offLc = 0; offRc = 0; break;
      case "0":
        if (offRc > 0) offTc = 0;
        if (offLc > 0 && offRc == 0) offTc++;
        if (offTc >= 1) close(); // FIXME 3
        break;
    }
  }

  public static final double AMMETER_SCALE_CORRECTION = 30 / 23.0; // tested, it never reach 23 mA, no clipping error
  @Override
  public void run() {
    final int dutyCycleT = 64;
    int[] histogram = new int[32];
    Queue<byte[]> queue = new ArrayDeque<>();
    while (open) {
      try {
        do {
          queue.add(audio.queue.take()); // take at least one
        } while (!audio.queue.isEmpty());
      } catch (InterruptedException e) {
        break;
      }
      int byteSize = queue.element().length;
      int size = queue.size();
      byte[] audioBytes = new byte[byteSize * size];
      for (int i = 0; i < size; i++) System.arraycopy(queue.remove(), 0, audioBytes, i * byteSize, byteSize);
      Arrays.fill(histogram, 0);
      for (int i = audioBytes.length - 1; i >= 0; i--) histogram[audioBytes[i] >> 3 & 0x1F]++;
      int[] mArray = Arrays.copyOf(histogram, 32);
      Arrays.sort(mArray);
      int median = Math.max(1, mArray[16]);
      double ent = this.ent.apply(audioBytes);
      vuPwm.setDutyCycle((int) (ent * dutyCycleT * AMMETER_SCALE_CORRECTION), dutyCycleT);
      for (int x = 0; x < 32; x++) {
        int v = (histogram[x] + median - 1) / median;
        for (int y = 0; y < 7; y++) display.print(x, y, y < v ? "1" : "0", 0);
      }
      update();
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
  synchronized public void close() {
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
