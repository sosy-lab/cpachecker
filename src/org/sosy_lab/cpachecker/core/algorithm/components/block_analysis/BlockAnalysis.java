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
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.block.BlockStartReachedTargetInformation;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockTransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.SolverException;

public abstract class BlockAnalysis {

  protected final Algorithm algorithm;
  protected final ReachedSet reachedSet;
  protected final ConfigurableProgramAnalysis cpa;
  protected final DistributedCompositeCPA distributedCompositeCPA;

  protected final Precision emptyPrecision;

  protected final BlockNode block;
  protected final LogManager logger;

  protected AlgorithmStatus status;

  public BlockAnalysis(
      String pId,
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      SSAMap pTypeMap,
      AnalysisDirection pDirection,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
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

    emptyPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
    status = AlgorithmStatus.NO_PROPERTY_CHECKED;

    block = pBlock;
    logger = pLogger;

    distributedCompositeCPA =
        new DistributedCompositeCPA(pId, block, pTypeMap, emptyPrecision, pDirection);
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

  public List<AbstractState> extractBlockEntryPoints(
      ReachedSet pReachedSet,
      CFANode targetNode,
      AbstractState startState) {
    List<AbstractState> compositeStates = new ArrayList<>();
    for (AbstractState abstractState : pReachedSet) {
      if (abstractState.equals(startState)) {
        continue;
      }
      Optional<CFANode> optionalLocation = abstractStateToLocation(abstractState);
      if (optionalLocation.isPresent() && optionalLocation.orElseThrow().equals(targetNode)) {
        compositeStates.add(extractCompositeStateFromAbstractState(abstractState));
      }
    }
    return compositeStates;
  }

  public DistributedCompositeCPA getDistributedCPA() {
    return distributedCompositeCPA;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public Precision getEmptyPrecision() {
    return emptyPrecision;
  }

  public AlgorithmStatus getStatus() {
    return status;
  }

  public Set<String> visitedBlocks(Collection<Message> pPayloads) {
    Set<String> visitedBlocks = new HashSet<>();
    for (Message message : pPayloads) {
      visitedBlocks.addAll(Splitter.on(",").splitToList(message.getPayload().getOrDefault(Payload.VISITED, "")));
    }
    visitedBlocks.remove("");
    visitedBlocks.add(block.getId());
    return visitedBlocks;
  }

  public abstract Collection<Message> analyze(Collection<Message> messages)
      throws CPAException, InterruptedException, SolverException;

  public static class ForwardAnalysis extends BlockAnalysis {

    private final BlockTransferRelation relation;
    private boolean reportedOriginalViolation;

    public ForwardAnalysis(
        String pId,
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        SSAMap pTypeMap,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, AnalysisDirection.FORWARD, pSpecification,
          pConfiguration,
          pShutdownManager);
      relation =
          (BlockTransferRelation) CPAs.retrieveCPA(cpa, BlockCPA.class).getTransferRelation();
    }

    @Override
    public Collection<Message> analyze(Collection<Message> messages)
        throws CPAException, InterruptedException {
      relation.init(block);
      reachedSet.clear();
      AbstractState startState = getStartState(messages);
      reachedSet.add(startState, emptyPrecision);
      status = algorithm.run(reachedSet);
      Set<ARGState> targetStates = from(reachedSet).filter(AbstractStates::isTargetState)
          .filter(ARGState.class).copyInto(new HashSet<>());
      /*if (targetStates.isEmpty()) {
        throw new AssertionError("At least one target state has to exist (block start)");
      }*/

      // find violations for potential backward analysis
      Set<Message> answers = new HashSet<>();
      if (!reportedOriginalViolation) {
        for (ARGState targetState : targetStates) {
          int startInfos = from(targetState.getTargetInformation()).filter(
              BlockStartReachedTargetInformation.class).size();
          if (targetState.getTargetInformation().size() > startInfos) {
            Optional<CFANode> targetNode =
                abstractStateToLocation(targetState);
            if (targetNode.isEmpty()) {
              throw new AssertionError(
                  "States need to have a location but this one does not:" + targetState);
            }
            reportedOriginalViolation = true;
            Payload initial = distributedCompositeCPA.serialize(
                distributedCompositeCPA.getInitialState(targetNode.orElseThrow(),
                    StateSpacePartition.getDefaultPartition()));
            answers.add(Message.newErrorConditionMessage(block.getId(),
                targetNode.orElseThrow().getNodeNumber(), initial, true,
                ImmutableSet.of(block.getId())));
            break;
          }
        }
      }

      // find all states with location at the end, make formula
      List<AbstractState> compositeStates =
          extractBlockEntryPoints(reachedSet, block.getLastNode(), startState);
      if (!compositeStates.isEmpty()) {
        AbstractState combined = distributedCompositeCPA.combine(compositeStates);
        Payload result = distributedCompositeCPA.serialize(combined);
        Message response =
            Message.newBlockPostCondition(block.getId(), block.getLastNode().getNodeNumber(),
                result, messages.size() == block.getPredecessors().size() && messages.stream()
                    .allMatch(m -> Boolean.parseBoolean(m.getPayload().get(Payload.FULL_PATH))),
                visitedBlocks(messages));
        answers.add(response);
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
        SSAMap pTypeMap,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, AnalysisDirection.BACKWARD, pSpecification,
          pConfiguration,
          pShutdownManager);
      relation = (BlockTransferRelation) CPAs.retrieveCPA(cpa, BlockCPABackward.class)
          .getTransferRelation();
    }

    @Override
    public Collection<Message> analyze(Collection<Message> messages)
        throws CPAException, InterruptedException, SolverException {
      relation.init(block);
      reachedSet.clear();
      AbstractState startState = getStartState(messages);
      reachedSet.add(startState, emptyPrecision);
      status = algorithm.run(reachedSet);
      List<AbstractState>
          states = extractBlockEntryPoints(reachedSet, block.getStartNode(), startState);
      if (states.isEmpty()) {
        // should not happen
        logger.log(Level.SEVERE, "Cannot reach block start?", reachedSet);
        return ImmutableSet.of(Message.newErrorConditionUnreachableMessage(block.getId()));
      }
      return ImmutableSet.of(
          Message.newErrorConditionMessage(block.getId(), block.getStartNode().getNodeNumber(),
              distributedCompositeCPA.serialize(distributedCompositeCPA.combine(states)), false,
              visitedBlocks(messages)));
    }
  }

  public static class NoopAnalysis extends BlockAnalysis {

    public NoopAnalysis(
        String pId,
        LogManager pLogger,
        BlockNode pBlock,
        CFA pCFA,
        SSAMap pTypeMap,
        AnalysisDirection pDirection,
        Specification pSpecification,
        Configuration pConfiguration,
        ShutdownManager pShutdownManager)
        throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
      super(pId, pLogger, pBlock, pCFA, pTypeMap, pDirection, pSpecification, pConfiguration,
          pShutdownManager);
    }

    @Override
    public Collection<Message> analyze(
        Collection<Message> condition)
        throws CPAException, InterruptedException, SolverException {
      return ImmutableSet.of();
    }
  }
}
