// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.SolverException;

class TraceAbstractionTransferRelation extends AbstractSingleWrapperTransferRelation {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final InterpolationSequenceStorage itpSequenceStorage;

  private final PredicateAbstractionManager predicateAbstractionManager;
  private final BooleanFormulaManagerView bFMgrView;

  TraceAbstractionTransferRelation(
      TransferRelation pDelegateTransferRelation,
      FormulaManagerView pFormulaManagerView,
      PredicateAbstractionManager pPredicateAbstractionManager,
      InterpolationSequenceStorage pItpSequenceStorage,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    super(pDelegateTransferRelation);

    itpSequenceStorage = pItpSequenceStorage;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    predicateAbstractionManager = pPredicateAbstractionManager;
    bFMgrView = pFormulaManagerView.getBooleanFormulaManager();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    TraceAbstractionState taState = (TraceAbstractionState) pState;

    Collection<? extends AbstractState> delegateSuccessorStates =
        getWrappedTR()
            .getAbstractSuccessorsForEdge(taState.getWrappedState(), pPrecision, pCfaEdge);

    verify(delegateSuccessorStates.size() == 1);

    // The TraceAbstraction needs more information from other CPA-states before
    // it can compute the correct successor state.
    // Until then we let the delegate compute its successor and return it with the predicates
    // from the previous TAState (the PredicateTR is expected to only return a single successor
    // state)
    return ImmutableList.of(
        taState.withWrappedState(Iterables.getOnlyElement(delegateSuccessorStates)));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    TraceAbstractionState taState = (TraceAbstractionState) pState;

    Collection<? extends AbstractState> delegateStrengthenedStates =
        getWrappedTR().strengthen(taState.getWrappedState(), pOtherStates, pCfaEdge, pPrecision);
    verify(delegateStrengthenedStates.size() == 1);

    PredicateAbstractState predSuccessorState =
        (PredicateAbstractState) Iterables.getOnlyElement(delegateStrengthenedStates);

    if (itpSequenceStorage.isEmpty() && !taState.containsPredicates()) {
      // The predecessor states have not yet been part of a refinement.
      // There are hence no predicates available for further processing.
      return ImmutableList.of(taState.withWrappedState(predSuccessorState));
    }

    verifyNotNull(pCfaEdge, "cfaEdge may not be null.");

    PathFormula pathFormula = predSuccessorState.getPathFormula();
    AbstractionFormula abstractionFormula = predSuccessorState.getAbstractionFormula();

    verify(
        abstractionFormula.isTrue(),
        "AbstractionFormula is expected to not getting changed in TraceAbstraction refinement");

    Optional<CallstackStateEqualsWrapper> callstackWrapper =
        FluentIterable.from(pOtherStates)
            .filter(CallstackState.class)
            .first()
            .transform(CallstackStateEqualsWrapper::new)
            .toJavaUtil();

    logger.logf(
        Level.FINER,
        "Taking edge: N%s -> N%s // %s\n",
        pCfaEdge.getPredecessor().getNodeNumber(),
        pCfaEdge.getSuccessor().getNodeNumber(),
        pCfaEdge.getDescription());

    ImmutableMap.Builder<InterpolationSequence, IndexedAbstractionPredicate> newStatePreds =
        ImmutableMap.builder();

    // TAStates contain only predicates that actually hold in that state
    ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> statePredicates =
        taState.getActivePredicates();

    // TODO: correctly handle the actual location instance.
    // This can be ignored for now, as currently only function-predicates are considered
    // (more specifically, the 'locationInstancePredicates' are not considered yet).
    LocationInstance locInstance =
        new PredicatePrecision.LocationInstance(pCfaEdge.getPredecessor(), 0);

    logger.log(Level.FINER, "Computing abstractions");
    for (Entry<InterpolationSequence, IndexedAbstractionPredicate> entry :
        statePredicates.entrySet()) {

      InterpolationSequence currentItpSequence = entry.getKey();
      if (!currentItpSequence.isInScopeOf(locInstance)) {
        continue;
      }

      IndexedAbstractionPredicate curPreds = entry.getValue();

      Optional<IndexedAbstractionPredicate> nextPreds =
          currentItpSequence.getNext(locInstance, curPreds);

      IndexedAbstractionPredicate precondition = nextPreds.orElse(curPreds);
      AbstractionFormula computedPostCondition =
          buildAbstraction(
              pCfaEdge,
              callstackWrapper,
              abstractionFormula,
              pathFormula,
              ImmutableSet.of(precondition.getPredicate()));

      if (computedPostCondition.isTrue()) {
        // Abstraction formula is true; stay in the current (interpolation) state
        newStatePreds.put(entry);
        continue;
      }

      if (computedPostCondition.isFalse()) {
        throw new AssertionError(
            "Computed abstraction is <false>, a non-trivial result was however unexpected.");
      }

      // A non-trivial abstraction was computed
      if (precondition.getPredicate().getSymbolicAtom().equals(computedPostCondition.asFormula())) {
        // Pre- and postcondition are the same; keep current pred in TAState
        newStatePreds.put(currentItpSequence, precondition);
      } else if (precondition
          .getPredicate()
          .getSymbolicAtom()
          .equals(bFMgrView.not(computedPostCondition.asFormula()))) {
        // Pre- and postcondition are negated to each other. The Hoare triple is of the following
        // form: [x==0] x!=0 [x!=0]

        // The next state is hence unsatisfiable.
        logger.log(
            Level.FINEST,
            "Abstraction is contradictory to current input predicates. The node is not reachable");
        return ImmutableSet.of();
      } else {

        // The postcondition is ambiguous. We cannot tell anything about the next
        // state. For now an exception is thrown until this is handled accordingly to make it
        // visible in the log
        throw new AssertionError(
            "Interpolant <false> is following on an interpolant <true>. This is not yet handled");
      }
    }

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
              pCfaEdge,
              callstackWrapper,
              abstractionFormula,
              pathFormula,
              ImmutableSet.of(preconditionPreds));

