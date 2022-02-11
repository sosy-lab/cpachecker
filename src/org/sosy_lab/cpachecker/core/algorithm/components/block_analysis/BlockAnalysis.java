// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
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
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.UpdatedTypeMap;
import org.sosy_lab.cpachecker.core.algorithm.components.worker.AnalysisOptions;
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
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
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

  public CompositeState extractCompositeStateFromAbstractState(AbstractState state) {
    checkNotNull(state, "state cannot be null");
    checkState(state instanceof ARGState, "State has to be an ARGState");
    ARGState argState = (ARGState) state;
    checkState(argState.getWrappedState() instanceof CompositeState,
        "First state must contain a CompositeState");
    return (CompositeState) argState.getWrappedState();
  }

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

  public Set<String> visitedBlocks(Collection<Message> pPayloads) {
    Set<String> visitedBlocks = new HashSet<>();
    for (Message message : pPayloads) {
      visitedBlocks.addAll(
          Splitter.on(",").splitToList(message.getPayload().getOrDefault(Payload.VISITED, "")));
    }
    visitedBlocks.remove("");
    visitedBlocks.add(block.getId());
    return visitedBlocks;
  }

  public abstract Collection<Message> analyze(Collection<Message> messages)
      throws CPAException, InterruptedException, SolverException;

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

  public static class ForwardAnalysis extends BlockAnalysis {

    private final BlockTransferRelation relation;
    private boolean reportedOriginalViolation;

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
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, AnalysisDirection.FORWARD, pSpecification,
          pConfiguration,
          pShutdownManager, pOptions);
      relation =
          (BlockTransferRelation) Objects.requireNonNull(CPAs.retrieveCPA(cpa, BlockCPA.class))
              .getTransferRelation();
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
      if (!violations.isEmpty()) {
        // we only need to report error locations once
        // since every new report of an already found location would only cause redundant work
        reportedOriginalViolation = true;
        answers.addAll(createErrorConditionMessages(violations));
      }
      // }

      Set<ARGState> blockEntries = extractBlockTargetStates(targetStates);
      answers.addAll(createBlockPostConditionMessage(messages, blockEntries));
      // find all states with location at the end, make formula
      return answers;
    }

    private Collection<Message> createBlockPostConditionMessage(
        Collection<Message> messages,
        Set<ARGState> blockEntries)
        throws CPAException, InterruptedException {
      List<AbstractState> compositeStates =
          blockEntries.stream().map(this::extractCompositeStateFromAbstractState).collect(
              ImmutableList.toImmutableList());
      Set<Message> answers = new HashSet<>();
      if (!compositeStates.isEmpty()) {
        AbstractState combined = distributedCompositeCPA.combine(compositeStates);
        Payload result = distributedCompositeCPA.serialize(combined);
        result = Payload.builder().putAll(result).addEntry(Payload.STATUS,
                status.wasPropertyChecked() + "," + status.isSound() + "," + status.isPrecise())
            .build();
        Message response =
            Message.newBlockPostCondition(block.getId(), block.getLastNode().getNodeNumber(),
                result, messages.size() == block.getPredecessors().size() && messages.stream()
                    .allMatch(m -> Boolean.parseBoolean(m.getPayload().get(Payload.FULL_PATH))),
                visitedBlocks(messages));
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
        initial = Payload.builder().putAll(initial).addEntry(Payload.STATUS,
                status.wasPropertyChecked() + "," + status.isSound() + "," + status.isPrecise())
            .build();
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
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
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
        logger.log(Level.SEVERE, "Cannot reach block start?", reachedSet);
        return ImmutableSet.of(Message.newErrorConditionUnreachableMessage(block.getId(),
            "backwards analysis cannot reach target at block entry"));
      }
      Payload payload = distributedCompositeCPA.serialize(distributedCompositeCPA.combine(states));
      payload = Payload.builder().putAll(payload).addEntry(Payload.STATUS,
          status.wasPropertyChecked() + "," + status.isSound() + "," + status.isPrecise()).build();
      return ImmutableSet.of(
          Message.newErrorConditionMessage(block.getId(), block.getStartNode().getNodeNumber(),
              payload, false,
              visitedBlocks(messages)));
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
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, pDirection, pSpecification, pConfiguration,
          pShutdownManager, pOptions);
    }

    @Override
    public Collection<Message> analyze(
        Collection<Message> condition)
        throws CPAException, InterruptedException, SolverException {
      return ImmutableSet.of();
    }
  }
}
