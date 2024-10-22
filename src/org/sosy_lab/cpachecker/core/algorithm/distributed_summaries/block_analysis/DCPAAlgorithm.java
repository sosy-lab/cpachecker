// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
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
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class DCPAAlgorithm {

  private final DistributedConfigurableProgramAnalysis dcpa;
  // In this map, value 'null' means that the key 'blockId' is a loop predecessor
  // from which we have not seen any summary message yet
  private final Map<String, BlockSummaryMessage> states;
  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;
  private final Set<String> predecessors;
  private final Set<String> loopPredecessors;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final Set<Set<String>> seenPrefixes;
  private final Map<Set<String>, BlockSummaryErrorConditionMessage> errors;
  private final LogManager logger;

  // forward analysis variables
  private AlgorithmStatus status;
  private boolean alreadyReportedInfeasibility;
  private final Set<String> soundPredecessors;

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
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfiguration, pLogger, pCFA.getMachineModel());
    // prepare dcpa and the algorithms
    status = AlgorithmStatus.SOUND_AND_PRECISE;
    algorithm = parts.algorithm();
    cpa = parts.cpa();
    block = pBlock;
    logger = pLogger;
    dcpa =
        DCPAFactory.distribute(
            cpa, pBlock, pCFA, pConfiguration, pLogger, pShutdownManager.getNotifier());
    // prepare reached set and initial elements
    reachedSet = parts.reached();
    checkNotNull(reachedSet, "BlockAnalysis requires the initial reachedSet");
    reachedSet.clear();
    assert dcpa != null : "Distribution of " + cpa.getClass().getSimpleName() + " not implemented.";

    // handle predecessors
    states = new LinkedHashMap<>();
    predecessors = block.getPredecessorIds();
    loopPredecessors = block.getLoopPredecessorIds();
    // messages of loop predecessors do not matter since they will depend on this block
    loopPredecessors.forEach(id -> states.put(id, null));
    errors = new HashMap<>();
    seenPrefixes = new HashSet<>();
    soundPredecessors = new HashSet<>();
  }

  public Collection<BlockSummaryMessage> reportUnreachableBlockEnd() {
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
      Set<ARGState> blockEnds, boolean allowTop) throws InterruptedException {
    ImmutableSet.Builder<BlockSummaryMessage> messages = ImmutableSet.builder();
    if (blockEnds.size() == 1) {
      ARGState blockEndState = Iterables.getOnlyElement(blockEnds);
      PredicateAbstractState predicateState =
          AbstractStates.extractStateByType(blockEndState, PredicateAbstractState.class);
      ARGState newBlockEndState = blockEndState;

      // remove InvariantsState if predicateState is an abstraction state
      if (predicateState.isAbstractionState()) {
        List<AbstractState> wrappedStates =
            new ArrayList<>(
                ((CompositeState) blockEndState.getWrappedStates().get(0)).getWrappedStates());
        wrappedStates.removeIf(state -> state instanceof InvariantsState);
        newBlockEndState = new ARGState(new CompositeState(wrappedStates), null);
      }

      if (dcpa.isTop(blockEndState) && !allowTop) {
        return ImmutableSet.of();
      }

      BlockSummaryMessagePayload serialized =
          dcpa.serialize(newBlockEndState, reachedSet.getPrecision(blockEndState));

      messages.add(
          BlockSummaryMessage.newBlockPostCondition(
              block.getId(),
              block.getLast().getNodeNumber(),
              DCPAAlgorithms.appendStatus(status, serialized),
              true));
      return messages.build();
    }
    AbstractState start =
        dcpa.getInitialState(block.getLast(), StateSpacePartition.getDefaultPartition());
    PredicateCPA predicateCPA =
        Objects.requireNonNull(CPAs.retrieveCPA(dcpa.getCPA(), PredicateCPA.class));

    SSAMapBuilder newMap = SSAMap.emptySSAMap().builder();
    BooleanFormulaManagerView bmgr =
        predicateCPA.getSolver().getFormulaManager().getBooleanFormulaManager();
    BooleanFormula predicateFormula = bmgr.makeFalse();

    for (PredicateAbstractState abstractState :
        FluentIterable.from(blockEnds)
            .transform(b -> AbstractStates.extractStateByType(b, PredicateAbstractState.class))) {
      predicateFormula =
          bmgr.or(predicateFormula, abstractState.getAbstractionFormula().asFormula());
      SSAMap ssa = abstractState.getPathFormula().getSsa();
      for (String variable : ssa.allVariables()) {
        if (!newMap.build().containsVariable(variable)) {
          newMap.setIndex(variable, ssa.getType(variable), 1);
        }
      }
    }
    predicateFormula =
        predicateCPA
            .getSolver()
            .getFormulaManager()
            .simplifyBooleanFormula(
                predicateCPA
                    .getSolver()
                    .getFormulaManager()
                    .instantiate(predicateFormula, newMap.build()));
    PredicateAbstractState state =
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            predicateCPA
                .getPathFormulaManager()
                .makeEmptyPathFormulaWithContext(
                    newMap.build(), PointerTargetSet.emptyPointerTargetSet())
                .withFormula(predicateFormula),
            Objects.requireNonNull(
                AbstractStates.extractStateByType(makeStartState(), PredicateAbstractState.class)));

    // Create new invariantsState which joins all invariantsStates of the blockEnds
    InvariantsCPA invariantsCPA = CPAs.retrieveCPA(dcpa.getCPA(), InvariantsCPA.class);
    InvariantsState joinedInvariantsState = null;
    if (invariantsCPA != null) {
      joinedInvariantsState =
          (InvariantsState)
              invariantsCPA.getInitialState(
                  block.getLast(), StateSpacePartition.getDefaultPartition());
      for (InvariantsState invariantState :
          FluentIterable.from(blockEnds)
              .transform(b -> AbstractStates.extractStateByType(b, InvariantsState.class))) {
        joinedInvariantsState = joinedInvariantsState.join(invariantState);
      }
    }
    List<AbstractState> curr = new ArrayList<>();
    for (AbstractState wrappedState :
        Objects.requireNonNull(AbstractStates.extractStateByType(start, CompositeState.class))
            .getWrappedStates()) {
      if (wrappedState instanceof PredicateAbstractState) {
        curr.add(state);
      } else if (wrappedState instanceof InvariantsState) {
        if (joinedInvariantsState != null) {
          curr.add(joinedInvariantsState);
        }
      } else {
        curr.add(wrappedState);
      }
    }
    ARGState blockEndState = new ARGState(new CompositeState(curr), null);
    if (dcpa.isTop(blockEndState) && !allowTop) {
      return ImmutableSet.of();
    }
    BlockSummaryMessagePayload serialized =
        dcpa.serialize(blockEndState, reachedSet.getPrecision(Iterables.get(blockEnds, 0)));
    messages.add(
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getLast().getNodeNumber(),
            DCPAAlgorithms.appendStatus(status, serialized),
            true));
    return messages.build();
  }

  private boolean implies(
      BlockSummaryPostConditionMessage pMessage1, BlockSummaryPostConditionMessage pMessage2)
      throws InterruptedException, SolverException {
    if (pMessage1 == null || pMessage2 == null) {
      return false;
    }
    PredicateCPA predicateCPA =
        Objects.requireNonNull(CPAs.retrieveCPA(dcpa.getCPA(), PredicateCPA.class));
    PredicateAbstractState msg1 =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(
                dcpa.getDeserializeOperator().deserialize(pMessage1),
                PredicateAbstractState.class));
    PredicateAbstractState msg2 =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(
                dcpa.getDeserializeOperator().deserialize(pMessage2),
                PredicateAbstractState.class));
    return predicateCPA
        .getSolver()
        .implies(msg1.getPathFormula().getFormula(), msg2.getPathFormula().getFormula());
  }

  private Collection<BlockSummaryMessage> reportErrorConditions(
      Set<ARGState> violations,
      ARGState condition,
      boolean first,
      String pPrefix,
      boolean restoreAll)
      throws CPAException, InterruptedException, SolverException {
    ImmutableSet.Builder<ARGPath> pathsToViolations = ImmutableSet.builder();
    if (restoreAll) {
      for (ARGState violation : violations) {
        Set<ARGPath> found = new HashSet<>();
        ARGPath p = ARGUtils.getOnePathTo(violation);
        found.add(p);
        Optional<ARGPath> p2 = ARGUtils.getOnePathTo(violation, found);
        while (p2.isPresent()) {
          found.add(p2.orElseThrow());
          p2 = ARGUtils.getOnePathTo(violation, found);
        }
        pathsToViolations.addAll(found);
      }
    } else {
      pathsToViolations.addAll(
          transformedImmutableSetCopy(
              violations,
              v ->
                  ARGUtils.tryGetOrCreateCounterexampleInformation(
                          v, dcpa.getCPA(), assumptionToEdgeAllocator)
                      .orElseThrow()
                      .getTargetPath()));
    }
    ImmutableSet.Builder<BlockSummaryMessage> messages = ImmutableSet.builder();
    boolean makeFirst = false;
    for (ARGPath path : pathsToViolations.build()) {
      AbstractState abstractState;
      try {
        abstractState = dcpa.computeVerificationCondition(path, condition);
      } catch (VerificationConditionException e) {
        // see semantics of VerificationConditionException
        continue;
      }
      String prefix =
          FluentIterable.from(path.getFullPath())
              .transform(e -> e.getPredecessor() + "->" + e.getSuccessor())
              .join(Joiner.on(","));
      if (!pPrefix.isBlank()) {
        prefix = pPrefix + "," + prefix;
      }
      BlockSummaryMessagePayload serialized =
          dcpa.serialize(abstractState, reachedSet.getPrecision(path.getLastState()));
      messages.add(
          BlockSummaryMessage.newErrorConditionMessage(
              block.getId(),
              block.getFirst().getNodeNumber(),
              DCPAAlgorithms.appendStatus(status, serialized),
              first || makeFirst,
              prefix));
      makeFirst = true;
    }
    return messages.build();
  }

  public Collection<BlockSummaryMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(makeStartState(), makeStartPrecision());

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    if (result.getViolationStates().isEmpty()) {
      if (result.getBlockEndStates().isEmpty()) {
        return reportUnreachableBlockEnd();
      }
      return reportBlockPostConditions(result.getBlockEndStates(), true);
    }

    return reportErrorConditions(result.getViolationStates(), null, true, "", true);
  }

  public BlockSummaryMessageProcessing shouldRepeatAnalysis(
      BlockSummaryPostConditionMessage pReceived) throws InterruptedException, SolverException {
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pReceived);
    BlockSummaryMessageProcessing processing =
        dcpa.getProceedOperator().processForward(deserialized);
    if (!processing.shouldProceed()) {
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
    // but it has no access to this attribute.
    boolean repeat = false;
    if (pReceived.isReachable()) {
      boolean receivedMessageHasNewInformation = hasNewInformation(pReceived);
      // reset all loop predecessors if non-loop predecessor updates
      if (!loopPredecessors.isEmpty() && !loopPredecessors.contains(pReceived.getBlockId())) {
        if (receivedMessageHasNewInformation) {
          repeat = true;
          loopPredecessors.forEach(id -> states.put(id, null));
          soundPredecessors.clear();
        }
      }
      if (loopPredecessors.contains(pReceived.getBlockId()) && dcpa.isTop(deserialized)) {
        states.put(pReceived.getBlockId(), null);
        repeat = true;
        soundPredecessors.remove(pReceived.getBlockId());
      } else {
        if (receivedMessageHasNewInformation) {
          repeat = true;
          soundPredecessors.remove(pReceived.getBlockId());
        } else if (loopPredecessors.contains(pReceived.getBlockId())) {
          soundPredecessors.add(pReceived.getBlockId());
        }
        states.put(pReceived.getBlockId(), pReceived);
      }
    } else {
      // null means that we cannot expect a state from this predecessor, i.e.,
      // we do not under-approximate when ignoring this predecessor.
      states.put(pReceived.getBlockId(), null);
      if (loopPredecessors.contains(pReceived.getBlockId())) {
        soundPredecessors.add(pReceived.getBlockId());
      }
    }
    return repeat ? BlockSummaryMessageProcessing.proceed() : BlockSummaryMessageProcessing.stop();
  }

  /**
   * Returns whether the given message provides a stronger postcondition for its block id then the
   * postcondition already known.
   */
  private boolean hasNewInformation(BlockSummaryPostConditionMessage pNewMessage)
      throws SolverException, InterruptedException {
    BlockSummaryPostConditionMessage oldPostCond =
        (BlockSummaryPostConditionMessage) states.get(pNewMessage.getBlockId());
    return !implies(oldPostCond, pNewMessage);
  }

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @param pReceived Current message to process
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<BlockSummaryMessage> runAnalysis(BlockSummaryPostConditionMessage pReceived)
      throws SolverException, InterruptedException, CPAException {
    logger.log(Level.INFO, "Running forward analysis with new precondition");
    // check if message is meant for this block
    // if we do not have messages from all predecessors we under-approximate, so we abort!
    // if element is top element, we abort
    // for now we do not analyze at all
    BlockSummaryMessageProcessing processing = shouldRepeatAnalysis(pReceived);
    if (!processing.shouldProceed()) {
      return processing;
    }

    ImmutableSet.Builder<BlockSummaryMessage> fixpointIteration = ImmutableSet.builder();
    for (BlockSummaryErrorConditionMessage value : errors.values()) {
      for (BlockSummaryMessage blockSummaryMessage : runAnalysisUnderCondition(value, false)) {
        if ((blockSummaryMessage.getType() == MessageType.BLOCK_POSTCONDITION
                || blockSummaryMessage.getType() == MessageType.ERROR_CONDITION)
            && soundPredecessors.containsAll(loopPredecessors)) {
          fixpointIteration.add(blockSummaryMessage);
        }
      }
    }
    return fixpointIteration.build();
  }

  private AbstractState makeStartState() throws InterruptedException {
    return dcpa.getInitialState(block.getFirst(), StateSpacePartition.getDefaultPartition());
  }

  private Precision makeStartPrecision() throws InterruptedException {
    return dcpa.getInitialPrecision(block.getFirst(), StateSpacePartition.getDefaultPartition());
  }

  public void updateErrorCondition(BlockSummaryErrorConditionMessage pErrorCondition) {
    boolean covered = false;
    Set<String> originPrefix =
        ImmutableSet.copyOf(Splitter.on(",").splitToList(pErrorCondition.getOrigin()));
    for (Set<String> errorPrefixes : errors.keySet()) {
      if (originPrefix.containsAll(errorPrefixes)) {
        covered = true;
        break;
      }
    }
    if (!covered) {
      errors.put(originPrefix, pErrorCondition);
    }
  }

  public Set<String> updateSeenPrefixes(BlockSummaryErrorConditionMessage errorCond) {
    ImmutableSet<String> originPrefixes =
        ImmutableSet.copyOf(Splitter.on(",").splitToList(errorCond.getOrigin()));
    seenPrefixes.add(originPrefixes);
    return originPrefixes;
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
      BlockSummaryErrorConditionMessage pErrorCondition, boolean put)
      throws CPAException, InterruptedException, SolverException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    // merge all states into the reached set
    AbstractState errorCondition = dcpa.getDeserializeOperator().deserialize(pErrorCondition);
    BlockSummaryMessageProcessing processing =
        dcpa.getProceedOperator().processBackward(errorCondition);
    if (!processing.shouldProceed()) {
      return processing;
    }

    if (put) {
      updateErrorCondition(pErrorCondition);
    }
    prepareReachedSet();

    reachedSet.forEach(
        abstractState ->
            Objects.requireNonNull(
                    AbstractStates.extractStateByType(abstractState, BlockState.class))
                .setErrorCondition(errorCondition));

    BlockAnalysisIntermediateResult result =
        DCPAAlgorithms.findReachableTargetStatesInBlock(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    ImmutableSet<Set<String>> previouslySeenPrefixes = ImmutableSet.copyOf(seenPrefixes);
    Set<String> originPrefixes = updateSeenPrefixes(pErrorCondition);
    boolean matched = false;
    for (Set<String> seenPrefix : previouslySeenPrefixes) {
      if (originPrefixes.containsAll(seenPrefix)) {
        matched = true;
        break;
      }
    }

    ImmutableSet.Builder<BlockSummaryMessage> messages = ImmutableSet.builder();
    if (!result.getBlockEndStates().isEmpty()
        && block.isAbstractionPossible()
        && result.getAbstractionStates().isEmpty()) {
      messages.addAll(reportBlockPostConditions(result.getBlockEndStates(), false));
    }
    boolean restoreAll = !matched && !loopPredecessors.isEmpty();
    if (restoreAll) {
      reachedSet.clear();
      reachedSet.add(makeStartState(), makeStartPrecision());
      reachedSet.forEach(
          abstractState ->
              Objects.requireNonNull(
                      AbstractStates.extractStateByType(abstractState, BlockState.class))
                  .setErrorCondition(errorCondition));

      result = DCPAAlgorithms.findReachableTargetStatesInBlock(algorithm, reachedSet, block);
      status = status.update(result.getStatus());
    }
    messages.addAll(
        reportErrorConditions(
            result.getAbstractionStates(),
            ((ARGState) errorCondition),
            false,
            pErrorCondition.getOrigin(),
            false));
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
        reachedSet.add(value, makeStartPrecision());
      } else {
        // CPA algorithm
        for (AbstractState abstractState : ImmutableSet.copyOf(reachedSet)) {
          AbstractState merged =
              cpa.getMergeOperator().merge(value, abstractState, makeStartPrecision());
          if (!merged.equals(abstractState)) {
            reachedSet.remove(abstractState);
            reachedSet.add(merged, makeStartPrecision());
          }
        }
        if (!cpa.getStopOperator()
            .stop(value, reachedSet.getReached(block.getFirst()), makeStartPrecision())) {
          reachedSet.add(value, makeStartPrecision());
        }
      }
    }

    if (reachedSet.isEmpty()) {
      reachedSet.add(makeStartState(), makeStartPrecision());
    }
  }

  public DistributedConfigurableProgramAnalysis getDCPA() {
    return dcpa;
  }
}
