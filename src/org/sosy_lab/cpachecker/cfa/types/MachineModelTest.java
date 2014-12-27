/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.types;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

@RunWith(Parameterized.class)
public class MachineModelTest {

  @Parameters(name="{4}: {0}")
  public static Object[][] machineModels() {
    Object[][] types = new Object[][] {
        // type          // size in bits // min value // max value
        {CNumericTypes.BOOL,          8,           0L,          1L},
        {CNumericTypes.CHAR,          8,        -128L,        127L},
        {CNumericTypes.SIGNED_CHAR,   8,        -128L,        127L},
        {CNumericTypes.UNSIGNED_CHAR, 8,           0L,        255L},
        {CNumericTypes.SHORT_INT,    16,      -32768L,      32767L},
        {CNumericTypes.INT,          32, -2147483648L, 2147483647L},
        {CNumericTypes.SIGNED_INT,   32, -2147483648L, 2147483647L},
        {CNumericTypes.UNSIGNED_INT, 32,           0L, 4294967295L},
        };

    // Create a copy of types for each MachineModel and append the MachineModel instance in each row
    MachineModel[] machineModels = MachineModel.values();
    Object[][] result = new Object[machineModels.length * types.length][];
    for (int m = 0; m < machineModels.length; m++) {
      int offset = m * types.length;
      for (int t = 0; t < types.length; t++) {
        result[offset + t] = Arrays.copyOf(types[t], types[t].length + 1);
        result[offset + t][types[t].length] = machineModels[m];
      }
    }

    return result;
  }

  @Parameter(0)
  public CSimpleType type;

  @Parameter(1)
  public int sizeInBits;

  @Parameter(2)
  public long minValue;

  @Parameter(3)
  public long maxValue;

  @Parameter(4)
  public MachineModel machineModel;

  @Test
  public void testSizeOfInBits() {
    assertEquals(sizeInBits, machineModel.getSizeofInBits(type));
  }

  @Test
  public void testMinimalValue() {
    assertEquals(minValue, machineModel.getMinimalIntegerValue(type).longValue());
  }

  @Test
  public void testMaximalValue() {
    assertEquals(maxValue, machineModel.getMaximalIntegerValue(type).longValue());
  }
}