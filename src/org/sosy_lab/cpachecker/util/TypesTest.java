/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.CHAR;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.DOUBLE;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.FLOAT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.LONG_LONG_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.SHORT_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.SIGNED_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.UNSIGNED_CHAR;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.UNSIGNED_INT;
import static org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes.UNSIGNED_SHORT_INT;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/**
 * Unit tests for {@link Types}.
 */
public class TypesTest {

  MachineModel machineModel = MachineModel.LINUX32;

  @Test
  public void shortHoldsChar() {
    assertThat(Types.canHoldAllValues(SHORT_INT, CHAR, machineModel)).isTrue();
  }

  @Test
  public void intHoldsShort() {
    assertThat(Types.canHoldAllValues(INT, SHORT_INT, machineModel)).isTrue();
  }

  @Test
  public void doubleHoldsInt() {
    assertThat(Types.canHoldAllValues(DOUBLE, INT, machineModel)).isTrue();
  }

  @Test
  public void doubleHoldsFloat() {
    assertThat(Types.canHoldAllValues(DOUBLE, FLOAT, machineModel)).isTrue();
  }

  @Test
  public void intHoldsFloat() {
    assertThat(Types.canHoldAllValues(INT, FLOAT, machineModel)).isFalse();
  }

  @Test
  public void charHoldsLongLong() {
    assertThat(Types.canHoldAllValues(CHAR, LONG_LONG_INT, machineModel)).isFalse();
  }

  @Test
  public void signedIntHoldsUnsignedShort() {
    assertThat(Types.canHoldAllValues(SIGNED_INT, UNSIGNED_SHORT_INT, machineModel)).isTrue();
  }

  @Test
  public void unsignedHoldsSignedInt() {
    assertThat(Types.canHoldAllValues(UNSIGNED_INT, SIGNED_INT, machineModel)).isFalse();
  }

  @Test
  public void unsignedCharHoldsInt() {
    assertThat(Types.canHoldAllValues(UNSIGNED_CHAR, INT, machineModel)).isFalse();
  }
}