      if (computedPostCondition.isTrue()) {
        // Abstraction formula yields result <true>; discard the current itpSequence in this state
        continue;
      }

      if (computedPostCondition.isFalse()) {
        // Abstraction is false, this node is infeasible.
        // TODO: this needs to be handled accordingly (i.e. return bottom state)
        // Until then an exception is thrown to make this visible in the log output
        throw new AssertionError(
            "Interpolant <false> is following on an interpolant <true>. This is not yet handled");
      }

      // A non-trivial abstraction was computed
      if (preconditionPreds.getSymbolicAtom().equals(computedPostCondition.asFormula())) {
        // Pre- and postcondition are the same; it is the first from the current interpolation
        // sequence
        newStatePreds.put(itpSequence, indexedPred);
      } else if (preconditionPreds
          .getSymbolicAtom()
          .equals(bFMgrView.not(computedPostCondition.asFormula()))) {
        // Pre- and postcondition are negated to each other.
        newStatePreds.put(itpSequence, indexedPred);
      } else {
        throw new AssertionError(
            String.format(
                "Precond: %s%nPostcond: %s",
                preconditionPreds.getSymbolicAtom(), computedPostCondition.asFormula()));
      }
    }

    ImmutableMap<InterpolationSequence, IndexedAbstractionPredicate> newPreds =
        newStatePreds.build();
    logger.logf(Level.FINER, "Active predicates in the next state: %s\n", newPreds);
    return ImmutableSet.of(new TraceAbstractionState(predSuccessorState, newPreds));
  }

  private TransferRelation getWrappedTR() {
    return Iterables.getOnlyElement(super.getWrappedTransferRelations());
  }

  private AbstractionFormula buildAbstraction(
      CFAEdge pCfaEdge,
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
              ImmutableSet.of(pCfaEdge.getPredecessor()),
              callstackWrapper,
              abstractionFormula,
              pathFormula,
              relevantPreds);

      logger.logf(Level.FINER, "New abstraction formula: %s\n", abstractionFormula);
      printHoareTriple(relevantPreds, abstractionResult);

      return abstractionResult;
    } catch (SolverException e) {
      throw new CPATransferException("Solver Failure: " + e.getMessage(), e);
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

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException(
        "The "
            + this.getClass().getSimpleName()
            + " expects to be called with a CFA edge supplied"
            + " and does not support configuration where it needs to"
            + " return abstract states for any CFA edge.");
  }
}
