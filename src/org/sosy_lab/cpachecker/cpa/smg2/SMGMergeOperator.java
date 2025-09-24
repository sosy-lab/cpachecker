// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;
import org.sosy_lab.cpachecker.util.smg.util.MergedSMGStateAndMergeStatus;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class SMGMergeOperator implements MergeOperator {

  private final SMGCPAStatistics statistics;
  private final StatTimer totalMergeTimer;

  public SMGMergeOperator(SMGCPAStatistics pStatistics) {
    statistics = pStatistics;
    totalMergeTimer = statistics.getMergeTime();
  }

  // If the returned state is not equal to the second input state (the state from the reached-set),
  // then the input state from the reached set is removed from the reached-set (and waitlist) and
  // the new state returned from this method is put into the reached-set (and waitlist).
  // Independently, the new successor state is put into the stop operator after all merges have
  // concluded. As a consequence, a newly merged state is checked against the new successor in STOP,
  // so we retain the merge info to speed the stop operator up.
  @Override
  public AbstractState merge(
      AbstractState newSuccessorState, AbstractState stateFromReached, Precision precision)
      throws CPAException, InterruptedException {

    SMGState newSMGState = (SMGState) newSuccessorState;
    SMGState smgStateFromReached = (SMGState) stateFromReached;

    Optional<MergedSMGStateAndMergeStatus> mergeResult =
        merge(newSMGState, smgStateFromReached, false);
    if (mergeResult.isPresent()) {
      return mergeResult.orElseThrow().getMergedSMGState();
    }

    return smgStateFromReached;
  }

  /**
   * If merge fails, returns empty. Else, returns the merged state and the merge status. Only for
   * tests. Note: ignores block-end status of states when merging.
   */
  public Optional<MergedSMGStateAndMergeStatus> mergeForTests(
      SMGState newSuccessorState, SMGState stateFromReached) throws CPAException {

    return merge(newSuccessorState, stateFromReached, true);
  }

  private Optional<MergedSMGStateAndMergeStatus> merge(
      SMGState newSMGState, SMGState smgStateFromReached, boolean ignoreBlockEnds)
      throws CPAException {
    checkArgument(!newSMGState.isResultOfMerge());

    // We only check at block ends, as this is where abstractions are performed.
    if (!newSMGState.mergeAtBlockEnd()
        || (newSMGState.createdAtBlockEnd() && smgStateFromReached.createdAtBlockEnd())
        || ignoreBlockEnds) {

      // TODO: add option for strict and not so strict interpretation of this!
      if (!ignoreBlockEnds
          && !newSMGState.getBlockEnd().equals(smgStateFromReached.getBlockEnd())) {
        return Optional.empty();
      }

      // A merge w/o nested lists can be expensive, but most of the time not needed.
      // TODO: allow with option, as it can havoc non-equal values and allows for a broader, but
      // less precise abstraction.
      Set<SMGSinglyLinkedListSegment> abstrObjs1 =
          newSMGState.getMemoryModel().getSmg().getAllValidAbstractedObjects();
      Set<SMGSinglyLinkedListSegment> abstrObjs2 =
          smgStateFromReached.getMemoryModel().getSmg().getAllValidAbstractedObjects();
      if (abstrObjs1.isEmpty() && abstrObjs2.isEmpty()) {
        return Optional.empty();
      }

      totalMergeTimer.start();
      statistics.incrementMergeAttempts();

      Optional<MergedSMGStateAndMergeStatus> maybeMergedStateAndStatus =
          newSMGState.merge(smgStateFromReached);

      totalMergeTimer.stop();

      if (maybeMergedStateAndStatus.isPresent()) {
        statistics.incrementNumberOfSuccessfulMerges();
        MergedSMGStateAndMergeStatus mergedStateAndStatus = maybeMergedStateAndStatus.orElseThrow();
        SMGMergeStatus mergeStatus = mergedStateAndStatus.getMergeStatus();

        SMGState mergedState = mergedStateAndStatus.getMergedSMGState();
        // Retain merge status to reason about in stop operator
        mergedState = mergedState.asResultOfMerge(newSMGState, smgStateFromReached, mergeStatus);
        // Retain block-end status
        if (smgStateFromReached.createdAtBlockEnd()) {
          mergedState = mergedState.withBlockEnd(smgStateFromReached.getBlockEnd());
        }
        checkState(mergedState.isResultOfMerge());
        checkState(
            ignoreBlockEnds || (mergedState.getBlockEnd() == smgStateFromReached.getBlockEnd()));

        // The merged state is strictly equally or more abstract than the input states.
        return Optional.of(MergedSMGStateAndMergeStatus.of(mergedState, mergeStatus));
      }
    }

    return Optional.empty();
  }
}
