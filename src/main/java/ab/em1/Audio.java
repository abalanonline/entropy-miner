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
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Audio implements AutoCloseable {

  public static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100, 8, 1, false, false);
  public static final int BLOCK_SIZE = 512 / 8; // MD5, SHA-1, SHA-256 block size = 512 bit
  public static final int QUEUE_MAX = (int) (AUDIO_FORMAT.getFrameRate() * AUDIO_FORMAT.getFrameSize() / BLOCK_SIZE); // 1 sec max
  public final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
  private TargetDataLine audioLine;
  private Thread thread;
  private boolean open;

  protected void run() {
    final int length = BLOCK_SIZE;
    byte[] audioBytes = new byte[length];
    while (open && audioLine.read(audioBytes, 0, length) == length) {
      while (queue.size() >= QUEUE_MAX) queue.poll();
      queue.add(Arrays.copyOf(audioBytes, length));
    }
  }

  public Audio open() {
    if (open) throw new IllegalStateException("not closed");
    open = true;
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
    Thread thread = this.thread;
    try {
      if (thread != null) thread.join();
    } catch (InterruptedException ignore) {
    }
    this.thread = null;
    TargetDataLine audioLine = this.audioLine;
    if (audioLine != null) audioLine.close();
    this.audioLine =  null;
  }

}
