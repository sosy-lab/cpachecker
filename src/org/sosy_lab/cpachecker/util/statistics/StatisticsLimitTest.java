// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatisticsLimitTest {

  public StatisticsValue<Integer> valInt;
  public StatisticsLimit<Integer> limitInt1;
  public StatisticsLimit<Integer> limitInt2;
  public StatisticsValue<Double> valDouble;
  public StatisticsLimit<Double> limitDouble;

  @Before
  public void init() {
    valInt = new StatisticsValue<>("IterationCount", 0);
    limitInt1 = new StatisticsLimit<>("IterationCount1", 1);
    limitInt2 = new StatisticsLimit<>("IterationCount2", 2);
    valDouble = new StatisticsValue<>("IterationCount", 0.0);
    limitDouble = new StatisticsLimit<>("IterationCount", 1.00);
  }

  @After
  public void teardown() {
    valInt.unregister(limitInt1);
    valInt.unregister(limitInt2);
    valDouble.unregister(limitDouble);
  }

  @Test
  public void isExceededTest() {
    // TODO: Better to access private limit variable through magic and not declare new statval
    // Cannot use setValue() because it would trigger exception
    StatisticsValue<Integer> statVal = new StatisticsValue<>("IterationCount", 2);
    assertThat(limitInt1.isExceeded(statVal)).isTrue();
    statVal = new StatisticsValue<>("IterationCount", 1);
    assertThat(limitInt1.isExceeded(statVal)).isTrue();
    statVal = new StatisticsValue<>("IterationCount", 0);
    assertThat(limitInt1.isExceeded(statVal)).isFalse();
  }

  @Test
  public void updatedIntTest() {
    valInt.register(limitInt1);
    assertThrows(
        InterruptedException.class,
        () -> {
          valInt.setValue(2);
        });
  }

  @Test
  public void updatedDoubleTest() {
    valDouble.register(limitDouble);
    assertThrows(
        InterruptedException.class,
        () -> {
          valDouble.setValue(3.00);
        });
  }

  @Test
  public void multiLimitsTest() throws InterruptedException {
    valInt.register(limitInt2);
    valInt.setValue(1);
    valInt.register(limitInt1);
    assertThrows(
        InterruptedException.class,
        () -> {
          valInt.setValue(1);
        });
  }
}
