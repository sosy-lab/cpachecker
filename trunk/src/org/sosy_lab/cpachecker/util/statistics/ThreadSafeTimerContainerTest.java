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
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ThreadSafeTimerContainerTest {

  private ThreadSafeTimerContainer container;

  public ThreadSafeTimerContainerTest() {}

  @SuppressWarnings("deprecation")
  @Before
  public void init() {
    container = new ThreadSafeTimerContainer("");
  }

  @Test
  public void singleTimer() {
    TimerWrapper timer = container.getNewTimer();
    for (int i = 0; i < 5; i++) {
      checkIntervalNum(i);
      timer.start();
      checkIntervalNum(i + 1);
      timer.stop();
      checkIntervalNum(i + 1);
    }
  }

  @Test
  public void multipleTimer() {
    for (int i = 0; i < 5; i++) {
      TimerWrapper timer = container.getNewTimer();
      checkIntervalNum(i);
      timer.start();
      checkIntervalNum(i + 1);
      timer.stop();
      checkIntervalNum(i + 1);
    }
  }

  @Test
  public void multipleTimerInterleaved() {
    TimerWrapper timer = container.getNewTimer();
    for (int i = 0; i < 5; i++) {
      TimerWrapper timer2 = container.getNewTimer();
      checkIntervalNum(2 * i);
      timer.start();
      checkIntervalNum(2 * i + 1);
      timer2.start();
      checkIntervalNum(2 * i + 2);
      timer.stop();
      checkIntervalNum(2 * i + 2);
      timer2.stop();
      checkIntervalNum(2 * i + 2);
    }
  }

  @Test
  public void multipleTimerInterleaved2() {
    TimerWrapper timer = container.getNewTimer();
    TimerWrapper timer1 = container.getNewTimer();
    for (int i = 0; i < 5; i++) {
      TimerWrapper timer2 = container.getNewTimer();
      checkIntervalNum(3 * i);
      timer.start();
      checkIntervalNum(3 * i + 1);
      timer1.start();
      checkIntervalNum(3 * i + 2);
      timer1.stop();
      checkIntervalNum(3 * i + 2);
      timer2.start();
      checkIntervalNum(3 * i + 3);
      timer2.stop();
      checkIntervalNum(3 * i + 3);
      timer.stop();
      checkIntervalNum(3 * i + 3);
    }
  }

  private void checkIntervalNum(int num) {
    assert_()
        .withMessage("number of intervals does not match")
        .that(container.getNumberOfIntervals())
        .isEqualTo(num);
  }
}
