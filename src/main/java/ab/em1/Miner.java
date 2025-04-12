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
import java.util.UUID;

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
  private boolean histogram;
  private int randomp;
  private final int[] randoms = new int[]{-1, -1, -1, 15,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      1, -1, 1, -1, 2, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1};

  public Miner(RotaryEncoder knob, Pwm fan, Max7219 display, Pwm vu, Audio audio) {
    this.knob = knob;
    this.fan = fan;
    this.display = display;
    this.vuPwm = vu;
    this.audio = audio;
    this.ent = new EntRandom();
    Arrays.fill(randoms, -1);
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
        if (offLc == 0 && offRc == 0) histogram = !histogram;
        if (offTc >= 3) close();
        break;
    }
  }

  protected void printHistogram(byte[] audioBytes) {
    int[] histogram = new int[32];
    for (int i = audioBytes.length - 1; i >= 0; i--) histogram[audioBytes[i] >> 3 & 0x1F]++;
    int[] mArray = Arrays.copyOf(histogram, 32);
    Arrays.sort(mArray);
    int median = Math.max(1, mArray[16]);
    for (int x = 0; x < 32; x++) {
      int v = (histogram[x] + median - 1) / median;
      for (int y = 0; y < 7; y++) display.print(x, y, y < v ? "1" : "0", 0);
    }
  }

  private static final byte[] HEX_FONT = new byte[]{
      0b11111, 0b10001, 0b11111, 0b00000, 0b11111, 0b00000, 0b11101, 0b10101, 0b10111, 0b10101, 0b10101, 0b11111,
      0b00111, 0b00100, 0b11111, 0b10111, 0b10101, 0b11101, 0b11111, 0b10101, 0b11101, 0b00001, 0b00001, 0b11111,
      0b11111, 0b10101, 0b11111, 0b10111, 0b10101, 0b11111, 0b11111, 0b00101, 0b11111, 0b11111, 0b10100, 0b11100,
      0b11111, 0b10001, 0b10001, 0b11100, 0b10100, 0b11111, 0b11111, 0b10101, 0b10101, 0b11111, 0b00101, 0b00101};
  protected void printRandom() {
    boolean[][] b = new boolean[7][32];
    int n = randoms.length;
    for (int x = -4, j = randomp, cv = 0, nv = 0; x < 32; x++, j++) {
      if (nv > 0) {
        randoms[j % n] = -1;
      } else {
        cv = randoms[j % n];
        if (cv >= 0) nv = 4;
      }
      if (nv > 0) nv--;
      if (nv > 0 && x >= 0) {
        byte c = HEX_FONT[cv * 3 + 3 - nv];
        for (int y = 0; y < 5; y++) {
          b[y + 1][x] = (c & 1) > 0;
          c >>>= 1;
        }
      }
    }
    StringBuilder s = new StringBuilder();
    for (int y = 0; y < 7; y++) {
      for (int x = 0; x < 32; x++) {
        s.append(b[y][x] ? '1' : '0');
      }
      display.print(0, y, s.toString(), 0);
      s.setLength(0);
    }
  }

  public static final double AMMETER_SCALE_CORRECTION = 30 / 23.0; // tested, it never reach 23 mA, no clipping error
  @Override
  public void run() {
    final int dutyCycleT = 64;
    final long nanoSleep = 100_000_000;
    Queue<byte[]> queue = new ArrayDeque<>();
    long nanoTime = System.nanoTime();
    while (open) {
      // prepare a block of bytes
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
      // display entropy
      for (int i = 0; i < size; i++) System.arraycopy(queue.remove(), 0, audioBytes, i * byteSize, byteSize);
      double ent = this.ent.apply(audioBytes);
      vuPwm.setDutyCycle((int) (ent * dutyCycleT * AMMETER_SCALE_CORRECTION), dutyCycleT);
      randoms[randomp] = ent > 0.5 ? (int) (UUID.nameUUIDFromBytes(audioBytes).getLeastSignificantBits()) & 0x0F : -1;
      //if (randoms[randomp] >= 0) System.out.println(randoms[randomp]);
      randomp = (randomp + 1) % randoms.length;
      // print histogram/number
      if (histogram) printHistogram(audioBytes); else printRandom();
      update();
      long t = System.nanoTime();
      if (nanoTime < t) nanoTime = t; // late
      if (nanoTime > t) {
        try {
          Thread.sleep((nanoTime - t) / 1_000_000);
        } catch (InterruptedException e) {
          break;
        }
      }
      nanoTime += nanoSleep;
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
