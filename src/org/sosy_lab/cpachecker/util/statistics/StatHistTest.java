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

public class StatHistTest {

  private StatHist sh;

  @Before
  public void init() {
    sh = new StatHist("foo");
  }

  @Test
  public void testMinMax0() {
    assert_().that(sh.getMax()).isEqualTo(Integer.MIN_VALUE);
    assert_().that(sh.getMin()).isEqualTo(Integer.MAX_VALUE);
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
  public void testAvg1() {
    sh.insertValue(1);
    assert_().that(sh.getAvg()).isEqualTo(1.0);
    sh.insertValue(1);
    assert_().that(sh.getAvg()).isEqualTo(1.0);
    sh.insertValue(4);
    assert_().that(sh.getAvg()).isEqualTo(2.0);
    sh.insertValue(4);
    assert_().that(sh.getAvg()).isEqualTo(2.5);
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
  }
}
