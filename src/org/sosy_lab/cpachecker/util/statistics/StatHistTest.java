// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import static com.google.common.truth.Truth.assert_;

import org.junit.Before;
import org.junit.Test;

public class StatHistTest {

  private StatHist sh;

  @Before
  public void init() {
    sh = new StatHist("foo");
  }

  @Test
  public void testMinMax0() {
    assert_().that(sh.getMax()).isEqualTo(Long.MIN_VALUE);
    assert_().that(sh.getMin()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  public void testMinMax1() {
    sh.insertValue(1);
    sh.insertValue(3);
    sh.insertValue(1);
    sh.insertValue(-10);
    assert_().that(sh.getMax()).isEqualTo(3);
    assert_().that(sh.getMin()).isEqualTo(-10);
  }

  @Test
  public void testAvg0() {
    assert_().that(sh.getAvg()).isEqualTo(Double.NaN);
  }

  @Test
  public void testDev0() {
    assert_().that(sh.getStdDeviation()).isEqualTo(Double.NaN);
  }

  @Test
  public void testMean0() {
    assert_().that(sh.getMean()).isEqualTo(0);
  }

  @Test
  public void testAvg1() {
    sh.insertValue(1);
    assert_().that(sh.getAvg()).isEqualTo(1.0);
    assert_().that(sh.getMean()).isEqualTo(1);
    sh.insertValue(1);
    assert_().that(sh.getAvg()).isEqualTo(1.0);
    assert_().that(sh.getMean()).isEqualTo(1);
    sh.insertValue(4);
    assert_().that(sh.getAvg()).isEqualTo(2.0);
    assert_().that(sh.getMean()).isEqualTo(1);
    sh.insertValue(4);
    assert_().that(sh.getAvg()).isEqualTo(2.5);
    assert_().that(sh.getMean()).isEqualTo(1);
  }

  @Test
  public void testAvg2() {
    for (int x : new int[] {2, 4, 4, 4, 5, 5, 7, 9}) {
      sh.insertValue(x);
    }
    assert_().that(sh.getAvg()).isEqualTo(5.0);
    assert_().that(sh.getStdDeviation()).isEqualTo(2.0);
    assert_().that(sh.getMin()).isEqualTo(2);
    assert_().that(sh.getMax()).isEqualTo(9);
    assert_().that(sh.getMean()).isEqualTo(4);
  }
}
