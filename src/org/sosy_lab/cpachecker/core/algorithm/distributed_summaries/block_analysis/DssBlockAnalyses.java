// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DssBlockAnalyses {

  private DssBlockAnalyses() {}

  static DssMessagePayload appendStatus(
      AlgorithmStatus pStatus, DssMessagePayload pCurrentPayload) {
    return DssMessagePayload.builder()
        .addAllEntries(pCurrentPayload)
        .addEntry(
            DssMessagePayload.PROPERTY,
            pStatus.wasPropertyChecked()
                ? StatusPropertyChecked.CHECKED.name()
                : StatusPropertyChecked.UNCHECKED.name())
        .addEntry(
            DssMessagePayload.SOUND,
            pStatus.isSound() ? StatusSoundness.SOUND.name() : StatusSoundness.UNSOUND.name())
        .addEntry(
            DssMessagePayload.PRECISE,
            pStatus.isPrecise() ? StatusPrecise.PRECISE.name() : StatusPrecise.IMPRECISE.name())
        .buildPayload();
  }

  /**
   * Analyze the code block until all target states in this block are found. Block entry points
   * (initial and final location) are target states, too.
   *
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  static DssBlockAnalysisIntermediateResult runCpaAlgorithm(
      Algorithm pAlgorithm, ReachedSet pReachedSet, BlockNode pBlockNode)
      throws CPAException, InterruptedException {

    AbstractState startState = pReachedSet.getFirstState();
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    // find all target states in block, except target states that are only reachable from another
    // target state
    while (pReachedSet.hasWaitingState()) {
      status = status.update(pAlgorithm.run(pReachedSet));
      AbstractStates.getTargetStates(pReachedSet).forEach(pReachedSet::removeOnlyFromWaitlist);
    }

    return new DssBlockAnalysisIntermediateResult(pReachedSet, startState, pBlockNode, status);
  }

  static class DssBlockAnalysisIntermediateResult {

    private final ImmutableSet<ARGState> summaries;
    private final ImmutableSet<ARGState> violations;
    private final AlgorithmStatus status;

    private DssBlockAnalysisIntermediateResult(
        ReachedSet pReachedSet,
        AbstractState pStartState,
        BlockNode pBlockNode,
        AlgorithmStatus pStatus) {
      status = pStatus;
      ImmutableSet.Builder<ARGState> summariesBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> violationsBuilder = ImmutableSet.builder();
      for (AbstractState abstractState : pReachedSet) {
        if (abstractState.equals(pStartState)) {
          continue;
        }
        ARGState argState = (ARGState) abstractState;
        if (argState.isTarget()) {
          violationsBuilder.add(argState);
        } else if (pBlockNode.getLast().equals(AbstractStates.extractLocation(argState))) {
          summariesBuilder.add(argState);
        }
      }
      summaries = summariesBuilder.build();
      violations = violationsBuilder.build();
    }

    public AlgorithmStatus getStatus() {
      return status;
    }

    public ImmutableSet<ARGState> getSummaries() {
      return summaries;
    }

    public ImmutableSet<ARGState> getViolations() {
      return violations;
    }

    @Override
    public String toString() {
      return "DssBlockAnalysisIntermediateResult{"
          + "abstractionStates="
          + summaries
          + ", violationStates="
          + violations
          + ", status="
          + status
          + '}';
    }
  }
}
