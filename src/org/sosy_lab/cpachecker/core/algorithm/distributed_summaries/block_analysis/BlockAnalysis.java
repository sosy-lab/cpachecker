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
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.AnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.ObserverBlockSummaryWorker.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.ObserverBlockSummaryWorker.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.ObserverBlockSummaryWorker.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.block.BlockEntryReachedTargetInformation;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockTransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class BlockAnalysis {

  protected final Algorithm algorithm;
  protected final ReachedSet reachedSet;
  protected final ConfigurableProgramAnalysis cpa;
  protected final DistributedCompositeCPA distributedCompositeCPA;

  protected final Precision initialPrecision;
  protected final AnalysisDirection direction;
  protected final AbstractState top;

  protected final BlockNode block;
  protected final LogManager logger;

  protected final boolean containsLoops;

  protected AlgorithmStatus status;

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
      AnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    algorithm = parts.getFirst();
    cpa = parts.getSecond();
    reachedSet = parts.getThird();
    direction = pDirection;

    status = AlgorithmStatus.SOUND_AND_PRECISE;

    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    initialPrecision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));

    block = pBlock;
    logger = pLogger;

    DCPABuilder builder = new DCPABuilder(pOptions);
    CompositeCPA compositeCPA =
        CPAs.retrieveCPAOrFail(cpa, CompositeCPA.class, BlockAnalysis.class);
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      builder.addCPA(wrappedCPA, block, direction);
    }
    distributedCompositeCPA =
        new DistributedCompositeCPA(compositeCPA, block, pDirection, builder.getAnalyses());
    containsLoops = pCFA.getAllLoopHeads().isPresent() || pOptions.sendEveryErrorMessage();

    top =
        cpa.getInitialState(
            direction == AnalysisDirection.FORWARD ? block.getStartNode() : block.getLastNode(),
            StateSpacePartition.getDefaultPartition());
  }

  protected Optional<CFANode> abstractStateToLocation(AbstractState state) {
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
  protected ARGState getStartState(Collection<ActorMessage> receivedPostConditions)
      throws InterruptedException, CPAException {
    List<AbstractState> states = new ArrayList<>();
    for (ActorMessage receivedPostCondition : receivedPostConditions) {
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

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public Precision getInitialPrecision() {
    return initialPrecision;
  }

  /**
   * Find all blocks from which this message contains information
   *
   * @param pMessages all messages at block entry or exit
   * @return visited block ids as set of strings
   */
  public Set<String> visitedBlocks(Collection<ActorMessage> pMessages) {
    ImmutableSet.Builder<String> visitedBlocks = ImmutableSet.builder();
    for (ActorMessage message : pMessages) {
      Set<String> visited = ImmutableSet.of();
      if (message.getType() == MessageType.BLOCK_POSTCONDITION) {
        visited = ((BlockPostConditionMessage) message).visitedBlockIds();
      } else if (message.getType() == MessageType.ERROR_CONDITION) {
        visited = ((ErrorConditionMessage) message).visitedBlockIds();
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

  public abstract Collection<ActorMessage> analyze(Collection<ActorMessage> messages)
      throws CPAException, InterruptedException, SolverException;

  public abstract Collection<ActorMessage> performInitialAnalysis()
      throws InterruptedException, CPAException;

  /**
   * Analyze the code block until all target states in this block are found. Block entry points
   * (initial and final location) are target states, too.
   *
   * @param startState initial state from a message
   * @param relation the block transfer relation (has to be resetted if it contains loops)
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  protected ImmutableSet<ARGState> findReachableTargetStatesInBlock(
      AbstractState startState, BlockTransferRelation relation)
      throws CPAException, InterruptedException {
    relation.init(block);
    reachedSet.clear();
    reachedSet.add(startState, initialPrecision);

    // find all target states in block, except target states that are only reachable from another
    // target state
    while (reachedSet.hasWaitingState()) {
      status = status.update(algorithm.run(reachedSet));
      AbstractStates.getTargetStates(reachedSet).forEach(reachedSet::removeOnlyFromWaitlist);
    }

    return from(reachedSet)
        .filter(AbstractStates::isTargetState)
        .filter(ARGState.class)
        .filter(s -> !startState.equals(s))
        .toSet();
  }

  /**
   * Find all error locations in a set of target states
   *
   * @param targetStates abstract states with target information
   * @return subset of targetStates where the target information equals {@link
   *     BlockEntryReachedTargetInformation}
   */
  protected ImmutableSet<ARGState> extractBlockTargetStates(Set<ARGState> targetStates) {
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
  protected Set<ARGState> extractViolations(Set<ARGState> targetStates) {
    Set<ARGState> violationStates = new HashSet<>();
    for (ARGState targetState : targetStates) {
      for (TargetInformation targetInformation : targetState.getTargetInformation()) {
        if (!(targetInformation instanceof BlockEntryReachedTargetInformation)) {
          violationStates.add(targetState);
          break;
        }
      }
    }
    return violationStates;
  }

  protected Payload appendStatus(AlgorithmStatus pStatus, Payload pCurrentPayload) {
    return new Payload.Builder()
        .addAllEntries(pCurrentPayload)
        .addEntry(
            Payload.PROPERTY,
            pStatus.wasPropertyChecked()
                ? StatusPropertyChecked.CHECKED.name()
                : StatusPropertyChecked.UNCHECKED.name())
        .addEntry(
            Payload.SOUND,
            pStatus.isSound() ? StatusSoundness.SOUND.name() : StatusSoundness.UNSOUND.name())
        .addEntry(
            Payload.PRECISE,
            pStatus.isPrecise() ? StatusPrecise.PRECISE.name() : StatusPrecise.IMPRECISE.name())
        .buildPayload();
  }

  public static class ForwardAnalysis extends BlockAnalysis {

    private final BlockTransferRelation relation;
    private boolean alreadyReportedError;

    public ForwardAnalysis(
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager,
        AnalysisOptions pOptions)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(
          pLogger,
          pBlock,
          pCFA,
          AnalysisDirection.FORWARD,
          pSpecification,
          pConfiguration,
          pShutdownManager,
          pOptions);
      relation =
          (BlockTransferRelation)
              CPAs.retrieveCPAOrFail(cpa, BlockCPA.class, BlockAnalysis.class)
                  .getTransferRelation();
      alreadyReportedError = containsLoops;
    }

    @Override
    public Collection<ActorMessage> analyze(Collection<ActorMessage> messages)
        throws CPAException, InterruptedException {
      ARGState startState = getStartState(messages);
      Set<ARGState> targetStates = findReachableTargetStatesInBlock(startState, relation);
      if (targetStates.isEmpty()) {
        // if final node is not reachable, do not broadcast anything.
        // in case abstraction is enabled, this might occur since we abstract at block end
        // TODO: Maybe even shutdown workers only listening to this worker??
        return ImmutableSet.of();
      }

      ImmutableSet.Builder<ActorMessage> answers = ImmutableSet.builder();
      // if (!reportedOriginalViolation) {
      Set<ARGState> violations = extractViolations(targetStates);
      if (!violations.isEmpty() && (!alreadyReportedError || containsLoops)) {
        // we only need to report error locations once
        // since every new report of an already found location would only cause redundant work
        answers.addAll(createErrorConditionMessages(violations));
        alreadyReportedError = true;
      }
      // }

      Set<ARGState> blockEntries = extractBlockTargetStates(targetStates);
      answers.addAll(createBlockPostConditionMessage(messages, blockEntries));
      // find all states with location at the end, make formula
      return answers.build();
    }

    @Override
    public Collection<ActorMessage> performInitialAnalysis()
        throws InterruptedException, CPAException {
      ActorMessage initial =
          ActorMessage.newBlockPostCondition(
              block.getId(),
              block.getStartNode().getNodeNumber(),
              Payload.empty(),
              false,
              true,
              ImmutableSet.of());
      Collection<ActorMessage> result = analyze(ImmutableSet.of(initial));
      if (result.isEmpty()) {
        // full path = true as no predecessor can ever change unreachability of block exit
        return ImmutableSet.of(
            ActorMessage.newBlockPostCondition(
                block.getId(),
                block.getStartNode().getNodeNumber(),
                Payload.empty(),
                true,
                false,
                ImmutableSet.of()));
      }
      return result;
    }

    private Collection<ActorMessage> createBlockPostConditionMessage(
        Collection<ActorMessage> messages, Set<ARGState> blockEntries)
        throws CPAException, InterruptedException {
      List<AbstractState> compositeStates =
          transformedImmutableListCopy(
              blockEntries,
              state ->
                  (AbstractState) AbstractStates.extractStateByType(state, CompositeState.class));
      ImmutableSet.Builder<ActorMessage> answers = ImmutableSet.builder();
      if (!compositeStates.isEmpty()) {
        boolean fullPath =
            messages.size() == block.getPredecessors().size()
                && messages.stream()
                    .allMatch(m -> ((BlockPostConditionMessage) m).representsFullPath());
        Set<String> visited = visitedBlocks(messages);
        AbstractState combined =
            Iterables.getOnlyElement(
                distributedCompositeCPA
                    .getCombineOperator()
                    .combine(compositeStates, top, initialPrecision));
        Payload result = distributedCompositeCPA.getSerializeOperator().serialize(combined);
        result = appendStatus(status, result);
        BlockPostConditionMessage response =
            (BlockPostConditionMessage)
                ActorMessage.newBlockPostCondition(
                    block.getId(),
                    block.getLastNode().getNodeNumber(),
                    result,
                    fullPath,
                    true,
                    visited);
        distributedCompositeCPA.getProceedOperator().update(response);
        answers.add(response);
      }
      return answers.build();
    }

    private Collection<ActorMessage> createErrorConditionMessages(Set<ARGState> violations)
        throws InterruptedException {
      ImmutableSet.Builder<ActorMessage> answers = ImmutableSet.builder();
      for (ARGState targetState : violations) {
        Optional<CFANode> targetNode = abstractStateToLocation(targetState);
        if (targetNode.isEmpty()) {
          throw new AssertionError(
              "States need to have a location but this one does not:" + targetState);
        }
        Payload initial =
            distributedCompositeCPA
                .getSerializeOperator()
                .serialize(
                    distributedCompositeCPA.getInitialState(
                        targetNode.orElseThrow(), StateSpacePartition.getDefaultPartition()));
        initial = appendStatus(status, initial);
        answers.add(
            ActorMessage.newErrorConditionMessage(
                block.getId(),
                targetNode.orElseThrow().getNodeNumber(),
                initial,
                true,
                ImmutableSet.of(block.getId())));
      }
      return answers.build();
    }
  }

  public static class BackwardAnalysis extends BlockAnalysis {

    private final BlockTransferRelation relation;

    public BackwardAnalysis(
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager,
        AnalysisOptions pOptions)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(
          pLogger,
          pBlock,
          pCFA,
          AnalysisDirection.BACKWARD,
          pSpecification,
          pConfiguration,
          pShutdownManager,
          pOptions);
      relation =
          (BlockTransferRelation)
              Objects.requireNonNull(CPAs.retrieveCPA(cpa, BlockCPABackward.class))
                  .getTransferRelation();
    }

    @Override
    public Collection<ActorMessage> analyze(Collection<ActorMessage> messages)
        throws CPAException, InterruptedException, SolverException {
      ARGState startState = getStartState(messages);
      Set<ARGState> targetStates = findReachableTargetStatesInBlock(startState, relation);
      List<AbstractState> states =
          transformedImmutableListCopy(
              targetStates,
              state -> AbstractStates.extractStateByType(state, CompositeState.class));
      if (states.isEmpty()) {
        // should only happen if abstraction is activated
        logger.log(Level.ALL, "Cannot reach block start?", reachedSet);
        return ImmutableSet.of(
            ActorMessage.newErrorConditionUnreachableMessage(
                block.getId(), "backwards analysis cannot reach target at block entry"));
      }
      ImmutableSet.Builder<ActorMessage> responses = ImmutableSet.builder();
      for (AbstractState state : states) {
        Payload payload = distributedCompositeCPA.getSerializeOperator().serialize(state);
        payload = appendStatus(status, payload);
        responses.add(
            ActorMessage.newErrorConditionMessage(
                block.getId(),
                block.getStartNode().getNodeNumber(),
                payload,
                false,
                visitedBlocks(messages)));
      }
      return responses.build();
    }

    @Override
    public Collection<ActorMessage> performInitialAnalysis()
        throws InterruptedException, CPAException {
      // current approach does not need an initial backward analysis.
      throw new AssertionError("Initial backward analysis is not implemented yet.");
    }
  }

  public static class NoopAnalysis extends BlockAnalysis {

    public NoopAnalysis(
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        AnalysisDirection pDirection,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager,
        AnalysisOptions pOptions)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(
          pLogger,
          pBlock,
          pCFA,
          pDirection,
          pSpecification,
          pConfiguration,
          pShutdownManager,
          pOptions);
    }

    @Override
    public Collection<ActorMessage> analyze(Collection<ActorMessage> condition)
        throws CPAException, InterruptedException, SolverException {
      return ImmutableSet.of();
    }

    /**
     * Broadcast one initial message such that successors know that they are connected to the root
     *
     * @return Message containing the T-element of the underlying composite CPA
     * @throws InterruptedException thread interrupted
     * @throws CPAException forwarded exception (wraps internal errors)
     */
    @Override
    public Collection<ActorMessage> performInitialAnalysis()
        throws InterruptedException, CPAException {
      return ImmutableSet.of(
          ActorMessage.newBlockPostCondition(
              block.getId(),
              block.getLastNode().getNodeNumber(),
              distributedCompositeCPA
                  .getSerializeOperator()
                  .serialize(
                      distributedCompositeCPA.getInitialState(
                          block.getStartNode(), StateSpacePartition.getDefaultPartition())),
              true,
              true,
              ImmutableSet.of(block.getId())));
    }
  }
}
