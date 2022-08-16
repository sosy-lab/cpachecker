// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockEntryReachedTargetInformation;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BlockAnalysisUtil {

  /**
   * Calculate the first state based on a collection of messages
   *
   * @param startMessages all messages on a block exit or entry point
   * @return the initial abstract state for the waitlist
   * @throws InterruptedException thread interrupted
   * @throws CPAException wrapper exception
   */
  static ARGState getStartState(CFANode pStart, Precision pPrecision, DistributedConfigurableProgramAnalysis pAnalysis, Collection<BlockSummaryMessage> startMessages)
      throws InterruptedException, CPAException {
    ImmutableList.Builder<AbstractState> states = ImmutableList.builder();
    for (BlockSummaryMessage receivedPostCondition : startMessages) {
      states.add(
          pAnalysis.getDeserializeOperator().deserialize(receivedPostCondition));
    }
    return new ARGState(
        Iterables.getOnlyElement(
            pAnalysis
                .getCombineOperator()
                .combine(
                    states.build(),
                    pAnalysis.getInitialState(pStart, StateSpacePartition.getDefaultPartition()),
                    pPrecision)),
        null);
  }

  /**
   * Find all blocks from which this message contains information
   *
   * @param pMessages all messages at block entry or exit
   * @return visited block ids as set of strings
   */
  static ImmutableSet<String> findVisitedBlocks(Collection<BlockSummaryMessage> pMessages) {
    ImmutableSet.Builder<String> visitedBlocks = ImmutableSet.builder();
    for (BlockSummaryMessage message : pMessages) {
      Set<String> visited = ImmutableSet.of();
      if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
        visited = ((BlockSummaryPostConditionMessage) message).visitedBlockIds();
      } else if (message.getType() == MessageType.ERROR_CONDITION) {
        visited = ((BlockSummaryErrorConditionMessage) message).visitedBlockIds();
      }
      for (String part : visited) {
        if (!part.isBlank()) {
          visitedBlocks.add(part);
        }
      }
    }
    // TODO visitedBlocks.add(block.getId());
    return visitedBlocks.build();
  }

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
    return new BlockSummaryMessagePayload.Builder()
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
      Algorithm pAlgorithm,
      ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    AbstractState startState = pReachedSet.getFirstState();
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    // find all target states in block, except target states that are only reachable from another
    // target state
    while (pReachedSet.hasWaitingState()) {
      status = status.update(pAlgorithm.run(pReachedSet));
      AbstractStates.getTargetStates(pReachedSet).forEach(pReachedSet::removeOnlyFromWaitlist);
    }

    return BlockAnalysisIntermediateResult.of(from(pReachedSet)
        .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
        .filter(AbstractStates::isTargetState)
        .filter(s -> !Objects.equals(startState, s))
        .toSet(), status);
  }

  public static class BlockAnalysisIntermediateResult {

    private final ImmutableSet<ARGState> targets;
    private final ImmutableSet<ARGState> blockTargets;
    private final AlgorithmStatus status;


    private BlockAnalysisIntermediateResult(
        ImmutableSet<ARGState> pTargets,
        AlgorithmStatus pStatus) {
      blockTargets = extractBlockTargetStates(pTargets);
      targets = extractViolations(pTargets);
      status = pStatus;
    }

    public static BlockAnalysisIntermediateResult of(ImmutableSet<ARGState> pTargets, AlgorithmStatus pStatus) {
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
