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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.io.OutputStream;

public class Audio implements AutoCloseable {

  public static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100, 16, 1, true, false); // audio cd mono
  public static final int BLOCK_SIZE = 4; // MD5, SHA-1, SHA-256 block size = 512 bit
  // BLOCK 1=1378Hz, 4=344, 16=86, 64=22Hz=46ms
  private byte[] audioBytes = new byte[BLOCK_SIZE * (512 / 8)];
  private TargetDataLine audioLine;
  private Thread thread;
  private final Runtime runtime = Runtime.getRuntime();
  private boolean open;

  public double ent() {
    int length = audioBytes.length / 2;
    byte[] bytes = new byte[length];
    for (int i = 0, j = 0; i < length; i++, j++) bytes[i] = audioBytes[++j];
    try {
      Process process = runtime.exec("ent -t");
      OutputStream output = process.getOutputStream();
      output.write(bytes);
      output.close();
      if (process.waitFor() != 0) throw new IllegalStateException(new String(process.getErrorStream().readAllBytes()));
      String s = new String(process.getInputStream().readAllBytes());
      String[] v = s.split("\n")[1].split(",");
      int fileBytes = Integer.parseInt(v[1]);
      if (fileBytes != bytes.length) throw new IllegalStateException("ent File-bytes = " + fileBytes);
      double entropy = Double.parseDouble(v[2]);
      double chiSquare = Double.parseDouble(v[3]);
      double mean = Double.parseDouble(v[4]);
      double monteCarloPi = Double.parseDouble(v[5]);
      double serialCorrelation = Double.parseDouble(v[6]);
      return entropy / 8; // ent output is 0.000-7.999
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  protected void runEnt() {
  }

  protected void run() {
    final int length = audioBytes.length;
    while (open && audioLine.read(audioBytes, 0, length) == length) {
    }
  }

  public Audio open() {
    if (open) throw new IllegalStateException("not closed");
    open = true;
    ent(); // test run
    try {
      audioLine = AudioSystem.getTargetDataLine(AUDIO_FORMAT);
      audioLine.open(AUDIO_FORMAT);
    } catch (LineUnavailableException e) {
      throw new IllegalStateException(e);
    }
    audioLine.start();
    thread = new Thread(this::run);
    thread.start();
    return this;
  }

  @Override
  public void close() {
    open = false;
    try {
      thread.join();
    } catch (InterruptedException ignore) {
    }
    TargetDataLine audioLine = this.audioLine;
    if (audioLine != null) audioLine.close();
    this.audioLine =  null;
  }

}
