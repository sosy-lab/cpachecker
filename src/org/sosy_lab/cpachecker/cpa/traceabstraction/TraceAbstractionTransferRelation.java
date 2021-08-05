// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class TraceAbstractionTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final TraceAbstractionPredicatesStorage predicatesStorage;

  // Lazily instantiated, as this object is taken over from the PredicateCPA,
  // which might not be instantiated yet
  private PredicateAbstractionManager predicateAbstractionManager;

  public TraceAbstractionTransferRelation(
      TraceAbstractionPredicatesStorage pPredicatesStorage,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    predicatesStorage = pPredicatesStorage;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    // we need to wait for more information from other CPA-states before
    // we know what successor state needs to be computed
    return ImmutableList.of(pState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    TraceAbstractionState state = (TraceAbstractionState) pState;

    ImmutableMultimap<String, AbstractionPredicate> availablePredicates =
        predicatesStorage.getPredicates();
    ImmutableMultimap<String, AbstractionPredicate> previousStatePredicates =
        state.getFunctionPredicates();

    if (availablePredicates.isEmpty() && previousStatePredicates.isEmpty()) {
      // The predecessor states have not yet been a part of a refinement.
      // No predicates are hence available for further processing.
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
          // TODO: in this case we probably don't need to compute an abstraction
          //          return ImmutableList.of();
        }

        abstractionFormula = predState.getAbstractionFormula();
        pathFormula = predState.getPathFormula();
      }
    }

    verifyNotNull(abstractionFormula, "AbstractionFormula may not be null.");
    verifyNotNull(pathFormula, "PathFormula may not be null.");

    String functionName = pCfaEdge.getPredecessor().getFunctionName();
    Collection<AbstractionPredicate> relevantPreds = availablePredicates.get(functionName);

    logger.logf(
        Level.FINEST,
        "Taking edge: N%s -> N%s // %s\n",
        pCfaEdge.getPredecessor().getNodeNumber(),
        pCfaEdge.getSuccessor().getNodeNumber(),
        pCfaEdge.getDescription());

    logger.logf(Level.FINEST, "Current function predicates: %s\n", relevantPreds);

    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.FINE, "Computing an abstraction");
    // compute a new abstraction with a precision based on `preds`
    AbstractionFormula newAbstractionFormula;
    try {
      newAbstractionFormula =
          getPredicateAbstractionManager()
              .buildAbstraction(
                  ImmutableSet.of(pCfaEdge.getPredecessor()),
                  callstackWrapper,
                  abstractionFormula,
                  pathFormula,
                  relevantPreds);
      logger.logf(Level.FINER, "New abstraction formula: %s\n", newAbstractionFormula);
    } catch (SolverException e) {
      throw new CPATransferException("Solver Failure: " + e.getMessage(), e);
    }

    if (newAbstractionFormula.isTrue()) {
      // Formula is true; stay in the current (interpolation) state
      return ImmutableList.of(state);
    }

    // if the abstraction is false, return bottom (represented by empty set)
    if (newAbstractionFormula.isFalse()) {
      logger.log(Level.FINEST, "Abstraction is false, node is not reachable");
      return ImmutableSet.of();
    }

    // TODO: add predicates to the strengthened state and include them in all successive states.

    return ImmutableList.of(state);
  }

  @SuppressWarnings("resource")
  private PredicateAbstractionManager getPredicateAbstractionManager() throws CPATransferException {
    if (predicateAbstractionManager == null) {
      try {
        PredicateCPA predCPA =
            CPAs.retrieveCPAOrFail(
                GlobalInfo.getInstance().getCPA().orElseThrow(),
                PredicateCPA.class,
                TraceAbstractionCPA.class);
        predicateAbstractionManager = predCPA.getPredicateManager();
      } catch (InvalidConfigurationException e) {
        throw new CPATransferException(
            "Could not retrieve PredicateAbstractionManager: " + e.getMessage(), e);
      }
    }
    return predicateAbstractionManager;
  }
}
