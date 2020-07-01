/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
    this.testTargets = pTestTargets;
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
