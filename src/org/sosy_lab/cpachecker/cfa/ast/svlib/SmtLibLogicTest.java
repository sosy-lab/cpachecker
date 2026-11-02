// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SmtLibLogicTest {

  private static void assertTheories(String pLogic, boolean pIntegers, boolean pBitvectors) {
    SmtLibLogic logic = SmtLibLogic.fromString(pLogic);
    assertThat(logic.toString()).isEqualTo(pLogic);
    assertThat(logic.containsIntegerArithmetic()).isEqualTo(pIntegers);
    assertThat(logic.containsBitvectorOperations()).isEqualTo(pBitvectors);
  }

  @Test
  public void logicsOfTheTranslatedTasks() {
    assertTheories("ALL", true, true);
    assertTheories("LIA", true, false);
    assertTheories("QF_LIA", true, false);
    assertTheories("QF_NIA", true, false);
    assertTheories("QF_ALIA", true, false);
    assertTheories("QF_ANIA", true, false);
    assertTheories("QF_UFLIA", true, false);
    assertTheories("QF_BV", false, true);
    assertTheories("QF_ABV", false, true);
    assertTheories("QF_BVFP", false, true);
    assertTheories("QF_ABVFP", false, true);
    assertTheories("QF_UFBVLIA", true, true);
    assertTheories("QF_AUFBVLIA", true, true);
    assertTheories("UFBVFPDTNIRA", true, true);
    assertTheories("AUFBVFPDTNIRA", true, true);
  }

  @Test
  public void otherLogics() {
    assertTheories("QF_AX", false, false);
    assertTheories("QF_IDL", true, false);
    assertTheories("QF_LRA", false, false);
    assertTheories("QF_SLIA", true, false);
  }

  @Test
  public void unknownLogics() {
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("QF_"));
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("QF_XYZ"));
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("QF_LIAX"));
    assertThrows(IllegalArgumentException.class, () -> SmtLibLogic.fromString("all"));
  }
}
