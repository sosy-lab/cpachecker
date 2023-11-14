// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPATest0;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/*
 * Tests SMGState readValue and writeValue. Note: this expects little endian encoding!
 */
public class ValueReadWriteTest extends SMGCPATest0 {

  @SuppressWarnings("unused")
  private static final int LONG_SIZE_IN_BITS = 64;

  private static final int INT_SIZE_IN_BITS = 32;

  private static final int SHORT_SIZE_IN_BITS = 16;

  private static final int CHAR_SIZE_IN_BITS = 8;
  // Int is 32 Bytes, values = 0, 1, 2, 4,
  private static Value[] INT_ARRAY_VALUES = new Value[64];

  @BeforeClass
  public static void prepareArrays() {
    for (int i = 0; i < 64; i = i + 2) {
      int v1 = (int) Math.pow(2, i / 2.0) - 1;
      INT_ARRAY_VALUES[i + 1] = new NumericValue(BigInteger.valueOf(v1));
      int v2 = (int) Math.pow(2, i / 2.0);
      INT_ARRAY_VALUES[i] = new NumericValue(BigInteger.valueOf(v2));
    }
  }

  @Test
  public void testCleanArrayRead() throws SMGSolverException, SMGException {
    SMGObject array = buildFilledArray(INT_ARRAY_VALUES.length, INT_ARRAY_VALUES, INT_SIZE_IN_BITS);
    for (int i = 0; i < INT_ARRAY_VALUES.length; i++) {
      List<ValueAndSMGState> readAndState =
          currentState.readValue(
              array,
              BigInteger.valueOf((long) i * (long) INT_SIZE_IN_BITS),
              BigInteger.valueOf(INT_SIZE_IN_BITS),
              null,
              true);
      assertThat(readAndState).hasSize(1);
      currentState = readAndState.get(0).getState();
      Value readValue = readAndState.get(0).getValue();
      assertThat(readValue.asNumericValue().bigIntegerValue())
          .isEqualTo(INT_ARRAY_VALUES[i].asNumericValue().bigIntegerValue());
    }
  }

  // Read only short size from the array (but always just parts from 1 int value)
  @Test
  public void testPartialIntArrayReadAsShort() throws SMGSolverException, SMGException {
    SMGObject array = buildFilledArray(INT_ARRAY_VALUES.length, INT_ARRAY_VALUES, INT_SIZE_IN_BITS);
    for (int i = 0; i < INT_ARRAY_VALUES.length; i++) {
      int expectedNonCutIntValue =
          INT_ARRAY_VALUES[i].asNumericValue().bigIntegerValue().intValueExact();

      readValueAndCheckResult(
          array,
          i * INT_SIZE_IN_BITS + SHORT_SIZE_IN_BITS,
          SHORT_SIZE_IN_BITS,
          1,
          expectedNonCutIntValue >> 16);

      readValueAndCheckResult(
          array, i * INT_SIZE_IN_BITS, SHORT_SIZE_IN_BITS, 1, expectedNonCutIntValue & 0x0000FFFF);
    }
  }

  @Test
  public void testPartialIntArrayReadAsChar() throws SMGSolverException, SMGException {
    SMGObject array = buildFilledArray(INT_ARRAY_VALUES.length, INT_ARRAY_VALUES, INT_SIZE_IN_BITS);
    for (int j = 0; j < 2; j++) {
      for (int i = 0; i < INT_ARRAY_VALUES.length; i++) {
        int expectedNonCutIntValue =
            INT_ARRAY_VALUES[i].asNumericValue().bigIntegerValue().intValueExact();

        readValueAndCheckResult(
            array,
            i * INT_SIZE_IN_BITS + 3 * CHAR_SIZE_IN_BITS,
            CHAR_SIZE_IN_BITS,
            1,
            expectedNonCutIntValue >> 24);

        readValueAndCheckResult(
            array,
            i * INT_SIZE_IN_BITS + 2 * CHAR_SIZE_IN_BITS,
            CHAR_SIZE_IN_BITS,
            1,
            expectedNonCutIntValue >>> 16 & 0x000000FF);

        readValueAndCheckResult(
            array,
            i * INT_SIZE_IN_BITS + 1 * CHAR_SIZE_IN_BITS,
            CHAR_SIZE_IN_BITS,
            1,
            expectedNonCutIntValue >>> 8 & 0x000000FF);

        readValueAndCheckResult(
            array,
            i * INT_SIZE_IN_BITS + 0 * CHAR_SIZE_IN_BITS,
            CHAR_SIZE_IN_BITS,
            1,
            expectedNonCutIntValue & 0x000000FF);
      }
    }
  }

  private void readValueAndCheckResult(
      SMGObject objectToRead,
      int offset,
      int sizeInBits,
      int expectedNumOfEdges,
      int expectedReadValue)
      throws SMGException {
    List<ValueAndSMGState> readAndState =
        currentState.readValue(
            objectToRead, BigInteger.valueOf(offset), BigInteger.valueOf(sizeInBits), null, true);
    assertThat(readAndState).hasSize(expectedNumOfEdges);
    currentState = readAndState.get(0).getState();
    Value readValue1 = readAndState.get(0).getValue();
    assertThat(readValue1.isNumericValue()).isTrue();

    assertThat(readValue1.asNumericValue().bigIntegerValue().intValueExact())
        .isEqualTo(expectedReadValue);
  }
}
