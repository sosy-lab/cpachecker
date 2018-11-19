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
package org.sosy_lab.cpachecker.cpa.interval;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings({"unchecked", "rawtypes"})
public class IntervalAnalysisStateTest {

  @Test
  public void pseudoPartiotionKey() {
    IntervalAnalysisState s = new IntervalAnalysisState();
    IntervalAnalysisState sa1 = s.addInterval("a", new Interval(1L, 1L), 10);
    IntervalAnalysisState sb2 = s.addInterval("b", new Interval(2L, 2L), 10);
    IntervalAnalysisState sa1b2 = sa1.addInterval("b", new Interval(2L, 2L), 10);
    IntervalAnalysisState sa1b3 = sa1.addInterval("b", new Interval(3L, 3L), 10);
    IntervalAnalysisState sa1b23 = sa1.addInterval("b", new Interval(2L, 3L), 10);

    Comparable cs = s.getPseudoPartitionKey();
    Comparable csa1 = sa1.getPseudoPartitionKey();
    Comparable csb2 = sb2.getPseudoPartitionKey();
    Comparable csa1b2 = sa1b2.getPseudoPartitionKey();
    Comparable csa1b3 = sa1b3.getPseudoPartitionKey();
    Comparable csa1b23 = sa1b23.getPseudoPartitionKey();

    checkEquals(cs, cs);
    checkEquals(csa1, csa1);
    checkEquals(csb2, csb2);
    checkEquals(csa1b2, csa1b2);
    checkEquals(csa1b3, csa1b3);
    checkEquals(csa1b23, csa1b23);

    checkEquals(csa1, csb2);
    checkEquals(csa1b2, csa1b3);

    checkLess(cs, csa1);
    checkLess(cs, csb2);
    checkLess(cs, csa1b2);
    checkLess(cs, csa1b3);
    checkLess(csa1, csa1b2);
    checkLess(csa1, csa1b3);
    checkLess(csb2, csa1b2);

    checkLess(csa1, csa1b23);
    checkLess(csb2, csa1b23);
    checkLess(csa1b23, csa1b2);
    checkLess(csa1b23, csa1b3);
  }

  private void checkLess(Comparable c1, Comparable c2) {
    assertTrue(c1.compareTo(c2) < 0);
    assertTrue(c2.compareTo(c1) > 0);
  }

  private void checkEquals(Comparable c1, Comparable c2) {
    assertTrue(c1.compareTo(c2) == 0);
    assertTrue(c2.compareTo(c1) == 0);
  }
}
