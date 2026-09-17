// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGMergeOptions.MergePolicy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.util.EitherMergedStateAndMergeStatusOrFailureInfo;
import org.sosy_lab.cpachecker.util.smg.util.MergedSMGStateAndMergeStatus;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class SMGMergeOperator implements MergeOperator {

  @SuppressWarnings("unused")
  private final SMGOptions options;

  private final SMGCPAStatistics statistics;

  public SMGMergeOperator(SMGCPAStatistics pStatistics, SMGOptions pOptions) {
    statistics = pStatistics;
    options = pOptions;
  }

  // If the returned state is not equal to the second input state (the state from the reached-set),
  // then the input state from the reached set is removed from the reached-set (and waitlist) and
  // the new state returned from this method is put into the reached-set (and waitlist).
  // Independently, the new successor state is put into the stop operator after all merges have
  // concluded. As a consequence, a newly merged state is checked against the new successor in STOP,
  // so we retain the merge info to speed the stop operator up.
  @Override
  public AbstractState merge(
      AbstractState pNewSuccessorState, AbstractState pStateFromReached, Precision precision)
      throws CPAException, InterruptedException {

    SMGState newSuccessorState = (SMGState) pNewSuccessorState;
    SMGState stateFromReached = (SMGState) pStateFromReached;

    MergePolicy mergePolicy = newSuccessorState.getMergePolicy();
    if (mergePolicy == MergePolicy.SEP) {
      return stateFromReached;
    }

    StatTimer timeForMergeWithoutPreprocessingAndChecks =
        statistics.getTimeForMergeWithoutPreprocessingAndChecks();

    statistics.incrementMergeOperatorCalls();
    statistics.getTotalMergeOperatorTime().start();

    // Merging states from equal locations is the most useful. We do not restrict users in this
    // though.
    // Some settings may howver restrict allowed locations:
    EitherMergedStateAndMergeStatusOrFailureInfo mergeResult =
        SMGState.mergeStatesWithPostProcessing(
            newSuccessorState,
            stateFromReached,
            mergePolicy,
            false,
            Optional.of(timeForMergeWithoutPreprocessingAndChecks));

    if (mergeResult.failedDueToPrecisionLoss()) {
      statistics.incrementFailedMergesDueToPrecisionLoss();
    } else if (mergeResult.isMergedSuccessfully()) {
      statistics.incrementNumberOfSuccessfulMerges();
    }

    timeForMergeWithoutPreprocessingAndChecks.stopIfRunning();
    statistics.getTotalMergeOperatorTime().stop();
    return stateFromReached;
  }

  /**
   * If merge fails, returns empty. Else, returns the merged state and the merge status. Only for
   * tests.
   */
  public Optional<MergedSMGStateAndMergeStatus> mergeForTests(
      SMGState newSuccessorState, SMGState stateFromReached) throws CPAException {

    EitherMergedStateAndMergeStatusOrFailureInfo mergeRes =
        SMGState.mergeStatesWithPostProcessing(
            newSuccessorState,
            stateFromReached,
            MergePolicy.MERGE_WITH_ABSTRACTION,
            false,
            Optional.empty());

    if (mergeRes.isMergedSuccessfully()) {
      return Optional.of(mergeRes.getMergedStateAndMergeStatus());
    }
    return Optional.empty();
  }
}
