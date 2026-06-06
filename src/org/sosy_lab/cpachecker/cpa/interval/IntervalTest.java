// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class IntervalTest {

  @Test
  public void negateEmptyIntervalReturnsEmpty() {
    Interval result = Interval.EMPTY.negate();
    assertThat(result).isEqualTo(Interval.EMPTY);
  }
}
