// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetState.Status;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class TestTargetTransferRelation extends SingleEdgeTransferRelation {

  private final Set<CFAEdge> testTargets;

  TestTargetTransferRelation(final Set<CFAEdge> pTestTargets) {
    testTargets = pTestTargets;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState, final Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    checkNotNull(testTargets);
    Preconditions.checkArgument(
        pState instanceof TestTargetState,
        "Abstract state in TestTargetTransferRelation not an element of TestTargetState");

    if (((TestTargetState) pState).isStop()) {
      return ImmutableSet.of();
    }

    return Collections.singleton(
        testTargets.contains(pCfaEdge)
            ? new TestTargetState(Status.TARGET)
            : TestTargetState.noTargetState());
  }

  public Set<CFAEdge> getTestTargets() {
    return testTargets;
  }
}
