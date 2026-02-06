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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DssBlockAnalyses {

  private DssBlockAnalyses() {}

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
  static void executeCpaAlgorithmWithStates(
      ReachedSet reachedSet, ConfigurableProgramAnalysis pCpa, List<StateAndPrecision> pStates)
      throws InterruptedException, CPAException {
    for (StateAndPrecision stateAndPrecision : pStates) {
      AbstractState state = stateAndPrecision.state();
      CFANode location = AbstractStates.extractLocation(state);
      assert location != null;
      if (reachedSet.isEmpty()) {
        reachedSet.add(state, stateAndPrecision.precision());
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
          if (!pCpa.getStopOperator()
              .stop(
                  state,
                  reachedSet.getReached(location),
                  pCpa.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()))) {
            reachedSet.add(
                state,
                pCpa.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()));
          }
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

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    // find all target states in block, except target states that are only reachable from another
    // target state
    while (pReachedSet.hasWaitingState()) {
      status = status.update(pAlgorithm.run(pReachedSet));
      AbstractStates.getTargetStates(pReachedSet).forEach(pReachedSet::removeOnlyFromWaitlist);
    }

    return new DssBlockAnalysisResult(pReachedSet, pBlockNode, status);
  }

  static class DssBlockAnalysisResult {

    private final ImmutableSet<ARGState> summaries;
    private final ImmutableSet<ARGState> finalLocationStates;
    private final ImmutableSet<ARGState> allViolations;
    private final ImmutableSet<ARGState> vcViolations;
    private final ImmutableSet<ARGState> targetStates;
    private final AlgorithmStatus status;

    /**
     * Interpret the reached set after the block analysis. We collect all states at the final
     * location, all target states (violations) and all summary states (final location, not target,
     * no children).
     *
     * @param pReachedSet the reached set after the block analysis
     * @param pBlockNode the block node that was analyzed
     * @param pStatus the status returned by the analysis algorithm
     */
    private DssBlockAnalysisResult(
        ReachedSet pReachedSet, BlockNode pBlockNode, AlgorithmStatus pStatus) {
      status = pStatus;
      ImmutableSet.Builder<ARGState> summariesBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> violationsBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> vcViolationsBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> targetStatesBuilder = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> finalLocationBuilder = ImmutableSet.builder();
      for (AbstractState abstractState : pReachedSet) {
        ARGState argState = (ARGState) abstractState;
        BlockState blockState =
            Objects.requireNonNull(AbstractStates.extractStateByType(argState, BlockState.class));
        if (blockState.getType() == BlockStateType.INITIAL) {
          continue;
        }
        if (blockState.getType() == BlockStateType.FINAL) {
          finalLocationBuilder.add(argState);
        }
        if (argState.isTarget()) {
          // if we find a target state, it is either a real violation
          // or the ghost edge was reached (violation condition cannot be refuted)
          violationsBuilder.add(argState);
          if (blockState.getType() == BlockStateType.ABSTRACTION) {
            vcViolationsBuilder.add(argState);
          } else {
            targetStatesBuilder.add(argState);
          }
        } else if (blockState.getLocationNode().equals(pBlockNode.getFinalLocation())
            && blockState.getType() == BlockStateType.FINAL
            && argState.getChildren().isEmpty()) {
          summariesBuilder.add(argState);
        }
      }
      allViolations = violationsBuilder.build();
      summaries = summariesBuilder.build();
      finalLocationStates = finalLocationBuilder.build();
      vcViolations = vcViolationsBuilder.build();
      targetStates = targetStatesBuilder.build();
    }

    public AlgorithmStatus getStatus() {
      return status;
    }

    public ImmutableSet<ARGState> getSummaries() {
      return summaries;
    }

    public ImmutableSet<ARGState> getAllViolations() {
      return allViolations;
    }

    public ImmutableSet<ARGState> getTargetStates() {
      return targetStates;
    }

    public ImmutableSet<ARGState> getViolationConditionViolations() {
      return vcViolations;
    }

    public ImmutableSet<ARGState> getFinalLocationStates() {
      return finalLocationStates;
    }

    @Override
    public String toString() {
      return "DssBlockAnalysisResult{"
          + "abstractionStates="
          + summaries
          + ", violationStates="
          + allViolations
          + ", status="
          + status
          + '}';
    }
  }
}
