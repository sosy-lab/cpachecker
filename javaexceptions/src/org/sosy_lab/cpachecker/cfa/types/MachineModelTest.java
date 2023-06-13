// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.truth.Truth.assertThat;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

@RunWith(Parameterized.class)
@SuppressFBWarnings(
    value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
    justification = "Fields are filled by parameterization of JUnit")
public class MachineModelTest {

  @Parameters(name = "{4}: {0}")
  public static Object[][] machineModels() {
    Object[][] types = {
      // type          // size in bits // min value // max value
      {CNumericTypes.BOOL, 8, 0L, 1L},
      {CNumericTypes.CHAR, 8, -128L, 127L},
      {CNumericTypes.SIGNED_CHAR, 8, -128L, 127L},
      {CNumericTypes.UNSIGNED_CHAR, 8, 0L, 255L},
      {CNumericTypes.SHORT_INT, 16, -32768L, 32767L},
      {CNumericTypes.INT, 32, -2147483648L, 2147483647L},
      {CNumericTypes.SIGNED_INT, 32, -2147483648L, 2147483647L},
      {CNumericTypes.UNSIGNED_INT, 32, 0L, 4294967295L},
    };

    // Create a copy of types for each MachineModel and append the MachineModel instance in each row
    MachineModel[] machineModels = MachineModel.values();
    Object[][] result = new Object[machineModels.length * types.length][];
    for (int m = 0; m < machineModels.length; m++) {
      int offset = m * types.length;
      for (int t = 0; t < types.length; t++) {
        result[offset + t] = Arrays.copyOf(types[t], types[t].length + 1);
        if (types[t][0] == CNumericTypes.CHAR && !machineModels[m].isDefaultCharSigned()) {
          result[offset + t][2] = 0;
          result[offset + t][3] = 255;
        }
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
    assertThat(machineModel.getSizeofInBits(type)).isEqualTo(sizeInBits);
  }

  @Test
  public void testMinimalValue() {
    assertThat(machineModel.getMinimalIntegerValue(type).longValue()).isEqualTo(minValue);
  }

  @Test
  public void testMaximalValue() {
    assertThat(machineModel.getMaximalIntegerValue(type).longValue()).isEqualTo(maxValue);
  }
}
