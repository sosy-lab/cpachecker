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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cpa.sign.SIGN;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
@SuppressWarnings({ "unchecked", "rawtypes" })
public class UnifyAnalysisStateTest {
  @Test
  public void pseudoPartiotionKey() {
      UnifyAnalysisState s = new UnifyAnalysisState(NumericalType.INTERVAL);
      UnifyAnalysisState sa1 = s.assignElement(MemoryLocation.valueOf("a"), new IntegerInterval(1L, 1L), null);
      UnifyAnalysisState sb2 = s.assignElement(MemoryLocation.valueOf("b"), new IntegerInterval(2L, 2L), null);
      UnifyAnalysisState sa1b2 = sa1.assignElement(MemoryLocation.valueOf("b"), new IntegerInterval(2L, 2L), null);
      UnifyAnalysisState sa1b3 = sa1.assignElement(MemoryLocation.valueOf("b"), new IntegerInterval(3L, 3L), null);
      UnifyAnalysisState sa1b23 = sa1.assignElement(MemoryLocation.valueOf("b"), new IntegerInterval(2L, 3L), null);

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

@Test
public void testDropFrameIntervals() throws Exception {

    PersistentMap<MemoryLocation, NumberInterface> intervals = PathCopyingPersistentTreeMap.of();
    intervals = intervals.putAndCopy(MemoryLocation.valueOf("fu::a"), new IntegerInterval(1L, 1L));
    intervals = intervals.putAndCopy(MemoryLocation.valueOf("fu::b"), new IntegerInterval(2L, 2L));
    intervals = intervals.putAndCopy(MemoryLocation.valueOf("fu::c"), new IntegerInterval(2L, 2L));
    intervals = intervals.putAndCopy(MemoryLocation.valueOf("be::x"), new IntegerInterval(3L, 3L));
    intervals = intervals.putAndCopy(MemoryLocation.valueOf("be::y"), new IntegerInterval(2L, 3L));

    assertEquals(5, intervals.size());

    UnifyAnalysisState intervalUnifyAnalysis = new UnifyAnalysisState(intervals, NumericalType.INTERVAL);
    assertEquals(5, intervalUnifyAnalysis.getSize());
    assertEquals(2, intervalUnifyAnalysis.dropFrame("fu").getSize());
    assertEquals(5, intervalUnifyAnalysis.getSize());

}
@Test
public void testDropFrameSign() throws Exception {
//    UnifyAnalysisState valueUnifyAnalysis = new UnifyAnalysisState(NumericalType.VALUE);

    PersistentMap<MemoryLocation, NumberInterface> signMap = PathCopyingPersistentTreeMap.of();
    signMap = signMap.putAndCopy(MemoryLocation.valueOf("fu::a"), SIGN.EMPTY);
    signMap = signMap.putAndCopy(MemoryLocation.valueOf("fu::b"), SIGN.MINUS);
    signMap = signMap.putAndCopy(MemoryLocation.valueOf("fu::c"), SIGN.ALL);
    signMap = signMap.putAndCopy(MemoryLocation.valueOf("be::x"), SIGN.ALL);
    signMap = signMap.putAndCopy(MemoryLocation.valueOf("be::y"), SIGN.PLUS);

    assertEquals(5, signMap.size());

    UnifyAnalysisState signUnifyAnalysis = new UnifyAnalysisState(signMap, NumericalType.SIGN);
    assertEquals(5, signUnifyAnalysis.getSize());
    assertEquals(2, signUnifyAnalysis.dropFrame("fu").getSize());
    assertEquals(5, signUnifyAnalysis.getSize());


}
@Test
public void testDropFrameValue() throws Exception {
    UnifyAnalysisState valueUnifyAnalysis = new UnifyAnalysisState(NumericalType.VALUE);

    valueUnifyAnalysis.assignElement(MemoryLocation.valueOf("fu::a"), new NumericValue(1), null);
    valueUnifyAnalysis.assignElement(MemoryLocation.valueOf("fu::b"), new NumericValue(2), null);
    valueUnifyAnalysis.assignElement(MemoryLocation.valueOf("fu::c"), new NumericValue(3), null);
    valueUnifyAnalysis.assignElement(MemoryLocation.valueOf("be::x"), new NumericValue(4), null);
    valueUnifyAnalysis.assignElement(MemoryLocation.valueOf("be::y"), new NumericValue(5), null);

    assertEquals(5, valueUnifyAnalysis.getSize());
    assertEquals(2, valueUnifyAnalysis.dropFrame("fu").getSize());
    assertEquals(2, valueUnifyAnalysis.getSize());


}

}
