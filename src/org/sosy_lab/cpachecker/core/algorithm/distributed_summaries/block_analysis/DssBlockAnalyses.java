// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DssBlockAnalyses {

  private DssBlockAnalyses() {}

  private static List<AbstractState> extractBlockStatesAtGhostLocation(ReachedSet pAbstractStates) {
    return FluentIterable.from(pAbstractStates)
        .filter(
            a ->
                AbstractStates.extractStateByType(a, BlockState.class).getType()
                    == BlockStateType.ABSTRACTION)
        .toList();
  }

  private static Multimap<BlockState, AbstractState> sortGhostStatesByPredecessor(
      List<AbstractState> states) {
    return Multimaps.index(
        states, a -> AbstractStates.extractStateByType(a, BlockState.class).getPredecessor());
  }

  /**
   * Analyze the code block until all target states in this block are found. Block entry points
   * (initial and final location) are target states, too.
   *
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  static DssBlockAnalysisResult runAlgorithm(Algorithm pAlgorithm, ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    // find all target states in block, except target states that are only reachable from another
    // target state
    Multimap<BlockState, AbstractState> previousConditions = ArrayListMultimap.create();
    while (pReachedSet.hasWaitingState()) {
      status = status.update(pAlgorithm.run(pReachedSet));
      AbstractStates.getTargetStates(pReachedSet).forEach(pReachedSet::removeOnlyFromWaitlist);
      ImmutableMap<BlockState, AbstractState> blockStateToState =
          Maps.uniqueIndex(
              pReachedSet, a -> AbstractStates.extractStateByType(a, BlockState.class));
      Multimap<BlockState, AbstractState> predecessorToStates =
          sortGhostStatesByPredecessor(extractBlockStatesAtGhostLocation(pReachedSet));
      for (BlockState blockState : predecessorToStates.keySet()) {
        Preconditions.checkState(blockState.getType() == BlockStateType.FINAL);
        ImmutableSet<? extends @NonNull AbstractState> processedViolationConditions =
            FluentIterable.from(predecessorToStates.get(blockState))
                .transformAndConcat(
                    a ->
                        AbstractStates.extractStateByType(a, BlockState.class)
                            .getViolationConditions())
                .toSet();
        ImmutableList.Builder<AbstractState> remainingConditionsBuilder = ImmutableList.builder();
        for (AbstractState violationCondition : blockState.getViolationConditions()) {
          if (!processedViolationConditions.contains(violationCondition)) {
            remainingConditionsBuilder.add(violationCondition);
          }
        }
        ImmutableList<AbstractState> remainingConditions = remainingConditionsBuilder.build();
        Collection<AbstractState> previous = previousConditions.removeAll(blockState);
        if (ImmutableSet.copyOf(previous).equals(ImmutableSet.copyOf(remainingConditions))) {
          pReachedSet.removeOnlyFromWaitlist(blockStateToState.get(blockState));
        } else {
          previousConditions.putAll(blockState, remainingConditions);
          if (remainingConditions.isEmpty()) {
            pReachedSet.removeOnlyFromWaitlist(blockStateToState.get(blockState));
          }
          blockState.setViolationConditions(remainingConditions);
        }
      }
    }

    return new DssBlockAnalysisResult(pReachedSet, status);
  }

  static class DssBlockAnalysisResult {

    private final ImmutableSet<ARGState> finalLocationStates;
    private final ImmutableSet<ARGState> allViolations;
    private final ImmutableSet<ARGState> vcViolations;
    private final ImmutableSet<ARGState> targetStates;
    private final AlgorithmStatus status;

    /**
     * Interpret the reached set after the block analysis. We collect all states at the final
     * location and all target states (violations).
     *
     * @param pReachedSet the reached set after the block analysis
     * @param pStatus the status returned by the analysis algorithm
     */
    private DssBlockAnalysisResult(ReachedSet pReachedSet, AlgorithmStatus pStatus) {
      status = pStatus;
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
        }
      }
      allViolations = violationsBuilder.build();
      finalLocationStates = finalLocationBuilder.build();
      vcViolations = vcViolationsBuilder.build();
      targetStates = targetStatesBuilder.build();
    }

    public AlgorithmStatus getStatus() {
      return status;
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
          + "finalLocationStates="
          + finalLocationStates
          + ", violationStates="
          + allViolations
          + ", status="
          + status
          + '}';
    }
  }
}
