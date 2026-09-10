// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * A reached set that partitions states by location, <em>call chain</em> and deepest loop iteration.
 *
 * <p>This exists for the induction step case of k-induction. The default {@link
 * PartitionedReachedSet} asks every {@link org.sosy_lab.cpachecker.core.interfaces.Partitionable}
 * component for a key, and two of those components make the key so fine that each state ends up
 * alone in its partition: {@link CallstackState} is compared by object identity by design, and
 * {@link org.sosy_lab.cpachecker.cpa.loopbound.LoopBoundState} carries the whole per-loop iteration
 * vector. Since merge is only ever attempted between states of the same partition, and since in BMC
 * mode merge^JOIN is the only mechanism that can collapse states at all, nothing is ever collapsed.
 *
 * <p>Partitioning by location alone ({@link LocationMappedReachedSet}) does enable merging, but
 * makes every partition so large that each merge scans all states at that location. This class uses
 * the granularity in between: states are compared exactly when they are at the same location, were
 * reached through the same chain of calls, and agree on the deepest loop iteration.
 *
 * <p>Partitioning affects only <em>which</em> states are compared, never whether comparing them
 * succeeds; coverage and merging remain governed by the CPAs' own operators. Making the partition
 * coarser is therefore always semantically safe. To actually benefit from it, the callstack and
 * loop-bound CPAs need to be configured to compare states accordingly (see {@code
 * cpa.callstack.domain} and {@code cpa.loopbound.domain}).
 */
public class InductionPartitionedReachedSet extends PartitionedReachedSet {

  public InductionPartitionedReachedSet(
      ConfigurableProgramAnalysis pCpa, WaitlistFactory pWaitlistFactory) {
    super(pCpa, pWaitlistFactory);
  }

  @Override
  protected Object getPartitionKey(AbstractState pState) {
    CFANode location = AbstractStates.extractLocation(pState);
    CallstackState callstack = AbstractStates.extractStateByType(pState, CallstackState.class);
    LoopIterationReportingState loopState =
        AbstractStates.extractStateByType(pState, LoopIterationReportingState.class);
    return List.of(
        location == null ? "" : location,
        callstack == null ? "" : new CallstackStateEqualsWrapper(callstack),
        loopState == null ? -1 : loopState.getDeepestIteration());
  }
}
