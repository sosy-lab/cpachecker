// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.exp;
import static org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.variable;

import java.util.Set;
import org.junit.Test;

public class BoundTest {

  @Test
  public void testInvertibleAdaption() {
    Bound bound = new Bound(exp("i"));
    Bound updatedBound = bound.adaptForChangedVariableValues(variable("i"), Set.of(exp("i", -1)));
    assertThat(updatedBound.expressions()).contains(exp("i", 1));
  }

  @Test
  public void testNonInvertibleAdaption() {
    Bound bound = new Bound(Set.of(exp("i"), exp("j")));
    Bound updatedBound = bound.adaptForChangedVariableValues(variable("i"), Set.of(exp("k")));
    assertThat(updatedBound.expressions()).doesNotContain(exp("i"));
  }

  @Test
  public void testIntroduceEqualVariableAdaption() {
    Bound bound = new Bound(Set.of(exp(0)));
    Bound updatedBound = bound.adaptForChangedVariableValues(variable("i"), Set.of(exp(0)));
    assertThat(updatedBound.expressions()).contains(exp("i"));
    assertThat(updatedBound.expressions()).contains(exp(0));
  }

  @Test
  public void testRelateVariablesAdaption() {
    Bound bound = new Bound(Set.of(exp("i", 1), exp("j", 1)));
    Bound updatedBound = bound.adaptForChangedVariableValues(variable("i"), Set.of(exp("j")));
    assertThat(updatedBound.expressions()).contains(exp("i", 1));
    assertThat(updatedBound.expressions()).contains(exp("j", 1));
  }
}
