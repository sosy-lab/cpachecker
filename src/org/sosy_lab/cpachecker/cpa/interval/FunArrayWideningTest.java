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

public class FunArrayWideningTest {

  @Test
  public void testWideningForInitialization() throws FunArrayBuilderException {

    // {0} 0 {i} ⊤ {n}?
    FunArray leftSide =
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build();

    // {0} 0 {i-1} 0 {i} ⊤ {n}?
    FunArray rightSide =
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("j", -1))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build();

    FunArray result = leftSide.widen(rightSide);

    assertThat(result).isEqualTo(leftSide);
  }
}
