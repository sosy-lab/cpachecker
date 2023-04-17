// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.AlgorithmFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms.BlockAndLocation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DCPAFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
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
  private final Map<BlockAndLocation, AbstractState> abstractionStates;

  // forward analysis variables
  private AlgorithmStatus status;
  private boolean alreadyReportedError;
  private boolean isInfeasible;
  private boolean alreadyReportedInfeasibility;
  private Precision blockStartPrecision;
  private Precision blockEndPrecision;
  private boolean hasSentFirstMessages;

  public DCPAAlgorithm(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    alreadyReportedError = false;
    alreadyReportedInfeasibility = false;
    isInfeasible = false;
    AnalysisComponents parts =
        AlgorithmFactory.createAlgorithm(
            pLogger, pSpecification, pCFA, pConfiguration, pShutdownManager, pBlock);
    // prepare dcpa and the algorithms
    status = AlgorithmStatus.SOUND_AND_PRECISE;
    algorithm = parts.algorithm();
    cpa = parts.cpa();
    block = pBlock;
    dcpa = DCPAFactory.distribute(cpa, pBlock, AnalysisDirection.FORWARD, pCFA);
    // prepare reached set and initial elements
    reachedSet = parts.reached();
    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    reachedSet.clear();
    blockStartPrecision =
        dcpa.getInitialPrecision(block.getFirst(), StateSpacePartition.getDefaultPartition());
    blockEndPrecision = blockStartPrecision;
    startState = dcpa.getInitialState(block.getFirst(), StateSpacePartition.getDefaultPartition());

    // handle predecessors
    states = new LinkedHashMap<>();
    predecessors = block.getPredecessorIds();
    loopPredecessors = block.getLoopPredecessorIds();
    // messages of loop predecessors do not matter since they will depend on this block
    loopPredecessors.forEach(id -> states.put(id, null));
    abstractionStates = new LinkedHashMap<>();
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

  public Collection<BlockSummaryMessage> runInitialAnalysis()
      throws CPAException, InterruptedException {
    reachedSet.clear();
    reachedSet.add(startState, blockStartPrecision);
    Collection<BlockSummaryMessage> results =
        processIntermediateResult(
            DCPAAlgorithms.findReachableTargetStatesInBlock(
                algorithm, reachedSet, block, AnalysisDirection.FORWARD),
            false);
    if (results.isEmpty()) {
      return reportUnreachableBlockEnd();
    }
    return results;
  }

  public void addAbstractionState(BlockSummaryMessage pMessage) throws InterruptedException {
    Preconditions.checkArgument(pMessage.getType() == MessageType.ABSTRACTION_STATE);
    if (pMessage.getTargetNodeNumber() != block.getFirst().getNodeNumber()) {
      return;
    }
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pMessage);
    abstractionStates.put(
        new BlockAndLocation(pMessage.getBlockId(), pMessage.getTargetNodeNumber()), deserialized);
  }

  public Collection<BlockSummaryMessage> runAnalysisForMessage(
      BlockSummaryPostConditionMessage pReceived)
      throws SolverException, InterruptedException, CPAException {
    // check if message is meant for this block
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pReceived);
    blockStartPrecision = dcpa.getDeserializePrecisionOperator().deserializePrecision(pReceived);
    BlockSummaryMessageProcessing processing = dcpa.getProceedOperator().proceed(deserialized);
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
    if (states.size() != predecessors.size()) {
      return ImmutableSet.of();
    }
    // we do not have any error condition for simple forward analyses
    return runAnalysisUnderCondition(Optional.empty());
  }

  /**
   * Runs the CPA under an error condition, i.e., if the current block contains a block-end edge,
   * the error condition will be attached to that edge. In case this makes the path formula
   * infeasible, we compute an abstraction. If no error condition is present, we run the CPA.
   *
   * @param errorCondition an abstract state representing an error condition
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   */
  public Collection<BlockSummaryMessage> runAnalysisUnderCondition(
      Optional<AbstractState> errorCondition) throws CPAException, InterruptedException {
    // merge all states into the reached set
    prepareReachedSet();

    // set error condition to all starting states if present
    errorCondition.ifPresent(
        condition ->
            reachedSet.forEach(
                abstractState ->
                    Objects.requireNonNull(
                            AbstractStates.extractStateByType(abstractState, BlockState.class))
                        .setErrorCondition(errorCondition)));

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(
            algorithm, reachedSet, block, AnalysisDirection.FORWARD);
    result =
        result.refine(
            abstractionStates.values(), dcpa.getAbstractDomain(), dcpa.getAbstractStateClass());
    return processIntermediateResult(result, errorCondition.isPresent());
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
        for (AbstractState abstractState : reachedSet) {
          AbstractState merged =
              cpa.getMergeOperator().merge(value, abstractState, blockStartPrecision);
          if (!merged.equals(abstractState)) {
            reachedSet.add(merged, blockStartPrecision);
            reachedSet.remove(abstractState);
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

  private Collection<BlockSummaryMessage> processIntermediateResult(
      BlockAnalysisIntermediateResult result, boolean filter) throws InterruptedException {
    // adapt precision
    isInfeasible = result.wasAbstracted();
    status = status.update(result.getStatus());
    assert reachedSet.getFirstState() != null;
    if (reachedSet.getLastState() != null) {
      blockStartPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
      blockEndPrecision = reachedSet.getPrecision(reachedSet.getLastState());
    }
    // no feasible path to block end ==> block end is unreachable and successors
    // can remove this block from their predecessor set.
    if (result.isEmpty()) {
      return reportUnreachableBlockEnd();
    }
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    // empty block ends imply that there was no abstraction node reached
    assert !result.getBlockEnds().isEmpty() || result.getBlockTargets().isEmpty();
    if (!result.getBlockEnds().isEmpty()) {
      answers
          // serialize all target states (they are for sure abstraction states, necessary for
          // covered by)
          /*.addAll(
          FluentIterable.from(reachedSet.getReached(block.getFirst()))
              .append(reachedSet.getReached(block.getLastNode()))
              .filter(AbstractStates::isTargetState)
              .filter(s -> !startState.equals(s))
              .transform(
                  a ->
                      BlockSummaryMessage.newAbstractionStateMessage(
                          block.getId(),
                          Objects.requireNonNull(AbstractStates.extractLocation(a))
                              .getNodeNumber(),
                          dcpa.getSerializeOperator().serialize(a))))*/
          .addAll(
          // serialize the precision and the states at the block ends
          // (BlockPostConditionMessages)
          FluentIterable.from(result.getBlockEnds())
              .filter(m -> filter || !hasSentFirstMessages || !dcpa.isTop(m))
              .transform(state -> dcpa.serialize(state, reachedSet.getPrecision(state)))
              .transform(
                  p ->
                      BlockSummaryMessage.newBlockPostCondition(
                          block.getId(),
                          block.getLast().getNodeNumber(),
                          DCPAAlgorithms.appendStatus(status, p),
                          true)));
    } else {
      answers.addAll(reportUnreachableBlockEnd());
    }
    // if we encounter an error location for the first time, we notify the same block to start
    // a backwards analysis from there
    if (!result.getViolations().isEmpty() && !alreadyReportedError) {
      alreadyReportedError = true;
      return createErrorConditionMessages(result.getViolations());
    }
    hasSentFirstMessages = true;
    return answers.build();
  }

  private Collection<BlockSummaryMessage> createErrorConditionMessages(Set<ARGState> violations)
      throws InterruptedException {
    ImmutableSet.Builder<BlockSummaryMessage> answers = ImmutableSet.builder();
    for (ARGState targetState : violations) {
      Optional<CFANode> targetNode = DCPAAlgorithms.abstractStateToLocation(targetState);
      if (targetNode.isEmpty()) {
        throw new AssertionError(
            "States need to have a location but this one does not: " + targetState);
      }
      BlockSummaryMessagePayload initial =
          dcpa.getSerializeOperator()
              .serialize(
                  dcpa.getInitialState(
                      targetNode.orElseThrow(), StateSpacePartition.getDefaultPartition()));
      initial = DCPAAlgorithms.appendStatus(status, initial);
      answers.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(), targetNode.orElseThrow().getNodeNumber(), initial, true));
    }
    return answers.build();
  }

  boolean isInfeasible() {
    boolean storage = isInfeasible;
    isInfeasible = false;
    return storage;
  }

  Precision getPrecisionAtBlockEnd() {
    return blockEndPrecision;
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
