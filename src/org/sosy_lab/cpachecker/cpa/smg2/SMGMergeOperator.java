// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGAbstractionOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGMergeOptions;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.join.SMGMergeStatus;
import org.sosy_lab.cpachecker.util.smg.util.MergedSMGStateAndMergeStatus;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class SMGMergeOperator implements MergeOperator {

  private final SMGOptions options;
  private final SMGAbstractionOptions abstractionOptions;
  private final SMGMergeOptions mergeOptions;

  private final SMGCPAStatistics statistics;
  private final StatTimer totalMergeTimer;

  public SMGMergeOperator(SMGCPAStatistics pStatistics, SMGOptions pOptions) {
    statistics = pStatistics;
    totalMergeTimer = statistics.getMergeTime();
    options = pOptions;
    abstractionOptions = options.getAbstractionOptions();
    mergeOptions = options.getMergeOptions();
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

    Optional<MergedSMGStateAndMergeStatus> mergeResult = merge(newSMGState, smgStateFromReached);

    if (mergeResult.isPresent()) {
      return mergeResult.orElseThrow().getMergedSMGState();
    } else {
      // Try abstract input again and merge again

      // TODO: I found that there are states that SHOULD be abstracted more, e.g.:
      //  [0+] -> [concrete element equal to 0+] -> 0
      //  with no outside pointers, so it should be
      //  [1+] -> 0
      //  The reason is most likely the "dropping" of the outside pointer and not encountering a
      //  location where abstraction is allowed before the state is merged.
      SMGState abstrNewSMGState = tryToAbstractState(newSMGState);
      SMGState abstrSmgStateFromReached = tryToAbstractState(smgStateFromReached);
      if (abstrNewSMGState != newSMGState || abstrSmgStateFromReached != smgStateFromReached) {
        mergeResult = merge(abstrNewSMGState, abstrSmgStateFromReached);
        if (mergeResult.isPresent()) {
          return mergeResult.orElseThrow().getMergedSMGState();
        }
      }
    }

    return smgStateFromReached;
  }

  /**
   * If merge fails, returns empty. Else, returns the merged state and the merge status. Only for
   * tests. Note: ignores block-end status of states when merging.
   */
  public Optional<MergedSMGStateAndMergeStatus> mergeForTests(
      SMGState newSuccessorState, SMGState stateFromReached) throws CPAException {

    return merge(newSuccessorState, stateFromReached);
  }

  private Optional<MergedSMGStateAndMergeStatus> merge(
      SMGState newSMGState, SMGState smgStateFromReached) throws CPAException {

    // We only check at block ends, as this is where abstractions are performed.
    if (!mergeOptions.mergeOnlyOnBlockEnd()
        || (newSMGState.createdAtBlockEnd() && smgStateFromReached.createdAtBlockEnd())) {

      if (mergeOptions.mergeOnlyOnBlockEnd()
          && mergeOptions.mergeOnlyEqualBlockEnds()
          && !newSMGState.getBlockEnd().equals(smgStateFromReached.getBlockEnd())) {
        return Optional.empty();
      }

      // A merge w/o nested lists can be expensive, but most of the time not needed.
      Set<SMGSinglyLinkedListSegment> abstrObjs1 =
          newSMGState.getMemoryModel().getSmg().getAllValidAbstractedObjects();
      Set<SMGSinglyLinkedListSegment> abstrObjs2 =
          smgStateFromReached.getMemoryModel().getSmg().getAllValidAbstractedObjects();
      if (mergeOptions.mergeOnlyWithAbstractionPresent()
          && abstrObjs1.isEmpty()
          && abstrObjs2.isEmpty()) {
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
        // We might now be able to fold more items into an abstracted element
        mergedState = tryToAbstractState(mergedState);

        // Retain merge status to reason about in stop operator
        mergedState = mergedState.asResultOfMerge(newSMGState, smgStateFromReached, mergeStatus);
        // Retain block-end status
        if (smgStateFromReached.createdAtBlockEnd()) {
          mergedState = mergedState.withBlockEnd(smgStateFromReached.getBlockEnd());
        }
        checkState(mergedState.isResultOfMerge());
        checkState(
            !mergeOptions.mergeOnlyOnBlockEnd()
                || (!mergeOptions.mergeOnlyEqualBlockEnds()
                    || (mergedState.getBlockEnd() == smgStateFromReached.getBlockEnd())));

        // The merged state is strictly equally or more abstract than the input states.
        return Optional.of(MergedSMGStateAndMergeStatus.of(mergedState, mergeStatus));
      }
    }

    return Optional.empty();
  }

  private SMGState tryToAbstractState(SMGState mergedState) {
    try {
      return new SMGCPAAbstractionManager(
              mergedState,
              abstractionOptions.getListAbstractionMinimumLengthThreshold(),
              statistics)
          .findAndAbstractLists();
    } catch (SMGException e) {
      // Do nothing. This should never happen anyway
      throw new RuntimeException(e);
    }
  }
}
