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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.BlockAnalysisIntermediateResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysisFactory.AnalysisComponents;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
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

public class DssBlockAnalysis {

  private final DistributedConfigurableProgramAnalysis dcpa;
  // In this map, value 'null' means that the key 'blockId' is a loop predecessor
  // from which we have not seen any summary message yet
  private final Map<String, DssMessage> states;
  private final ConfigurableProgramAnalysis cpa;
  private final BlockNode block;
  private final ReachedSet reachedSet;
  private final Algorithm algorithm;
  private final Set<String> predecessors;
  private final Set<String> loopPredecessors;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final Set<Set<String>> seenPrefixes;
  private final Map<Set<String>, DssViolationConditionMessage> errors;
  private final LogManager logger;

  private final DssMessageFactory messageFactory;

  // forward analysis variables
  private AlgorithmStatus status;
  private boolean alreadyReportedInfeasibility;
  private final Set<String> soundPredecessors;

  public DssBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      Specification pSpecification,
      Configuration pConfiguration,
      DssAnalysisOptions pOptions,
      DssMessageFactory pMessageFactory,
      ShutdownManager pShutdownManager)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    messageFactory = pMessageFactory;
    alreadyReportedInfeasibility = false;
    AnalysisComponents parts =
        DssBlockAnalysisFactory.createAlgorithm(
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
        DssFactory.distribute(
            cpa,
            pBlock,
            pCFA,
            pConfiguration,
            pOptions,
            pMessageFactory,
            pLogger,
            pShutdownManager.getNotifier());
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

  public Collection<DssMessage> reportUnreachableBlockEnd() {
    // if sent once, it will never change (precondition is always the most general information)
    if (alreadyReportedInfeasibility) {
      return ImmutableSet.of();
    }
    alreadyReportedInfeasibility = true;
    return ImmutableSet.of(
        messageFactory.newBlockPostCondition(
            block.getId(),
            block.getLast().getNodeNumber(),
            DssBlockAnalyses.appendStatus(
                AlgorithmStatus.SOUND_AND_PRECISE, DssMessagePayload.empty()),
            false));
  }

  private Collection<DssMessage> reportBlockPostConditions(
      Set<ARGState> blockEnds, boolean allowTop) throws InterruptedException {
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    if (blockEnds.size() == 1) {
      ARGState blockEndState = Iterables.getOnlyElement(blockEnds);
      if (dcpa.isTop(blockEndState) && !allowTop) {
        return ImmutableSet.of();
      }
      DssMessagePayload serialized =
          dcpa.serialize(blockEndState, reachedSet.getPrecision(blockEndState));
      messages.add(
          messageFactory.newBlockPostCondition(
              block.getId(),
              block.getLast().getNodeNumber(),
              DssBlockAnalyses.appendStatus(status, serialized),
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
    BooleanFormula formula = bmgr.makeFalse();
    for (PredicateAbstractState abstractState :
        FluentIterable.from(blockEnds)
            .transform(b -> AbstractStates.extractStateByType(b, PredicateAbstractState.class))) {
      formula = bmgr.or(formula, abstractState.getAbstractionFormula().asFormula());
      SSAMap ssa = abstractState.getPathFormula().getSsa();
      for (String variable : ssa.allVariables()) {
        if (!newMap.build().containsVariable(variable)) {
          newMap.setIndex(variable, ssa.getType(variable), 1);
        }
      }
    }
    formula =
        predicateCPA
            .getSolver()
            .getFormulaManager()
            .simplifyBooleanFormula(
                predicateCPA.getSolver().getFormulaManager().instantiate(formula, newMap.build()));
    PredicateAbstractState state =
        PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
            predicateCPA
                .getPathFormulaManager()
                .makeEmptyPathFormulaWithContext(
                    newMap.build(), PointerTargetSet.emptyPointerTargetSet())
                .withFormula(formula),
            Objects.requireNonNull(
                AbstractStates.extractStateByType(makeStartState(), PredicateAbstractState.class)));
    List<AbstractState> curr = new ArrayList<>();
    for (AbstractState wrappedState :
        Objects.requireNonNull(AbstractStates.extractStateByType(start, CompositeState.class))
            .getWrappedStates()) {
      if (wrappedState instanceof PredicateAbstractState) {
        curr.add(state);
      } else {
        curr.add(wrappedState);
      }
    }
    ARGState blockEndState = new ARGState(new CompositeState(curr), null);
    if (dcpa.isTop(blockEndState) && !allowTop) {
      return ImmutableSet.of();
    }
    DssMessagePayload serialized =
        dcpa.serialize(blockEndState, reachedSet.getPrecision(Iterables.get(blockEnds, 0)));
    messages.add(
        messageFactory.newBlockPostCondition(
            block.getId(),
            block.getLast().getNodeNumber(),
            DssBlockAnalyses.appendStatus(status, serialized),
            true));
    return messages.build();
  }

  private boolean implies(DssPostConditionMessage pMessage1, DssPostConditionMessage pMessage2)
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

  private Collection<DssMessage> reportViolationConditions(
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
    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
    boolean makeFirst = false;
    for (ARGPath path : pathsToViolations.build()) {
      Optional<AbstractState> verificationCondition =
          dcpa.getViolationConditionOperator()
              .computeViolationCondition(path, Optional.ofNullable(condition));
      if (verificationCondition.isEmpty()) {
        continue;
      }
      String prefix =
          FluentIterable.from(path.getFullPath())
              .transform(e -> e.getPredecessor() + "->" + e.getSuccessor())
              .join(Joiner.on(","));
      if (!pPrefix.isBlank()) {
        prefix = pPrefix + "," + prefix;
      }
      DssMessagePayload serialized =
          dcpa.serialize(
              verificationCondition.orElseThrow(), reachedSet.getPrecision(path.getLastState()));
      messages.add(
          messageFactory.newViolationConditionMessage(
              block.getId(),
              block.getFirst().getNodeNumber(),
              DssBlockAnalyses.appendStatus(status, serialized),
              first || makeFirst,
              prefix));
      makeFirst = true;
    }
    return messages.build();
  }

  public Collection<DssMessage> runInitialAnalysis()
      throws CPAException, InterruptedException, SolverException {
    reachedSet.clear();
    reachedSet.add(makeStartState(), makeStartPrecision());

    BlockAnalysisIntermediateResult result =
        DssBlockAnalyses.findReachableTargetStatesInBlock(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    if (result.getViolationStates().isEmpty()) {
      if (result.getBlockEndStates().isEmpty()) {
        return reportUnreachableBlockEnd();
      }
      return reportBlockPostConditions(result.getBlockEndStates(), true);
    }

    return reportViolationConditions(result.getViolationStates(), null, true, "", true);
  }

  public DssMessageProcessing shouldRepeatAnalysis(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException {
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pReceived);
    DssMessageProcessing processing = dcpa.getProceedOperator().processForward(deserialized);
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
    //  but it has no access to this attribute.
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
    return repeat ? DssMessageProcessing.proceed() : DssMessageProcessing.stop();
  }

  /**
   * Returns whether the given message provides a stronger postcondition for its block id then the
   * postcondition already known.
   */
  private boolean hasNewInformation(DssPostConditionMessage pNewMessage)
      throws SolverException, InterruptedException {
    DssPostConditionMessage oldPostCond =
        (DssPostConditionMessage) states.get(pNewMessage.getBlockId());
    return !implies(oldPostCond, pNewMessage);
  }

  /**
   * Adds a new abstract state to the known start states and execute the configured forward
   * analysis.
   *
   * @param pReceived Current message to process
   * @return All violations and/or abstractions that occurred while running the forward analysis.
   */
  public Collection<DssMessage> runAnalysis(DssPostConditionMessage pReceived)
      throws SolverException, InterruptedException, CPAException {
    logger.log(Level.INFO, "Running forward analysis with new precondition");
    // check if message is meant for this block
    // if we do not have messages from all predecessors we under-approximate, so we abort!
    // if element is top element, we abort
    // for now we do not analyze at all
    DssMessageProcessing processing = shouldRepeatAnalysis(pReceived);
    if (!processing.shouldProceed()) {
      return processing;
    }

    ImmutableSet.Builder<DssMessage> fixpointIteration = ImmutableSet.builder();
    for (DssViolationConditionMessage value : errors.values()) {
      for (DssMessage dssMessage : runAnalysisUnderCondition(value, false)) {
        if ((dssMessage.getType() == MessageType.BLOCK_POSTCONDITION
                || dssMessage.getType() == MessageType.VIOLATION_CONDITION)
            && soundPredecessors.containsAll(loopPredecessors)) {
          fixpointIteration.add(dssMessage);
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

  public void updateViolationCondition(DssViolationConditionMessage pViolationCondition) {
    boolean covered = false;
    Set<String> originPrefix =
        ImmutableSet.copyOf(Splitter.on(",").splitToList(pViolationCondition.getOrigin()));
    for (Set<String> errorPrefixes : errors.keySet()) {
      if (originPrefix.containsAll(errorPrefixes)) {
        covered = true;
        break;
      }
    }
    if (!covered) {
      errors.put(originPrefix, pViolationCondition);
    }
  }

  public Set<String> updateSeenPrefixes(DssViolationConditionMessage errorCond) {
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
   * @param pViolationCondition a message containing an abstract state representing an error condition
   * @return Important messages for other blocks.
   * @throws CPAException thrown if CPA runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly
   */
  public Collection<DssMessage> runAnalysisUnderCondition(
      DssViolationConditionMessage pViolationCondition, boolean put)
      throws CPAException, InterruptedException, SolverException {
    logger.log(Level.INFO, "Running forward analysis with respect to error condition");
    // merge all states into the reached set
    AbstractState ViolationCondition = dcpa.getDeserializeOperator().deserialize(pViolationCondition);
    DssMessageProcessing processing = dcpa.getProceedOperator().processBackward(ViolationCondition);
    if (!processing.shouldProceed()) {
      return processing;
    }

    if (put) {
      updateViolationCondition(pViolationCondition);
    }
    prepareReachedSet();

    reachedSet.forEach(
        abstractState ->
            Objects.requireNonNull(
                    AbstractStates.extractStateByType(abstractState, BlockState.class))
                .setViolationCondition(ViolationCondition));

    BlockAnalysisIntermediateResult result =
        DssBlockAnalyses.findReachableTargetStatesInBlock(algorithm, reachedSet, block);

    status = status.update(result.getStatus());

    ImmutableSet<Set<String>> previouslySeenPrefixes = ImmutableSet.copyOf(seenPrefixes);
    Set<String> originPrefixes = updateSeenPrefixes(pViolationCondition);
    boolean matched = false;
    for (Set<String> seenPrefix : previouslySeenPrefixes) {
      if (originPrefixes.containsAll(seenPrefix)) {
        matched = true;
        break;
      }
    }

    ImmutableSet.Builder<DssMessage> messages = ImmutableSet.builder();
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
                  .setViolationCondition(ViolationCondition));

      result = DssBlockAnalyses.findReachableTargetStatesInBlock(algorithm, reachedSet, block);
      status = status.update(result.getStatus());
    }
    messages.addAll(
        reportViolationConditions(
            result.getAbstractionStates(),
            ((ARGState) ViolationCondition),
            false,
            pViolationCondition.getOrigin(),
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
    for (DssMessage message : states.values()) {
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
