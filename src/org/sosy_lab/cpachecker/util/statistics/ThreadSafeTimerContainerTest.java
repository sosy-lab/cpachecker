/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.statistics;

import static com.google.common.truth.Truth.assert_;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ThreadSafeTimerContainerTest {

  private ThreadSafeTimerContainer container;

  public ThreadSafeTimerContainerTest() {}

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
        .withFailureMessage("number of intervals does not match")
        .that(container.getNumberOfIntervals())
        .isEqualTo(num);
  }

}
