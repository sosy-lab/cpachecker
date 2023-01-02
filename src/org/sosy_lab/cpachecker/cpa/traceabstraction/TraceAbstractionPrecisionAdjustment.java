// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * PrecisionAdjustment of the {@link TraceAbstractionCPA}. It delegates the wrapped {@link
 * PredicateAbstractState} to the {@link PredicatePrecisionAdjustment} and uses the result to
 * compute the actual successor of the TraceAbstractionState afterwards.
 */
class TraceAbstractionPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdjustment;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final InterpolationSequenceStorage itpSequenceStorage;

  private final PredicateAbstractionManager predicateAbstractionManager;
  private final FormulaManagerView fMgrView;
  private final BooleanFormulaManagerView bFMgrView;
  private final AbstractionManager abstractionManager;

  TraceAbstractionPrecisionAdjustment(
      PrecisionAdjustment pWrappedPrecAdjustment,
      FormulaManagerView pFormulaManagerView,
      PredicateAbstractionManager pPredicateAbstractionManager,
      AbstractionManager pAbstractionManager,
      InterpolationSequenceStorage pItpSequenceStorage,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    itpSequenceStorage = pItpSequenceStorage;
    fMgrView = pFormulaManagerView;
    bFMgrView = pFormulaManagerView.getBooleanFormulaManager();
    abstractionManager = pAbstractionManager;
    predicateAbstractionManager = pPredicateAbstractionManager;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    // The precision adjustment of TraceAbstraction consists of two steps:
    // First, the wrapped CPA (= predicateCPA) performs its precision adjustment.
    // Second (in case it contains a result), compute the actual successor
    // of the trace abstraction state.

    checkArgument(pState instanceof TraceAbstractionState);
    checkArgument(checkEmptyPredicatePrecision(pPrecision));
    checkArgument(pFullState instanceof ARGState);

    TraceAbstractionState taState = (TraceAbstractionState) pState;
    AbstractState wrappedPredState = verifyNotNull(taState.getWrappedState());

    Optional<PrecisionAdjustmentResult> wrappedPrecResult =
        wrappedPrecAdjustment.prec(
            wrappedPredState, PredicatePrecision.empty(), pStates, pStateProjection, pFullState);

    if (wrappedPrecResult.isEmpty()) {
      return Optional.empty();
    }

    PrecisionAdjustmentResult precisionAdjustmentResult = wrappedPrecResult.orElseThrow();
    AbstractState newWrappedState = precisionAdjustmentResult.abstractState();

    verify(newWrappedState instanceof PredicateAbstractState);
    verify(checkEmptyPredicatePrecision(precisionAdjustmentResult.precision()));

    PredicateAbstractState predSuccessor = (PredicateAbstractState) newWrappedState;
    if (newWrappedState != wrappedPredState) {
      taState = taState.withWrappedState(predSuccessor);
    }

    TraceAbstractionState result =
        computeTraceAbstractionSuccessor(taState, predSuccessor, (ARGState) pFullState);
    if (result == null) {
      // Computed trace abstraction state is bottom, i.e., this state is infeasible
      return Optional.empty();
    }

    return Optional.of(
        PrecisionAdjustmentResult.create(result, pPrecision, precisionAdjustmentResult.action()));
  }

  private boolean checkEmptyPredicatePrecision(Precision pPrecision) {
    return pPrecision instanceof PredicatePrecision
        && pPrecision.equals(PredicatePrecision.empty());
  }

  @Nullable // Null value represents the bottom state
  private TraceAbstractionState computeTraceAbstractionSuccessor(
      TraceAbstractionState pTaState, PredicateAbstractState pPredSuccessor, ARGState pFullState)
      throws CPATransferException, InterruptedException {

    if (itpSequenceStorage.isEmpty() && !pTaState.containsPredicates()) {
      // The predecessor states have not yet been part of a refinement.
      // There are thus no predicates available for further processing.
      return pTaState;
    }

    AbstractionFormula abstractionFormula = pPredSuccessor.getAbstractionFormula();

    verify(
        abstractionFormula.isTrue(),
        "AbstractionFormula was modified. This is not expected when using the TraceAbstraction"
            + " refinement");

    Optional<CallstackStateEqualsWrapper> callstackWrapper =
        AbstractStates.extractOptionalCallstackWraper(pFullState);

    CFANode curLocation = verifyNotNull(AbstractStates.extractLocation(pFullState));
    verify(pFullState.getParents().size() == 1);
    ARGState parentARGState = Iterables.getOnlyElement(pFullState.getParents());
    CFANode parentLocation = verifyNotNull(AbstractStates.extractLocation(parentARGState));

    List<CFAEdge> connectingCfaEdges = parentARGState.getEdgesToChild(pFullState);

    logger.logf(
        Level.FINE,
        "Taking edge: N%s -> N%s // %s\n",
        parentLocation.getNodeNumber(),
        curLocation.getNodeNumber(),
        connectingCfaEdges.stream().map(CFAEdge::getDescription).collect(Collectors.joining("\n")));

    if (connectingCfaEdges.stream().allMatch(x -> x.getEdgeType() == CFAEdgeType.BlankEdge)) {
      // Shortcut: the edge(s) do not contain any assignments or assumptions, so
      // we just return the current holding predicates
      logger.logf(
          Level.FINER, "Edge(s) only consist of blank edges; returning the current TAstate");
      return pTaState;
    }

    // TAStates only store predicates that actually hold in the respective states
    ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> statePredicates =
        pTaState.getActivePredicates();

    Integer newLocInstance =
        pPredSuccessor.getAbstractionLocationsOnPath().getOrDefault(curLocation, 0);
    LocationInstance locInstance = new LocationInstance(curLocation, newLocInstance);

    ImmutableMap.Builder<InterpolationSequence, IndexedAbstractionPredicate> itpSequenceMapping =
        ImmutableMap.builder();

    boolean continueExploring =
        checkTASuccessors(
            itpSequenceMapping,
            locInstance,
            statePredicates,
            connectingCfaEdges,
            abstractionFormula,
            curLocation,
            parentLocation,
            callstackWrapper);
    if (!continueExploring) {
      // One of the abstraction computations yielded a <false> result,
      // meaning that the successor state is not feasible.
      return null;
    }

    checkInactivePredicates(
        itpSequenceMapping,
        abstractionFormula,
        callstackWrapper,
        curLocation,
        parentLocation,
        statePredicates,
        locInstance);

    ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> newPreds =
        itpSequenceMapping.buildOrThrow();
    logger.logf(Level.FINER, "Active predicates in the next state: %s\n", newPreds);
    return new TraceAbstractionState(pPredSuccessor, newPreds);
  }

  /**
   * Compute the abstraction for the predicates that have already been active in the parent state.
   * This is done for each predicate separately. Depending on the outcome of the abstraction
   * computation, each predicate may advance to its next interpolation state, or otherwise stays in
   * the current interpolation state. If the successor for at least one of the interpolation states
   * is false, bottom state gets returned.
   *
   * @return false iff bottom state is reached; true otherwise
   */
  private boolean checkTASuccessors(
      ImmutableMap.Builder<InterpolationSequence, IndexedAbstractionPredicate> pItpSequenceMapping,
      LocationInstance locInstance,
      ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> statePredicates,
      List<CFAEdge> connectingCfaEdges,
      AbstractionFormula abstractionFormula,
      CFANode curLocation,
      CFANode parentLocation,
      Optional<CallstackStateEqualsWrapper> callstackWrapper)
      throws CPATransferException, InterruptedException {

    logger.log(Level.FINER, "Computing abstractions");

    for (Map.Entry<InterpolationSequence, IndexedAbstractionPredicate> entry :
        statePredicates.entrySet()) {

      Optional<InterpolationSequence> updatedItpSequence =
          itpSequenceStorage.getUpdatedItpSequence(entry.getKey());
      InterpolationSequence currentItpSequence = updatedItpSequence.orElse(entry.getKey());
      if (!currentItpSequence.isInScopeOf(locInstance)) {
        continue;
      }

      PathFormula pathFormula = abstractionFormula.getBlockFormula();
      //      PathFormula pathFormula = pPredSuccessor.getPathFormula();

      IndexedAbstractionPredicate curPreds = entry.getValue();

      Optional<IndexedAbstractionPredicate> nextPreds =
          currentItpSequence.getNextPredicate(curPreds);

      ImmutableList<IndexedAbstractionPredicate> precondition =
          nextPreds.isEmpty()
              ? ImmutableList.of(curPreds)
              : ImmutableList.of(curPreds, nextPreds.orElseThrow());

      BooleanFormula instantiatedFormula =
          fMgrView.instantiate(
              curPreds.getPredicate().getSymbolicAtom(),
              abstractionFormula.getBlockFormula().getSsa());

      BooleanFormula resultFormula = bFMgrView.and(pathFormula.getFormula(), instantiatedFormula);
      pathFormula = pathFormula.withFormula(resultFormula);

      AbstractionFormula computedPostCondition =
          buildAbstraction(
              ImmutableSet.of(curLocation, parentLocation),
              callstackWrapper,
              abstractionFormula,
              pathFormula,
              Collections3.transformedImmutableSetCopy(
                  precondition, IndexedAbstractionPredicate::getPredicate));

      if (computedPostCondition.isTrue()) {

        if (connectingCfaEdges.stream()
            .anyMatch(edge -> edge.getEdgeType() == CFAEdgeType.StatementEdge)) {
          boolean assignmentMadeForRelevantPred =
              precondition.stream()
                  .anyMatch(
                      pred ->
                          anyVariableModified(
                              abstractionFormula.getBlockFormula().getSsa(),
                              computedPostCondition.getBlockFormula().getSsa(),
                              fMgrView.extractVariableNames(
                                  pred.getPredicate().getSymbolicAtom())));
          if (assignmentMadeForRelevantPred) {
            continue;
          }
        }

        // Abstraction formula is true; stay in the current (interpolation) state
        pItpSequenceMapping.put(currentItpSequence, curPreds);
        continue;
      }

      if (computedPostCondition.isFalse()) {
        // Abstraction is false => successor state is not feasible
        return false;
      }

      // A non-trivial abstraction was computed
      if (nextPreds.isPresent()
          && checkPostcondEntailingPrecond(
              nextPreds.orElseThrow().getPredicate(), computedPostCondition)) {
        // Abstraction matches the postcondition -> go into next Itp state
        pItpSequenceMapping.put(currentItpSequence, nextPreds.orElseThrow());
        continue;
      } else if (checkPostcondEntailingPrecond(curPreds.getPredicate(), computedPostCondition)) {
        // Abstraction matches the precondition -> stay in current Itp state
        pItpSequenceMapping.put(currentItpSequence, curPreds);
        continue;
      }

      if (curPreds
          .getPredicate()
          .getSymbolicAtom()
          .equals(bFMgrView.not(computedPostCondition.asFormula()))) {
        // Pre- and postcondition are negated to each other. The Hoare triple is of the following
        // form: [x==0] x!=0 [x!=0]

        if (nextPreds.isEmpty()) {
          // The next pred is <false>.
          // As the abstraction result  was however not evaluated to false, this just means
          // that the current predicate just does not hold anymore in the next state.
          logger.log(
              Level.FINEST,
              "Abstraction is contradictory to current input predicates. The node is not"
                  + " reachable");
          continue;
        }

      } else if (nextPreds.isPresent()
          && nextPreds
              .orElseThrow()
              .getPredicate()
              .getSymbolicAtom()
              .equals(bFMgrView.not(computedPostCondition.asFormula()))) {
        logger.log(
            Level.FINEST,
            "Abstraction is contradictory to current input predicates. The node is not reachable");
        return false;
      } else {

        // The postcondition is ambiguous. We cannot tell anything about the next
        // state. For now an exception is thrown until this is handled accordingly to make it
        // visible in the log
        throw new AssertionError(
            "Interpolant <false> is following on an interpolant <true>. This is not yet handled");
      }
    }

    return true;
  }

  private void checkInactivePredicates(
      ImmutableMap.Builder<InterpolationSequence, IndexedAbstractionPredicate> itpSequenceMapping,
      AbstractionFormula abstractionFormula,
      Optional<CallstackStateEqualsWrapper> callstackWrapper,
      CFANode curLocation,
      CFANode parentLocation,
      ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> statePredicates,
      LocationInstance locInstance)
      throws InterruptedException, CPATransferException {

    for (InterpolationSequence itpSequence :
        itpSequenceStorage.difference(statePredicates.keySet())) {
      Optional<IndexedAbstractionPredicate> itpOpt = itpSequence.getFirst(locInstance);
      if (itpOpt.isEmpty()) {
        // Sequence is not in the scope of the current location or function
        continue;
      }

      IndexedAbstractionPredicate indexedPred = itpOpt.orElseThrow();
      AbstractionPredicate preconditionPreds = indexedPred.getPredicate();
      AbstractionFormula computedPostCondition =
          buildAbstraction(
              ImmutableSet.of(curLocation, parentLocation),
              callstackWrapper,
              abstractionFormula,
              abstractionFormula.getBlockFormula(),
              ImmutableSet.of(preconditionPreds));

      if (computedPostCondition.isTrue() || computedPostCondition.isFalse()) {
        // The abstraction formula is either <true> or <false>. For the former this just means
        // that the variables of the precondition are unrelated to the abstractionFormula.
        // For the latter the variables just coincidentally matched, and can thus be safely
        // disregarded at this point.
        continue;
      }

      if (checkPostcondEntailingPrecond(preconditionPreds, computedPostCondition)) {
        assert preconditionPreds.getSymbolicAtom().equals(computedPostCondition.asFormula());

        // A non-trivial abstraction was computed that entails the input predicates
        // (i.e., 'abstraction result => input predicate'). The current predicate is thus
        // added to the successor TAState

        // (The pre- and postcondition are equal to each other. The abstraction predicate is the
        // first in the current interpolation sequence)
        itpSequenceMapping.put(itpSequence, indexedPred);
      }
    }
  }

  /**
   * Check the region which is represented by the computed abstraction (i.e., the postcondition)
   * whether it is a subset of the precondition
   *
   * <p>More precisely, this method checks the following condition: postcond => precond
   */
  private boolean checkPostcondEntailingPrecond(
      AbstractionPredicate precondition, AbstractionFormula computedPostCondition)
      throws CPATransferException, InterruptedException {
    try {
      return abstractionManager.entails(
          computedPostCondition.asRegion(), precondition.getAbstractVariable());
    } catch (SolverException e) {
      throw new CPATransferException(e.getMessage(), e);
    }
  }

  private boolean anyVariableModified(
      SSAMap ssaParent, SSAMap ssaChild, Set<String> predVariables) {
    for (String variable : predVariables) {
      if (ssaChild.containsVariable(variable)
          && ssaParent.containsVariable(variable)
          && ssaChild.getIndex(variable) == ssaParent.getIndex(variable) + 1) {
        return true;
      }
    }

    return false;
  }

  private AbstractionFormula buildAbstraction(
      ImmutableSet<CFANode> pCfaNodes,
      Optional<CallstackStateEqualsWrapper> callstackWrapper,
      AbstractionFormula abstractionFormula,
      PathFormula pathFormula,
      ImmutableSet<AbstractionPredicate> relevantPreds)
      throws InterruptedException, CPATransferException {
    try {
      logger.logf(Level.FINER, "Predicates: %s\n", relevantPreds);
      shutdownNotifier.shutdownIfNecessary();

      AbstractionFormula abstractionResult =
          predicateAbstractionManager.buildAbstraction(
              pCfaNodes, callstackWrapper, abstractionFormula, pathFormula, relevantPreds);

      logger.logf(Level.FINER, "New abstraction formula: %s\n", abstractionFormula);
      printHoareTriple(relevantPreds, abstractionResult);
      printRegions(relevantPreds, abstractionResult);

      return abstractionResult;
    } catch (SolverException e) {
      throw new CPATransferException("Solver Failure: " + e.getMessage(), e);
    }
  }

  private void printRegions(
      ImmutableSet<AbstractionPredicate> relevantPreds, AbstractionFormula abstractionResult)
      throws SolverException, InterruptedException {
    Region regionPost = abstractionResult.asRegion();
    logger.log(
        Level.FINE,
        MoreStrings.lazyString(
            () ->
                String.format(
                    "Region Post: %s -- BF: %s%n",
                    regionPost, abstractionManager.convertRegionToFormula(regionPost))));

    for (AbstractionPredicate abstractionPredicate : relevantPreds) {
      Region regionPre = abstractionPredicate.getAbstractVariable();
      logger.log(
          Level.FINE,
          MoreStrings.lazyString(
              () ->
                  String.format(
                      "Region Pre: %s -- BF: %s%n",
                      regionPre, abstractionManager.convertRegionToFormula(regionPre))));

      logger.logf(
          Level.FINE,
          "%s => %s (pre => post): %s%n%s => %s (post => pre): %s%n",
          regionPre,
          regionPost,
          abstractionManager.entails(regionPre, regionPost),
          regionPost,
          regionPre,
          abstractionManager.entails(regionPost, regionPre));
    }
  }

  private void printHoareTriple(
      Collection<AbstractionPredicate> pPredicates, AbstractionFormula resultFormula) {
    Object prettyPrintPredicateList =
        MoreStrings.lazyString(
            () ->
                FluentIterable.from(pPredicates)
                    .transform(x -> x.getSymbolicAtom())
                    .join(Joiner.on(", ")));
    logger.logf(
        Level.FINER,
        "[%s] --- %s --- %s",
        prettyPrintPredicateList,
        resultFormula.getBlockFormula(),
        resultFormula.asInstantiatedFormula());
  }
}
