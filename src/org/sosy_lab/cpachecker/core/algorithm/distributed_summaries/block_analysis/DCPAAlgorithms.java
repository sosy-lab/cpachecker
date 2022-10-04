// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
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
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DCPAAlgorithms {

  static Optional<CFANode> abstractStateToLocation(AbstractState state) {
    LocationState locState = AbstractStates.extractStateByType(state, LocationState.class);
    if (locState != null) {
      return Optional.of(locState.getLocationNode());
    }
    BlockState blockState = AbstractStates.extractStateByType(state, BlockState.class);
    if (blockState != null) {
      return Optional.of(blockState.getLocationNode());
    }
    return Optional.empty();
  }

  /**
   * Find all error locations in a set of target states
   *
   * @param targetStates abstract states with target information
   * @return subset of targetStates where the target information equals {@link
   *     BlockEntryReachedTargetInformation}
   */
  static ImmutableSet<ARGState> extractBlockTargetStates(Set<ARGState> targetStates) {
    ImmutableSet.Builder<ARGState> blockTargetStates = ImmutableSet.builder();
    for (ARGState targetState : targetStates) {
      for (TargetInformation targetInformation : targetState.getTargetInformation()) {
        if (targetInformation instanceof BlockEntryReachedTargetInformation) {
          blockTargetStates.add(targetState);
          break;
        }
      }
    }
    return blockTargetStates.build();
  }

  /**
   * Find all error locations in a set of target states
   *
   * @param targetStates abstract states with target information
   * @return subset of targetStates where the target information is some kind of specification
   *     violation
   */
  static ImmutableSet<ARGState> extractViolations(Set<ARGState> targetStates) {
    ImmutableSet.Builder<ARGState> violationStates = ImmutableSet.builder();
    for (ARGState targetState : targetStates) {
      for (TargetInformation targetInformation : targetState.getTargetInformation()) {
        if (!(targetInformation instanceof BlockEntryReachedTargetInformation)) {
          violationStates.add(targetState);
          break;
        }
      }
    }
    return violationStates.build();
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
      Algorithm pAlgorithm, ReachedSet pReachedSet) throws CPAException, InterruptedException {

    AbstractState startState = pReachedSet.getFirstState();
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    // find all target states in block, except target states that are only reachable from another
    // target state
    while (pReachedSet.hasWaitingState()) {
      status = status.update(pAlgorithm.run(pReachedSet));
      AbstractStates.getTargetStates(pReachedSet).forEach(pReachedSet::removeOnlyFromWaitlist);
    }

    return BlockAnalysisIntermediateResult.of(
        from(pReachedSet)
            .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .filter(AbstractStates::isTargetState)
            .filter(s -> !Objects.equals(startState, s))
            .toSet(),
        status);
  }

  public static class BlockAnalysisIntermediateResult {

    private final ImmutableSet<ARGState> targets;
    private final ImmutableSet<ARGState> blockTargets;
    private final AlgorithmStatus status;

    private BlockAnalysisIntermediateResult(
        ImmutableSet<ARGState> pTargets, AlgorithmStatus pStatus) {
      blockTargets = extractBlockTargetStates(pTargets);
      targets = extractViolations(pTargets);
      status = pStatus;
    }

    private static BlockAnalysisIntermediateResult of(
        ImmutableSet<ARGState> pTargets, AlgorithmStatus pStatus) {
      return new BlockAnalysisIntermediateResult(pTargets, pStatus);
    }

    public AlgorithmStatus getStatus() {
      return status;
    }

    public ImmutableSet<ARGState> getTargets() {
      return targets;
    }

    public ImmutableSet<ARGState> getBlockTargets() {
      return blockTargets;
    }

    public boolean isEmpty() {
      return targets.isEmpty() && blockTargets.isEmpty();
    }
  }
}
