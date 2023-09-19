// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class DCPAAlgorithm {

  private final DistributedConfigurableProgramAnalysis dcpa;
  private final Map<String, BlockSummaryMessage> states;
  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;
  private final AbstractState startState;
  private final Set<String> predecessors;
  private final Set<String> loopPredecessors;

  // forward analysis variables
  private AlgorithmStatus status;
  private boolean alreadyReportedInfeasibility;
  private Precision blockStartPrecision;

  public DCPAAlgorithm(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    alreadyReportedInfeasibility = false;
    AnalysisComponents parts =
        DCPAAlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    // prepare dcpa and the algorithms
    status = AlgorithmStatus.SOUND_AND_PRECISE;
    algorithm = parts.algorithm();
    cpa = parts.cpa();
    block = pBlock;
    dcpa =
        DCPAFactory.distribute(
            cpa, pBlock, pCFA, pConfiguration, pLogger, pShutdownManager.getNotifier());
    // prepare reached set and initial elements
    reachedSet = parts.reached();
    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    reachedSet.clear();
    assert dcpa != null : "Distribution of " + cpa.getClass().getSimpleName() + " not implemented.";
    blockStartPrecision =
        dcpa.getInitialPrecision(block.getFirst(), StateSpacePartition.getDefaultPartition());
    startState = dcpa.getInitialState(block.getFirst(), StateSpacePartition.getDefaultPartition());

    // handle predecessors
    states = new LinkedHashMap<>();
    predecessors = block.getPredecessorIds();
    loopPredecessors = block.getLoopPredecessorIds();
    // messages of loop predecessors do not matter since they will depend on this block
    loopPredecessors.forEach(id -> states.put(id, null));
  }

  public Collection<BlockSummaryMessage> reportUnreachableBlockEnd() {
    // if sent once, it will never change (precondition is always the most general information)
    if (alreadyReportedInfeasibility) {
      return ImmutableSet.of();
    }
    alreadyReportedInfeasibility = true;
    return ImmutableSet.of(
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getLast().getNodeNumber(),
            DCPAAlgorithms.appendStatus(
                AlgorithmStatus.SOUND_AND_PRECISE, BlockSummaryMessagePayload.empty()),
            false));
  }

  private Collection<BlockSummaryMessage> reportBlockPostConditions(
      Set<ARGState> blockEnds, boolean allowTop) {
    ImmutableSet.Builder<BlockSummaryMessage> messages = ImmutableSet.builder();
    for (ARGState blockEndState : blockEnds) {
      if (!dcpa.isTop(blockEndState) || allowTop) {
        BlockSummaryMessagePayload serialized =
            dcpa.serialize(blockEndState, reachedSet.getPrecision(blockEndState));
        messages.add(
            BlockSummaryMessage.newBlockPostCondition(
                block.getId(),
                block.getLast().getNodeNumber(),
                DCPAAlgorithms.appendStatus(status, serialized),
                true));
      }
    }
    return messages.build();
  }

  private Collection<BlockSummaryMessage> reportErrorConditions(
      Set<ARGState> violations, Set<ARGState> blockEnds, ARGState condition, boolean first)
      throws CPAException, InterruptedException, SolverException {
    ImmutableSet<@NonNull ARGPath> pathsToViolations;
    if (!violations.isEmpty()
        && reachedSet.stream()
            .filter(AbstractStates::isTargetState)
            .allMatch(a -> ((ARGState) a).getCounterexampleInformation().isEmpty())) {
      pathsToViolations =
          FluentIterable.from(blockEnds)
              .transformAndConcat(v -> ARGUtils.getAllPaths(reachedSet, v))
              .toSet();
      if (pathsToViolations.size() > 5
          || block.getPredecessorIds().stream().anyMatch(id -> id.equals("root"))) {
        throw new CPAException(
            "Abstraction state did not contain a counterexample, fallback produced "
                + pathsToViolations.size()
                + " (exceeds the limit of 5) violations.");
      }
    } else {
      pathsToViolations =
          FluentIterable.from(violations)
              .filter(v -> v.getCounterexampleInformation().isPresent())
              .transform(v -> v.getCounterexampleInformation().orElseThrow().getTargetPath())
              .toSet();
    }
    ImmutableSet.Builder<BlockSummaryMessage> messages = ImmutableSet.builder();
    boolean makeFirst = false;
    for (ARGPath path : pathsToViolations) {
      AbstractState abstractState;
      try {
        abstractState = dcpa.computeVerificationCondition(path, condition);
      } catch (VerificationConditionException e) {
        // see semantics of VerificationConditionException
        continue;
      }
      BlockSummaryMessagePayload serialized =
          dcpa.serialize(abstractState, reachedSet.getPrecision(path.getLastState()));
      messages.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(),
              block.getFirst().getNodeNumber(),
              DCPAAlgorithms.appendStatus(status, serialized),
              first || makeFirst));
      makeFirst = true;
    }
    return messages.build();
  }

  public Collection<BlockSummaryMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(startState, blockStartPrecision);

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    if (result.getViolationStates().isEmpty()) {
      if (result.getBlockEndStates().isEmpty()) {
        return reportUnreachableBlockEnd();
      }
      return reportBlockPostConditions(result.getBlockEndStates(), true);
    }

    return reportErrorConditions(result.getViolationStates(), ImmutableSet.of(), null, true);
  }

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @param pReceived Current message to process
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<BlockSummaryMessage> runAnalysis(BlockSummaryPostConditionMessage pReceived)
      throws SolverException, InterruptedException {
    // check if message is meant for this block
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pReceived);
    blockStartPrecision = dcpa.getDeserializePrecisionOperator().deserializePrecision(pReceived);
    BlockSummaryMessageProcessing processing =
        dcpa.getProceedOperator().proceedForward(deserialized);
    if (processing.end()) {
      if (predecessors.contains(pReceived.getBlockId())) {
        // null means that we cannot expect a state from this predecessor
        states.put(pReceived.getBlockId(), null);
      }
      return processing;
    }
    assert processing.isEmpty() : "Proceed is not possible with unprocessed messages";
    assert predecessors.contains(pReceived.getBlockId())
        : "Proceed failed to recognize that this message is not meant for this block.";
    // TODO: this should somehow be checked by ProceedBlockStateOperator,
    //  but it has no access to this attribute.
    if (pReceived.isReachable()) {
      // reset all loop predecessors if non-loop predecessor updates
      if (!loopPredecessors.isEmpty() && !loopPredecessors.contains(pReceived.getBlockId())) {
        loopPredecessors.forEach(id -> states.put(id, null));
      }
      if (loopPredecessors.contains(pReceived.getBlockId()) && dcpa.isTop(deserialized)) {
        states.put(pReceived.getBlockId(), null);
      } else {
        states.put(pReceived.getBlockId(), pReceived);
      }
    } else {
      // null means that we cannot expect a state from this predecessor, i.e.,
      // we do not under-approximate when ignoring this predecessor.
      states.put(pReceived.getBlockId(), null);
    }
    // if we do not have messages from all predecessors we under-approximate, so we abort!
    // if element is top element, we abort
    // for now we do not analyze at all
    return ImmutableSet.of();
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param pErrorCondition a message containing an abstract state representing an error condition
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   */
  public Collection<BlockSummaryMessage> runAnalysisUnderCondition(
      BlockSummaryErrorConditionMessage pErrorCondition)
      throws CPAException, InterruptedException, SolverException {
    // merge all states into the reached set
    AbstractState errorCondition = dcpa.getDeserializeOperator().deserialize(pErrorCondition);
    BlockSummaryMessageProcessing processing =
        dcpa.getProceedOperator().proceedBackward(errorCondition);
    if (processing.end()) {
      if (predecessors.contains(pErrorCondition.getBlockId())) {
        // null means that we cannot expect a state from this predecessor
        states.put(pErrorCondition.getBlockId(), null);
      }
      return processing;
    }

    final AbstractState translatedErrorCondition =
        dcpa.getDeserializeOperator()
            .deserialize(
                BlockSummaryMessage.newBlockPostCondition(
                    pErrorCondition.getBlockId(),
                    pErrorCondition.getTargetNodeNumber(),
                    getDCPA().getSerializeOperator().serialize(errorCondition),
                    true));

    prepareReachedSet();

    reachedSet.forEach(
        abstractState ->
            Objects.requireNonNull(
                    AbstractStates.extractStateByType(abstractState, BlockState.class))
                .setErrorCondition(translatedErrorCondition));

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    ImmutableSet.Builder<BlockSummaryMessage> messages = ImmutableSet.builder();
    if (result.getBlockEndStates().isEmpty()) {
      messages.add(
          BlockSummaryMessage.newErrorConditionUnreachableMessage(
              block.getId(), "Condition unsatisfiable (no state present on block end)"));
    } else if (block.isAbstractionPossible() && result.getAbstractionStates().isEmpty()) {
      messages.addAll(reportBlockPostConditions(result.getBlockEndStates(), false));
      messages.add(
          BlockSummaryMessage.newErrorConditionUnreachableMessage(
              block.getId(), "Condition unsatisfiable (after strengthening)"));
    } else {
      Collection<BlockSummaryMessage> errorConditions =
          reportErrorConditions(
              result.getAbstractionStates(),
              result.getBlockEndStates(),
              ((ARGState) errorCondition),
              false);
      if (errorConditions.isEmpty()) {
        messages.add(
            BlockSummaryMessage.newErrorConditionUnreachableMessage(
                block.getId(),
                "Condition unsatisfiable (not reachable, but error conditions present)"));
      } else {
        messages.addAll(errorConditions);
      }
    }
    return messages.build();
  }

  /**
   * Prepare the reached set for next analysis by merging all received BPC messages into a non-empty
   * set of start states.
   *
   * @throws CPAException thrown in merge or stop operation runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly.
   */
  private void prepareReachedSet() throws CPAException, InterruptedException {
    // simulate merge and stop for all states ending up at block#getStartNode
    reachedSet.clear();
    for (BlockSummaryMessage message : states.values()) {
      if (message == null) {
        continue;
      }
      AbstractState value = dcpa.getDeserializeOperator().deserialize(message);
      if (reachedSet.isEmpty()) {
        reachedSet.add(value, blockStartPrecision);
      } else {
        // CPA algorithm
        for (AbstractState abstractState : ImmutableSet.copyOf(reachedSet)) {
          AbstractState merged =
              cpa.getMergeOperator().merge(value, abstractState, blockStartPrecision);
          if (!merged.equals(abstractState)) {
            reachedSet.remove(abstractState);
            reachedSet.add(merged, blockStartPrecision);
          }
        }
        if (!cpa.getStopOperator()
            .stop(value, reachedSet.getReached(block.getFirst()), blockStartPrecision)) {
          reachedSet.add(value, blockStartPrecision);
        }
      }
    }

    if (reachedSet.isEmpty()) {
      reachedSet.add(startState, blockStartPrecision);
    }
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
