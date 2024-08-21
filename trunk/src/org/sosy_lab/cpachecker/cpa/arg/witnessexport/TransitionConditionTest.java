// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;

public class TransitionConditionTest {

  @Test
  public void test_compareTo() {
    TransitionCondition empty = TransitionCondition.empty();
    TransitionCondition tc1 = TransitionCondition.empty().putAndCopy(KeyDef.STARTLINE, "1");
    TransitionCondition tc2 = TransitionCondition.empty().putAndCopy(KeyDef.STARTLINE, "2");
    TransitionCondition tc3 = TransitionCondition.empty().putAndCopy(KeyDef.ENDLINE, "1");

    assertThat(tc1.compareTo(empty)).isGreaterThan(0);
    assertThat(empty.compareTo(tc1)).isLessThan(0);

    assertThat(tc1.compareTo(tc2)).isLessThan(0);
    assertThat(tc2.compareTo(tc1)).isGreaterThan(0);

    assertThat(tc1.compareTo(tc3)).isLessThan(0);
    assertThat(tc3.compareTo(tc1)).isGreaterThan(0);

    assertThat(tc2.compareTo(tc3)).isLessThan(0);
    assertThat(tc3.compareTo(tc2)).isGreaterThan(0);
  }
}
