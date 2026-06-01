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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class IntervalMergeOperatorTest {

  @Test
  public void wideningConvergesAscendingChain() throws CPAException, InterruptedException {
    CFANode loopHead = CFANode.newDummyCFANode();
    loopHead.setLoopStart();

    IntervalAnalysisState oldState =
        new IntervalAnalysisState(loopHead).addInterval("x", new Interval(0L, 0L), -1, loopHead);
    IntervalAnalysisState newState =
        new IntervalAnalysisState(loopHead).addInterval("x", new Interval(1L, 1L), -1, loopHead);

    IntervalMergeOperator mergeOp = new IntervalMergeOperator();
    AbstractState result = mergeOp.merge(newState, oldState, null);

    assertThat(result).isInstanceOf(IntervalAnalysisState.class);
    assertThat(
        ((IntervalAnalysisState) result).intervals().get("x")
    ).isEqualTo(new Interval(0L, Long.MAX_VALUE));
  }
}
