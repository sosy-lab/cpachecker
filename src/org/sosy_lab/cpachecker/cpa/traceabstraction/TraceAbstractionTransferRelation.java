// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.SolverException;

public class TraceAbstractionTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final InterpolationSequenceStorage itpSequenceStorage;

  private boolean componentsInitialized = false;

  // Lazy instantiation, as these objects are taken from the PredicateCPA, which itself might not be
  // available at the instantiation of this class.
  private PredicateAbstractionManager predicateAbstractionManager;
  private BooleanFormulaManagerView bFMgrView;

  public TraceAbstractionTransferRelation(
      InterpolationSequenceStorage pItpSequenceStorage,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    itpSequenceStorage = pItpSequenceStorage;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    // We need to wait for more information from other CPA-states before
    // the correct successor state can computed
    return ImmutableList.of(pState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    if (!componentsInitialized) {
      initializeComponentsOnce();
      componentsInitialized = true;
    }

    TraceAbstractionState state = (TraceAbstractionState) pState;

    if (itpSequenceStorage.isEmpty() && !state.containsPredicates()) {
      // The predecessor states have not yet been part of a refinement.
      // There are hence no predicates available for further processing.
      return ImmutableList.of(state);
    }

    Optional<CallstackStateEqualsWrapper> callstackWrapper = Optional.empty();

    AbstractionFormula abstractionFormula = null;
    PathFormula pathFormula = null;

    for (AbstractState otherState : pOtherStates) {

      if (otherState instanceof CallstackState) {
        CallstackState callstack = (CallstackState) otherState;
        callstackWrapper = Optional.of(new CallstackStateEqualsWrapper(callstack));
      }

      if (otherState instanceof PredicateAbstractState) {
        PredicateAbstractState predState = (PredicateAbstractState) otherState;
        if (predState.getAbstractionFormula().isFalse()) {
          logger.log(Level.INFO, "Abstraction formula from predState is >false<");
          // TODO: PredicateTR has already computed the abstraction formula to be false.
          // In this case we likely don't need to compute an abstraction and can
          // immediately return bottom (represented by an empty set)
          //  return ImmutableList.of();
        }

        abstractionFormula = predState.getAbstractionFormula();
        pathFormula = predState.getPathFormula();
      }
    }

    verifyNotNull(pCfaEdge, "cfaEdge may not be null.");
    verifyNotNull(abstractionFormula, "AbstractionFormula may not be null.");
    verifyNotNull(pathFormula, "PathFormula may not be null.");

    logger.logf(
        Level.FINER,
        "Taking edge: N%s -> N%s // %s\n",
        pCfaEdge.getPredecessor().getNodeNumber(),
        pCfaEdge.getSuccessor().getNodeNumber(),
        pCfaEdge.getDescription());

    ImmutableMap.Builder<InterpolationSequence, AbstractionPredicate> newStatePreds =
        ImmutableMap.builder();

    // TAStates contain only predicates that actually hold in that state
    ImmutableMap<InterpolationSequence, AbstractionPredicate> statePredicates =
        state.getActivePredicates();

    // TODO: correctly handle the actual location instance.
    // This can be ignored for now, as currently only function-predicates are considered
    // (more specifically, the 'locationInstancePredicates' are not considered yet).
    LocationInstance locInstance =
        new PredicatePrecision.LocationInstance(pCfaEdge.getPredecessor(), 0);

    logger.log(Level.FINER, "Computing abstractions");
    for (Entry<InterpolationSequence, AbstractionPredicate> entry : statePredicates.entrySet()) {
      // TODO: check the next predicate in addition to the current one if it is non-trivial
      // TODO: remove interpolation sequences from states once they leave their scopes
      //       (e.g., remove function predicates from the current TAState when the function
      //        changes)
      AbstractionPredicate preconditionPreds = entry.getValue();

      AbstractionFormula computedPostCondition =
          buildAbstraction(
              pCfaEdge,
              callstackWrapper,
              abstractionFormula,
              pathFormula,
              ImmutableSet.of(preconditionPreds));

      if (computedPostCondition.isTrue()) {
        // Abstraction formula is true; stay in the current (interpolation) state
        newStatePreds.put(entry);
        continue;
      }

      if (computedPostCondition.isFalse()) {
        // Abstraction is false, return bottom (represented by empty set)
        logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
        return ImmutableSet.of();
      }

      // A non-trivial abstraction was computed
      if (preconditionPreds.getSymbolicAtom().equals(computedPostCondition.asFormula())) {
        // Pre- and postcondition are the same; keep current pred in TAState
        newStatePreds.put(entry.getKey(), preconditionPreds);
      } else if (preconditionPreds
          .getSymbolicAtom()
          .equals(bFMgrView.not(computedPostCondition.asFormula()))) {
        // Pre- and postcondition are negated to each other. The Hoare triple is of the following
        // form: [x=0] x==0 [x!=0]

        // The next state is hence unsatisfiable.
        logger.log(
            Level.FINEST,
            "Abstraction is contradictory to the given input preds; node is not reachable");
        return ImmutableSet.of();
      } else {

        // The postcondition is ambiguous. We cannot tell anything about the next
        // state. For now an exception is thrown until this is handled accordingly to make it
        // visible in the log
        throw new UnsupportedCodeException(
            "Interpolant <false> is following on an interpolant <true>. This is not yet handled",
            pCfaEdge);
      }
    }

    for (InterpolationSequence itpSequence :
        itpSequenceStorage.difference(statePredicates.keySet())) {
      AbstractionPredicate preconditionPreds = itpSequence.getFirst(locInstance);

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
        // TODO: this needs to be handled accordingly; until then an exception is thrown to make
        // this visible in the log output
        throw new UnsupportedCodeException(
            "Interpolant <false> is following on an interpolant <true>. This is not yet handled",
            pCfaEdge);
      }

      // A non-trivial abstraction was computed
      if (preconditionPreds.getSymbolicAtom().equals(computedPostCondition.asFormula())) {
        // Pre- and postcondition are the same; it is the first from the current interpolation
        // sequence
        newStatePreds.put(itpSequence, preconditionPreds);
      }
    }

    ImmutableMap<InterpolationSequence, AbstractionPredicate> newPreds = newStatePreds.build();
    logger.logf(Level.FINER, "Active predicates in the next state: %s\n", newPreds);
    return ImmutableSet.of(new TraceAbstractionState(newPreds));
  }

  @SuppressWarnings("resource")
  private void initializeComponentsOnce() throws CPATransferException {
    try {
      PredicateCPA predCPA =
          CPAs.retrieveCPAOrFail(
              GlobalInfo.getInstance().getCPA().orElseThrow(),
              PredicateCPA.class,
              TraceAbstractionTransferRelation.class);

      predicateAbstractionManager = predCPA.getPredicateManager();
      bFMgrView = predCPA.getSolver().getFormulaManager().getBooleanFormulaManager();
    } catch (InvalidConfigurationException e) {
      throw new CPATransferException(
          "Could not retrieve PredicateAbstractionManager: " + e.getMessage(), e);
    }
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
}
