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
  @Test
  public void testExample8FromPaper() throws FunArrayBuilderException {

    FunArray arrayA = FunArrayBuilder
        .firstBound(exp(0), exp("i"))
        .value(Interval.UNBOUND)
        .bound(exp("n"))
        .build();

    FunArray arrayB = FunArrayBuilder
        .firstBound(exp(0), exp("i", -1))
        .value(0)
        .bound(exp(1), exp("i"))
        .value(Interval.UNBOUND)
        .bound(exp("n"))
        .mayBeEmpty()
        .build();

    var unification = new FunArrayUnification(arrayA, arrayB);
    var result = unification.unify(Interval.EMPTY, Interval.EMPTY);

    FunArray resultA = result.resultA();
    FunArray resultB = result.resultB();

    FunArray expectedResultA = FunArrayBuilder
        .firstBound(exp(0))
        .value(Interval.EMPTY)
        .bound(exp("i"))
        .mayBeEmpty()
        .value(Interval.UNBOUND)
        .bound(exp("n"))
        .build();

    FunArray expectedResultB = FunArrayBuilder
        .firstBound(exp(0))
        .value(0)
        .bound(exp("i"))
        .value(Interval.UNBOUND)
        .bound(exp("n"))
        .mayBeEmpty()
        .build();

    assertThat(resultA).isEqualTo(expectedResultA);
    assertThat(resultB).isEqualTo(expectedResultB);
  }
}
