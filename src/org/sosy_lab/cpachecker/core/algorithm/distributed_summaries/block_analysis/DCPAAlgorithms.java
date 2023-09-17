// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockEntryReachedTargetInformation;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DCPAAlgorithms {

  private DCPAAlgorithms() {}

  static Optional<CFANode> abstractStateToLocation(AbstractState state) {
    return Optional.ofNullable(AbstractStates.extractLocation(state))
        .or(
            () ->
                Optional.ofNullable(AbstractStates.extractStateByType(state, BlockState.class))
                    .map(BlockState::getLocationNode));
  }

  static BlockSummaryMessagePayload appendStatus(
      AlgorithmStatus pStatus, BlockSummaryMessagePayload pCurrentPayload) {
    return BlockSummaryMessagePayload.builder()
        .addAllEntries(pCurrentPayload)
        .addEntry(
            BlockSummaryMessagePayload.PROPERTY,
            pStatus.wasPropertyChecked()
                ? StatusPropertyChecked.CHECKED.name()
                : StatusPropertyChecked.UNCHECKED.name())
        .addEntry(
            BlockSummaryMessagePayload.SOUND,
            pStatus.isSound() ? StatusSoundness.SOUND.name() : StatusSoundness.UNSOUND.name())
        .addEntry(
            BlockSummaryMessagePayload.PRECISE,
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
  static BlockAnalysisIntermediateResult findReachableTargetStatesInBlock(
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

    return new BlockAnalysisIntermediateResult(pReachedSet, startState, pBlockNode, status);
  }

  static class BlockAnalysisIntermediateResult {

    private final ImmutableSet<ARGState> abstractionStates;
    private final ImmutableSet<ARGState> violationStates;
    private final ImmutableSet<ARGState> blockEndStates;
    private final AlgorithmStatus status;

    private BlockAnalysisIntermediateResult(
        ReachedSet pReachedSet,
        AbstractState pStartState,
        BlockNode pBlockNode,
        AlgorithmStatus pStatus) {
      ImmutableSet.Builder<ARGState> abstractions = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> violations = ImmutableSet.builder();
      ImmutableSet.Builder<ARGState> blockEnds = ImmutableSet.builder();
      for (AbstractState abstractState : pReachedSet) {
        if (abstractState.equals(pStartState)) {
          continue;
        }
        ARGState argState = (ARGState) abstractState;
        abstractStateToLocation(argState)
            .ifPresent(
                a -> {
                  if (a.equals(pBlockNode.getLast())) {
                    blockEnds.add(argState);
                  }
                });
        if (argState.isTarget()) {
          if (argState.getTargetInformation().isEmpty()) {
            violations.add(argState);
            continue;
          }
          for (TargetInformation targetInformation : argState.getTargetInformation()) {
            if (targetInformation instanceof BlockEntryReachedTargetInformation) {
              // only on abstraction locations we can find this information
              abstractions.add(argState);
            } else {
              // specification violation otherwise
              violations.add(argState);
            }
          }
        }
      }
      abstractionStates = abstractions.build();
      violationStates = violations.build();
      blockEndStates = blockEnds.build();
      status = pStatus;
    }

    public AlgorithmStatus getStatus() {
      return status;
    }

    public ImmutableSet<ARGState> getAbstractionStates() {
      return abstractionStates;
    }

    public ImmutableSet<ARGState> getBlockEndStates() {
      return blockEndStates;
    }

    public ImmutableSet<ARGState> getViolationStates() {
      return violationStates;
    }

    @Override
    public String toString() {
      return "BlockAnalysisIntermediateResult{"
          + "abstractionStates="
          + abstractionStates
          + ", violationStates="
          + violationStates
          + ", blockEndStates="
          + blockEndStates
          + ", status="
          + status
          + '}';
    }
  }
}
