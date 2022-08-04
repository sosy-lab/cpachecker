// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAHandler;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockEntryReachedTargetInformation;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;

public abstract class BlockAnalysis implements BlockAnalyzer {

  private final Algorithm algorithm;
  private final ReachedSet reachedSet;
  private final DistributedCompositeCPA distributedCompositeCPA;

  private final Precision initialPrecision;
  private final AbstractState top;
  private final ConfigurableProgramAnalysis cpa;

  private final BlockNode block;

  private AlgorithmStatus status;

  /**
   * Analyze a subgraph of the CFA (block node) with an arbitrary CPA.
   *
   * @param pLogger logger to log information
   * @param pBlock coherent subgraph of the CFA
   * @param pCFA CFA where the subgraph pBlock is built from
   * @param pDirection analysis direction (forward or backward)
   * @param pSpecification the specification that the analysis should prove correct/wrong
   * @param pConfiguration user defined configurations
   * @param pShutdownManager shutdown manager for unexpected shutdown requests
   * @param pOptions user defined options for block analyses
   * @throws CPAException if the misbehaviour should be logged instead of causing a crash
   * @throws InterruptedException if the analysis is interrupted by the user
   * @throws InvalidConfigurationException if the configurations contain wrong values
   */
  public BlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      AnalysisDirection pDirection,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      BlockSummaryAnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.getFirst();
    cpa = parts.getSecond();
    reachedSet = parts.getThird();

    status = AlgorithmStatus.SOUND_AND_PRECISE;

    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    initialPrecision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));

    block = pBlock;

    DCPAHandler builder = new DCPAHandler(pOptions);
    CompositeCPA compositeCPA =
        CPAs.retrieveCPAOrFail(cpa, CompositeCPA.class, BlockAnalysis.class);
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      builder.registerDCPA(wrappedCPA, block, pDirection);
    }
    distributedCompositeCPA =
        new DistributedCompositeCPA(
            compositeCPA, block, pDirection, builder.getRegisteredAnalyses());

    top =
        cpa.getInitialState(
            pDirection == AnalysisDirection.FORWARD ? block.getStartNode() : block.getLastNode(),
            StateSpacePartition.getDefaultPartition());
  }

  Optional<CFANode> abstractStateToLocation(AbstractState state) {
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
   * Calculate the first state based on a collection of messages
   *
   * @param receivedPostConditions all messages on a block exit or entry point
   * @return the initial abstract state for the waitlist
   * @throws InterruptedException thread interrupted
   * @throws CPAException wrapper exception
   */
  ARGState getStartState(Collection<BlockSummaryMessage> receivedPostConditions)
      throws InterruptedException, CPAException {
    List<AbstractState> states = new ArrayList<>();
    for (BlockSummaryMessage receivedPostCondition : receivedPostConditions) {
      states.add(
          distributedCompositeCPA.getDeserializeOperator().deserialize(receivedPostCondition));
    }
    return new ARGState(
        Iterables.getOnlyElement(
            distributedCompositeCPA.getCombineOperator().combine(states, top, initialPrecision)),
        null);
  }

  public DistributedCompositeCPA getDistributedCPA() {
    return distributedCompositeCPA;
  }

  /**
   * Find all blocks from which this message contains information
   *
   * @param pMessages all messages at block entry or exit
   * @return visited block ids as set of strings
   */
  ImmutableSet<String> visitedBlocks(Collection<BlockSummaryMessage> pMessages) {
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
    visitedBlocks.add(block.getId());
    return visitedBlocks.build();
  }

  /**
   * Analyze the code block until all target states in this block are found. Block entry points
   * (initial and final location) are target states, too.
   *
   * @param startState initial state from a message
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  ImmutableSet<ARGState> findReachableTargetStatesInBlock(AbstractState startState)
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(startState, initialPrecision);

    // find all target states in block, except target states that are only reachable from another
    // target state
    while (reachedSet.hasWaitingState()) {
      status = status.update(algorithm.run(reachedSet));
      AbstractStates.getTargetStates(reachedSet).forEach(reachedSet::removeOnlyFromWaitlist);
    }

    ImmutableSet<ARGState> targets =
        from(reachedSet)
            .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .filter(AbstractStates::isTargetState)
            .filter(s -> !startState.equals(s))
            .toSet();
    /*    if (targets.isEmpty()) {
      throw new AssertionError(
          "Targets cannot be empty since the entry and exit nodes of each block are target"
              + " locations");
    }*/
    return targets;
  }

  /**
   * Find all error locations in a set of target states
   *
   * @param targetStates abstract states with target information
   * @return subset of targetStates where the target information equals {@link
   *     BlockEntryReachedTargetInformation}
   */
  ImmutableSet<ARGState> extractBlockTargetStates(Set<ARGState> targetStates) {
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
  ImmutableSet<ARGState> extractViolations(Set<ARGState> targetStates) {
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

  BlockSummaryMessagePayload appendStatus(
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

  ReachedSet getReachedSet() {
    return reachedSet;
  }

  AbstractState getTop() {
    return top;
  }

  Precision getInitialPrecision() {
    return initialPrecision;
  }

  AlgorithmStatus getStatus() {
    return status;
  }

  DistributedCompositeCPA getDistributedCompositeCPA() {
    return distributedCompositeCPA;
  }

  ConfigurableProgramAnalysis getCPA() {
    return cpa;
  }
}
