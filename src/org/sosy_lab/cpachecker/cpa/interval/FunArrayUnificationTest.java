// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cpa.interval.FunArrayBuilder.exp;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.interval.FunArrayBuilder.FunArrayBuilderException;

public class FunArrayUnificationTest {

  private void testUnification(
      FunArray initialA,
      FunArray initialB,
      FunArray expectedResultA,
      FunArray expectedResultB
  ) {
    var unification = new FunArrayUnification(initialA, initialB);
    var result = unification.unify(Interval.EMPTY, Interval.EMPTY);

    FunArray resultA = result.resultA();
    FunArray resultB = result.resultB();

    assertThat(resultA).isEqualTo(expectedResultA);
    assertThat(resultB).isEqualTo(expectedResultB);
  }

  /*
   * Tests example 8 from the Cousout, Cousot and Logozzo Paper.
   */
  @Test
  public void testExample8FromPaper() throws FunArrayBuilderException {

    testUnification(
        // {0 i} ⊤ {n}
        FunArrayBuilder
            .firstBound(exp(0), exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // {0 i-1} 0 {1 i} ⊤ {n}?
        FunArrayBuilder
            .firstBound(exp(0), exp("i", -1))
            .value(0)
            .bound(exp(1), exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build(),
        // {0} ⊥ {i}? ⊤ {n}
        FunArrayBuilder
            .firstBound(exp(0))
            .value(Interval.EMPTY)
            .bound(exp("i"))
            .mayBeEmpty()
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // {0} 0 {i} ⊤ {n}?
        FunArrayBuilder
            .firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build()
    );
  }
}
