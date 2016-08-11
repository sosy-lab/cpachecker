/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tgar.comparator;

import static org.mockito.Mockito.*;

import com.google.common.truth.Truth;

import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.Comparator;

public class ComparatorTest {

  @Test
  public void testDeeperLevelFirstComparator() {
    ARGState shallow = mock(ARGState.class);
    when(shallow.getStateLevel()).thenReturn(1);

    ARGState deep = mock(ARGState.class);
    when(deep.getStateLevel()).thenReturn(9);

    Comparator<ARGState> c = new DeeperLevelFirstComparator();

    Truth.assertThat(c.compare(deep, shallow)).isLessThan(0);
    Truth.assertThat(c.compare(shallow, deep)).isGreaterThan(0);
    Truth.assertThat(c.compare(shallow, shallow)).isEqualTo(0);
  }

  @Test
  public void testMostHandledFirstComparator() {
    MostHandledFirstComparator c = mock(MostHandledFirstComparator.class);

    ARGState moreInactive = mock(ARGState.class);
    when(c.getInactiveCount(moreInactive)).thenReturn(9);

    ARGState lessInactive = mock(ARGState.class);
    when(c.getInactiveCount(lessInactive)).thenReturn(1);

    when(c.compare(moreInactive, lessInactive)).thenCallRealMethod();
    Truth.assertThat(c.compare(moreInactive, lessInactive)).isLessThan(0);

    when(c.compare(lessInactive, moreInactive)).thenCallRealMethod();
    Truth.assertThat(c.compare(lessInactive, moreInactive)).isGreaterThan(0);

    when(c.compare(moreInactive, moreInactive)).thenCallRealMethod();
    Truth.assertThat(c.compare(moreInactive, moreInactive)).isEqualTo(0);
  }

}
