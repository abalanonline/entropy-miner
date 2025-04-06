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

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

/**
 * Random Sequence Tester program
 * https://github.com/Fourmilab/ent_random_sequence_tester/tree/master
 */
public class EntRandomTest implements Function<byte[], Double> {

  private final Runtime runtime = Runtime.getRuntime();

  public EntRandomTest() {
    exec(new byte[8]); // test run
  }

  protected double exec(byte[] bytes) {
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

  @Override
  public Double apply(byte[] bytes) {
    return exec(bytes);
  }

}
