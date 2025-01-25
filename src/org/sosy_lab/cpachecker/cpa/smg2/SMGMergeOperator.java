// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

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

  @Override
  public AbstractState merge(
      AbstractState newState, AbstractState stateFromReached, Precision precision)
      throws CPAException, InterruptedException {

    SMGState newSMGState = (SMGState) newState;
    SMGState smgStateFromReached = (SMGState) stateFromReached;

    // A merge w/o nested lists is expensive but most of the time not needed
    Set<SMGSinglyLinkedListSegment> abstrObjs1 =
        newSMGState.getMemoryModel().getSmg().getAllValidAbstractedObjects();
    Set<SMGSinglyLinkedListSegment> abstrObjs2 =
        smgStateFromReached.getMemoryModel().getSmg().getAllValidAbstractedObjects();
    if (abstrObjs1.isEmpty() && abstrObjs2.isEmpty()) {
      return smgStateFromReached;
    }

    totalMergeTimer.start();
    statistics.incrementMergeAttempts();
    Optional<MergedSMGStateAndMergeStatus> maybeMergedStateAndStatus =
        newSMGState.merge(smgStateFromReached);
    totalMergeTimer.stop();

    if (maybeMergedStateAndStatus.isPresent()) {
      statistics.incrementNumberOfSuccessfulMerges();

      if (maybeMergedStateAndStatus.orElseThrow().getMergeStatus() == SMGMergeStatus.EQUAL) {
        // Don't merge equal states, as they are subsumed with the stop-operator
        return smgStateFromReached;
      }

      // Retain block-end status
      SMGState mergedState = maybeMergedStateAndStatus.orElseThrow().getMergedSMGState();
      if (newSMGState.createdAtBlockEnd() && smgStateFromReached.createdAtBlockEnd()) {
        mergedState = mergedState.withBlockEnd(newSMGState.getBlockEnd());
      }

      newSMGState.setMergedInto(mergedState);
      smgStateFromReached.setMergedInto(mergedState);
      return mergedState;
    }

    return smgStateFromReached;
  }
}
