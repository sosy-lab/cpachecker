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
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetState.Status;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class TestTargetTransferRelation extends SingleEdgeTransferRelation {

  // Change from final to allow updates for parallel test generation
  // Volatile ensures visibility of changes across threads
  private volatile Set<CFAEdge> testTargets;

  /**
   * Constructor - initializes with the given test targets
   *
   * @param pTestTargets Initial set of test targets
   */
  TestTargetTransferRelation(final Set<CFAEdge> pTestTargets) {
    // Create copy for initial value
    testTargets = new HashSet<>(pTestTargets);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState, final Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    // Local variable for thread-safe access to volatile field
    Set<CFAEdge> currentTargets = testTargets;
    checkNotNull(currentTargets);
    Preconditions.checkArgument(
        pState instanceof TestTargetState,
        "Abstract state in TestTargetTransferRelation not an element of TestTargetState");

    if (((TestTargetState) pState).isStop()) {
      return ImmutableSet.of();
    }

    return Collections.singleton(
        currentTargets.contains(pCfaEdge)
            ? new TestTargetState(Status.TARGET)
            : TestTargetState.noTargetState());
  }

  /**
   * Returns the current set of test targets.
   *
   * @return Current set of test targets (may be immutable)
   */
  public Set<CFAEdge> getTestTargets() {
    return testTargets;
  }

  /**
   * Sets a new set of test targets for this transfer relation. This method enables parallel test
   * generation where each thread processes a different subset of the global test targets.
   *
   * <p>The field is marked as volatile to ensure that updates are visible to all threads
   * immediately.
   *
   * @param pTestTargets The new set of test targets to use
   */
  public void setTestTargets(Set<CFAEdge> pTestTargets) {
    // Use a regular HashSet (not thread-safe, but fine if only one thread uses it)
    this.testTargets = new HashSet<>(pTestTargets);
  }
}