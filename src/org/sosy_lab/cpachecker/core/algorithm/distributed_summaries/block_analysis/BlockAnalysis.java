// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.StatusObserver.StatusPrecise;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.StatusObserver.StatusPropertyChecked;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer.StatusObserver.StatusSoundness;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.AnalysisOptions;
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

  protected final BlockNode block;
  protected final LogManager logger;

  protected final boolean containsLoops;

  protected AlgorithmStatus status;

  public BlockAnalysis(
      String pId,
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      UpdatedTypeMap pTypeMap,
      AnalysisDirection pDirection,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      AnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> parts =
        AlgorithmFactory.createAlgorithm(pLogger, pSpecification, pCFA, pConfiguration,
            pShutdownManager,
            ImmutableSet.of(
                "analysis.algorithm.configurableComponents",
                "analysis.useLoopStructure",
                "cpa.predicate.blk.alwaysAtJoin",
                "cpa.predicate.blk.alwaysAtBranch",
                "cpa.predicate.blk.alwaysAtProgramExit"), pBlock);
    algorithm = parts.getFirst();
    cpa = parts.getSecond();
    reachedSet = parts.getThird();

    status = AlgorithmStatus.SOUND_AND_PRECISE;

    assert reachedSet != null : "BlockAnalysis requires the initial reachedSet";
    initialPrecision = reachedSet.getPrecision(Objects.requireNonNull(reachedSet.getFirstState()));

    block = pBlock;
    logger = pLogger;

    distributedCompositeCPA =
        new DistributedCompositeCPA(pId, block, pTypeMap, initialPrecision, pDirection, pOptions);
    distributedCompositeCPA.setParentCPA(CPAs.retrieveCPA(cpa, CompositeCPA.class));
    containsLoops = pCFA.getAllLoopHeads().isPresent() || pOptions.sendEveryErrorMessage();
  }

  public Optional<CFANode> abstractStateToLocation(AbstractState state) {
    if (state instanceof LocationState) {
      return Optional.of(((LocationState) state).getLocationNode());
    }
    if (state instanceof BlockState) {
      return Optional.of(((BlockState) state).getLocationNode());
    }
    if (state instanceof CompositeState) {
      for (AbstractState wrappedState : ((CompositeState) state).getWrappedStates()) {
        Optional<CFANode> maybeNode = abstractStateToLocation(wrappedState);
        if (maybeNode.isPresent()) {
          return maybeNode;
        }
      }
    }
    if (state.getClass().equals(ARGState.class)) {
      return abstractStateToLocation(((ARGState) state).getWrappedState());
    }
    return Optional.empty();
  }

  /**
   * Returns the wrapped composite state of an ARGState
   * @param pARGState an ARG state
   * @return the wrapped composite state, if it does not wrap an ARG state throw an error.
   */
  public CompositeState extractCompositeStateFromAbstractState(ARGState pARGState) {
    checkNotNull(pARGState, "state cannot be null");
    checkState(pARGState.getWrappedState() instanceof CompositeState,
        "First state must contain a CompositeState");
    return (CompositeState) pARGState.getWrappedState();
  }

  /**
   * Calculate the first state based on a collection of messages
   * @param receivedPostConditions all messages on a block exit or entry point
   * @return the initial abstract state for the waitlist
   * @throws InterruptedException thread interrupted
   * @throws CPAException wrapper exception
   */
  protected ARGState getStartState(Collection<Message> receivedPostConditions)
      throws InterruptedException, CPAException {
    List<AbstractState> states = new ArrayList<>();
    for (Message receivedPostCondition : receivedPostConditions) {
      states.add(distributedCompositeCPA.deserialize(receivedPostCondition));
    }
    return new ARGState(distributedCompositeCPA.combine(states), null);
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
   * @param pMessages all messages at block entry or exit
   * @return visited block ids as set of strings
   */
  public Set<String> visitedBlocks(Collection<Message> pMessages) {
    Set<String> visitedBlocks = new HashSet<>();
    for (Message message : pMessages) {
      visitedBlocks.addAll(
          Splitter.on(",").splitToList(message.getPayload().getOrDefault(Payload.VISITED, "")));
    }
    visitedBlocks.remove("");
    visitedBlocks.add(block.getId());
    return visitedBlocks;
  }

  public abstract Collection<Message> analyze(Collection<Message> messages)
      throws CPAException, InterruptedException, SolverException;

  public abstract Collection<Message> initialAnalysis() throws InterruptedException, CPAException;

  /**
   * Analyze the code block until all target states in this block are found.
   * Block entry points (initial and final location are target states, too)
   * @param startState initial state from a message
   * @param relation the block transfer relation (has to be resetted if it contains loops)
   * @return all target states in this code block
   * @throws CPAException wrapper exception
   * @throws InterruptedException thread interrupted
   */
  protected Set<ARGState> findReachableTargetStatesInBlock(
      AbstractState startState,
      BlockTransferRelation relation)
      throws CPAException, InterruptedException {
    relation.init(block);
    reachedSet.clear();
    reachedSet.add(startState, initialPrecision);

    // find all target states in block, except target states that are only reachable from another target state
    while (reachedSet.hasWaitingState()) {
      status = status.update(algorithm.run(reachedSet));
      AbstractStates.getTargetStates(reachedSet).forEach(reachedSet::removeOnlyFromWaitlist);
    }

    return from(reachedSet).filter(AbstractStates::isTargetState)
        .filter(ARGState.class).filter(s -> !startState.equals(s)).copyInto(new HashSet<>());
  }

  /**
   * Find all error locations in a set of target states
   * @param targetStates abstract states with target information
   * @return subset of targetStates where the target information equals {@link BlockEntryReachedTargetInformation}
   */
  protected Set<ARGState> extractBlockTargetStates(Set<ARGState> targetStates) {
    Set<ARGState> blockTargetStates = new HashSet<>();
    for (ARGState targetState : targetStates) {
      for (TargetInformation targetInformation : targetState.getTargetInformation()) {
        if (targetInformation instanceof BlockEntryReachedTargetInformation) {
          blockTargetStates.add(targetState);
          break;
        }
      }
    }
    return blockTargetStates;
  }

  /**
   * Find all error locations in a set of target states
   * @param targetStates abstract states with target information
   * @return subset of targetStates where the target information is some kind of specification violation
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
    return Payload.builder().putAll(pCurrentPayload).addEntry(
            Payload.PROPERTY,
            pStatus.wasPropertyChecked()
            ? StatusPropertyChecked.CHECKED.name()
            : StatusPropertyChecked.UNCHECKED.name())
        .addEntry(
            Payload.SOUND,
            pStatus.isSound()
            ? StatusSoundness.SOUND.name()
            : StatusSoundness.UNSOUND.name())
        .addEntry(
            Payload.PRECISE,
            pStatus.isPrecise()
            ? StatusPrecise.PRECISE.name()
            : StatusPrecise.IMPRECISE.name())
        .build();
  }

  public static class ForwardAnalysis extends BlockAnalysis {

    private final BlockTransferRelation relation;
    private boolean alreadyReportedError;

    public ForwardAnalysis(
        String pId,
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        UpdatedTypeMap pTypeMap,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager,
        AnalysisOptions pOptions)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, AnalysisDirection.FORWARD, pSpecification,
          pConfiguration,
          pShutdownManager, pOptions);
      relation =
          (BlockTransferRelation) Objects.requireNonNull(CPAs.retrieveCPA(cpa, BlockCPA.class))
              .getTransferRelation();
      alreadyReportedError = containsLoops;
    }


    @Override
    public Collection<Message> analyze(Collection<Message> messages)
        throws CPAException, InterruptedException {
      ARGState startState = getStartState(messages);
      Set<ARGState> targetStates = findReachableTargetStatesInBlock(startState, relation);
      if (targetStates.isEmpty()) {
        // if final node is not reachable, do not broadcast anything.
        // in case abstraction is enabled, this might occur since we abstract at block end
        // TODO: Maybe even shutdown workers only listening to this worker??
        return ImmutableSet.of();
      }

      Set<Message> answers = new HashSet<>();
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
      return answers;
    }

    @Override
    public Collection<Message> initialAnalysis() throws InterruptedException, CPAException {
      Message initial = Message.newBlockPostCondition(block.getId(), block.getStartNode().getNodeNumber(), Payload.empty(),
          false, true, ImmutableSet.of());
      Collection<Message> result = analyze(ImmutableSet.of(initial));
      if (result.isEmpty()) {
        // full path = true as no predecessor can ever change unreachability of block exit
        return ImmutableSet.of(Message.newBlockPostCondition(block.getId(), block.getStartNode().getNodeNumber(), Payload.empty(),
            true, false, ImmutableSet.of()));
      }
      return result;
    }

    private Collection<Message> createBlockPostConditionMessage(
        Collection<Message> messages, Set<ARGState> blockEntries)
        throws CPAException, InterruptedException {
      List<AbstractState> compositeStates =
          blockEntries.stream()
              .map(this::extractCompositeStateFromAbstractState)
              .collect(ImmutableList.toImmutableList());
      Set<Message> answers = new HashSet<>();
      if (!compositeStates.isEmpty()) {
        boolean fullPath =
            messages.size() == block.getPredecessors().size()
                && messages.stream()
                    .allMatch(m -> Boolean.parseBoolean(m.getPayload().get(Payload.FULL_PATH)));
        Set<String> visited = visitedBlocks(messages);
        AbstractState combined = distributedCompositeCPA.combine(compositeStates);
        Payload result = distributedCompositeCPA.serialize(combined);
        result = appendStatus(status, result);
        Message response =
            Message.newBlockPostCondition(
                block.getId(),
                block.getLastNode().getNodeNumber(),
                result,
                fullPath,
                true,
                visited);
        distributedCompositeCPA.setLatestOwnPostConditionMessage(response);
        answers.add(response);
      }
      return answers;
    }

    private Collection<Message> createErrorConditionMessages(Set<ARGState> violations)
        throws InterruptedException {
      Set<Message> answers = new HashSet<>();
      for (ARGState targetState : violations) {
        Optional<CFANode> targetNode =
            abstractStateToLocation(targetState);
        if (targetNode.isEmpty()) {
          throw new AssertionError(
              "States need to have a location but this one does not:" + targetState);
        }
        Payload initial = distributedCompositeCPA.serialize(
            distributedCompositeCPA.getInitialState(targetNode.orElseThrow(),
                StateSpacePartition.getDefaultPartition()));
        initial = appendStatus(status, initial);
        answers.add(Message.newErrorConditionMessage(block.getId(),
            targetNode.orElseThrow().getNodeNumber(), initial, true,
            ImmutableSet.of(block.getId())));
      }
      return answers;
    }
  }

  public static class BackwardAnalysis extends BlockAnalysis {

    private final BlockTransferRelation relation;

    public BackwardAnalysis(
        String pId,
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        UpdatedTypeMap pTypeMap,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager, AnalysisOptions pOptions)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, AnalysisDirection.BACKWARD, pSpecification,
          pConfiguration,
          pShutdownManager, pOptions);
      relation = (BlockTransferRelation) Objects.requireNonNull(
              CPAs.retrieveCPA(cpa, BlockCPABackward.class))
          .getTransferRelation();
    }

    @Override
    public Collection<Message> analyze(Collection<Message> messages)
        throws CPAException, InterruptedException, SolverException {
      ARGState startState = getStartState(messages);
      Set<ARGState> targetStates = findReachableTargetStatesInBlock(startState, relation);
      List<AbstractState> states =
          targetStates.stream().map(this::extractCompositeStateFromAbstractState)
              .collect(ImmutableList.toImmutableList());
      if (states.isEmpty()) {
        // should only happen if abstraction is activated
        logger.log(Level.ALL, "Cannot reach block start?", reachedSet);
        return ImmutableSet.of(Message.newErrorConditionUnreachableMessage(block.getId(),
            "backwards analysis cannot reach target at block entry"));
      }
      Set<Message> responses = new HashSet<>();
      for (AbstractState state : states) {
        Payload payload = distributedCompositeCPA.serialize(state);
        payload = appendStatus(status, payload);
        responses.add(Message.newErrorConditionMessage(block.getId(), block.getStartNode().getNodeNumber(),
            payload, false,
            visitedBlocks(messages)));
      }
      return responses;
    }

    @Override
    public Collection<Message> initialAnalysis() throws InterruptedException, CPAException {
      // current approach does not need an initial backward analysis.
      throw new AssertionError("Initial backward analysis is not implemented yet.");
    }
  }

  public static class NoopAnalysis extends BlockAnalysis {

    public NoopAnalysis(
        String pId,
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        UpdatedTypeMap pTypeMap,
        AnalysisDirection pDirection,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager, AnalysisOptions pOptions)
        throws CPAException, InterruptedException, InvalidConfigurationException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, pDirection, pSpecification, pConfiguration,
          pShutdownManager, pOptions);
    }

    @Override
    public Collection<Message> analyze(
        Collection<Message> condition)
        throws CPAException, InterruptedException, SolverException {
      return ImmutableSet.of();
    }

    /**
     * Broadcast one initial message such that successors know that they are connected to the root
     * @return Message containing the T-element of the underlying composite CPA
     * @throws InterruptedException thread interrupted
     * @throws CPAException forwarded exception (wraps internal errors)
     */
    @Override
    public Collection<Message> initialAnalysis() throws InterruptedException, CPAException {
      return ImmutableSet.of(
          Message.newBlockPostCondition(block.getId(), block.getLastNode().getNodeNumber(),
              distributedCompositeCPA.serialize(
                  distributedCompositeCPA.getInitialState(block.getStartNode(),
                      StateSpacePartition.getDefaultPartition())), true, true,
              ImmutableSet.of(block.getId())));
    }
  }
}
