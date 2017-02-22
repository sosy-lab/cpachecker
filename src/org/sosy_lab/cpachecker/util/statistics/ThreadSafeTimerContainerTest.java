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

import org.junit.Test;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ThreadSafeTimerContainerTest {

  public ThreadSafeTimerContainerTest() {}

  @Test
  public void singleTimer() {
    ThreadSafeTimerContainer container = new ThreadSafeTimerContainer("");

    TimerWrapper timer = container.getNewTimer();
    for (int i = 0; i < 5; i++) {
      assert_().that(container.getNumberOfIntervals()).isEqualTo(i);
      timer.start();
      timer.stop();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(i+1);
    }
  }

  @Test
  public void multipleTimer() {
    ThreadSafeTimerContainer container = new ThreadSafeTimerContainer("");

    for (int i = 0; i < 5; i++) {
      TimerWrapper timer = container.getNewTimer();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(i);
      timer.start();
      timer.stop();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(i+1);
    }
  }

  @Test
  public void multipleTimerInterleaved() {
    ThreadSafeTimerContainer container = new ThreadSafeTimerContainer("");

    TimerWrapper timer = container.getNewTimer();
    for (int i = 0; i < 5; i++) {
      TimerWrapper timer2 = container.getNewTimer();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(2*i);
      timer.start();
      timer2.start();
      timer.stop();
      timer2.stop();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(2*i+2);
    }
  }

  @Test
  public void multipleTimerInterleaved2() {
    ThreadSafeTimerContainer container = new ThreadSafeTimerContainer("");

    TimerWrapper timer = container.getNewTimer();
    TimerWrapper timer1 = container.getNewTimer();
    for (int i = 0; i < 5; i++) {
      TimerWrapper timer2 = container.getNewTimer();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(3*i);
      timer.start();
      timer1.start();
      timer1.stop();
      timer2.start();
      timer2.stop();
      timer.stop();
      assert_().that(container.getNumberOfIntervals()).isEqualTo(3*i+3);
    }
  }

}
