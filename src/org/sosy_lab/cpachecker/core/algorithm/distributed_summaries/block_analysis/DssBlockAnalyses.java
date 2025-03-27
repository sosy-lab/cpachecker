// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
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
   * Simulate the CPA algorithm on the given reached set using the states provided in the list. The
   * CPA algorithm is performed on the given reached set (by reference).
   *
   * @param reachedSet the reached set to perform the CPA algorithm on
   * @param pCpa the CPA to use
   * @param pStates the states to put into the reached set one-by-one starting with the first one.
   *     All states are required to have a location and should emerge from the same location.
   * @throws InterruptedException thread interrupted during precision computation
   * @throws CPAException wrapper exception for all CPA exceptions
   */
  static void performCpaAlgorithmWithStates(
      ReachedSet reachedSet, ConfigurableProgramAnalysis pCpa, List<AbstractState> pStates)
      throws InterruptedException, CPAException {
    for (AbstractState state : pStates) {
      CFANode location = AbstractStates.extractLocation(state);
      assert location != null;
      if (reachedSet.isEmpty()) {
        reachedSet.add(
            state, pCpa.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()));
      } else {
        // CPA algorithm
        for (AbstractState abstractState : ImmutableSet.copyOf(reachedSet)) {
          AbstractState merged =
              pCpa.getMergeOperator()
                  .merge(
                      state,
                      abstractState,
                      pCpa.getInitialPrecision(
                          location, StateSpacePartition.getDefaultPartition()));
          if (!merged.equals(abstractState)) {
            reachedSet.remove(abstractState);
            reachedSet.add(
                merged,
                pCpa.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()));
          }
        }
        if (!pCpa.getStopOperator()
            .stop(
                state,
                reachedSet.getReached(location),
                pCpa.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()))) {
          reachedSet.add(
              state, pCpa.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()));
        }
      }
    }
  }

  /**
   * Analyze the code block until all target states in this block are found. Block entry points
   * (initial and final location) are target states, too.
   *
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  static DssBlockAnalysisResult runAlgorithm(
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

    return new DssBlockAnalysisResult(pReachedSet, startState, pBlockNode, status);
  }

  static class DssBlockAnalysisResult {

    private final ImmutableSet<ARGState> summaries;
    private final ImmutableSet<ARGState> violations;
    private final AlgorithmStatus status;

    private DssBlockAnalysisResult(
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
          // if we find a target state, it is either a real violation
          // or the ghost edge was reached (violation condition cannot be refuted)
          violationsBuilder.add(argState);
        } else if (pBlockNode.getFinalLocation().equals(AbstractStates.extractLocation(argState))) {
          summariesBuilder.add(argState);
        }
      }
      violations = violationsBuilder.build();
      summaries = summariesBuilder.build();
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
      return "DssBlockAnalysisResult{"
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
