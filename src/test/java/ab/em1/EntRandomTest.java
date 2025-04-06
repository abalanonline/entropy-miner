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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class EntRandomTest {

  @Test
  void test() {
    EntRandom ent = new EntRandom();
    assertEquals(0, ent.apply(new byte[0]));
    byte[] test = new byte[256];
    assertEquals(0, ent.apply(test));
    for (int i = 0; i < 256; i++) test[i] = (byte) i;
    assertEquals(1, ent.apply(test));
    for (int i = 0; i < 256; i++) test[i] = (byte) (i & 0x0F);
    assertEquals(0.5, ent.apply(test));
    for (int i = 0; i < 256; i++) test[i] = (byte) (i & 0xC0);
    assertEquals(0.25, ent.apply(test));
  }

  @Disabled
  @Test
  void testInternal() {
    EntRandom ent = new EntRandom();
    byte[] test = new byte[256];
    Random random = ThreadLocalRandom.current();
    for (int n = 0; n < 1000; n++) {
      for (int i = 0; i < 256; i++) test[i] = (byte) random.nextInt();
      double exec = ent.exec(test);
      double internal = ent.internal(test);
      assertTrue(Math.abs(exec - internal) < 0.0000001);
    }
  }

}
