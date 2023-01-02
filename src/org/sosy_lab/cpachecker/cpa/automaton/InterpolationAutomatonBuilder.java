// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class InterpolationAutomatonBuilder {

  private static final String ITP_AUTOMATON_NAME = "ItpAut";

  private final FormulaManagerView predFormulaManagerView;
  private final PredicateAbstractionManager predicateAbstractionManager;

  private final LogManager logger;
  private final FormulaManagerView fMgrView;
  private final BooleanFormulaManagerView bFMgrView;

  private final boolean showTransitionsInFinalState;

  public InterpolationAutomatonBuilder(
      FormulaManagerView pFormulaManagerView,
      LogManager pLogger,
      FormulaManagerView pPredFormulaManagerView,
      PredicateAbstractionManager pPredicateAbstractionManager,
      boolean pShowTransitionsInFinalState) {
    fMgrView = checkNotNull(pFormulaManagerView);
    bFMgrView = fMgrView.getBooleanFormulaManager();
    logger = checkNotNull(pLogger);
    predicateAbstractionManager = checkNotNull(pPredicateAbstractionManager);
    predFormulaManagerView = checkNotNull(pPredFormulaManagerView);

    showTransitionsInFinalState = pShowTransitionsInFinalState;
  }

  public InterpolationAutomaton buildAutomatonFromPath(
      ARGPath pPath, List<BooleanFormula> pInterpolants, int pAutomatonIndex) {
    checkArgument(pPath.asStatesList().size() == pInterpolants.size() + 1);

    ImmutableList<BooleanFormula> interpolants =
        ImmutableList.<BooleanFormula>builderWithExpectedSize(pInterpolants.size() + 1)
            .addAll(pInterpolants)
            .add(bFMgrView.makeFalse())
            .build()
            .stream()
            .map(fMgrView::uninstantiate)
            .collect(ImmutableList.toImmutableList());

    ImmutableList<BooleanFormula> distinctInterpolants =
        interpolants.stream()
            .filter(x -> !bFMgrView.isTrue(x))
            .filter(x -> !bFMgrView.isFalse(x))
            .distinct()
            .collect(ImmutableList.toImmutableList());

    logger.logf(
        Level.INFO,
        "Refining the arg with automaton using the interpolants: %s",
        distinctInterpolants);

    String automatonName = ITP_AUTOMATON_NAME + pAutomatonIndex;

    UnmodifiableIterator<ARGState> stateIterator = pPath.asStatesList().iterator();
    Iterator<BooleanFormula> itpIterator = interpolants.iterator();
    ARGState curState = stateIterator.next();
    BooleanFormula curInterpolant = itpIterator.next();

    InterpolationAutomaton itpAutomaton =
        new InterpolationAutomaton(fMgrView, automatonName, distinctInterpolants);

    while (stateIterator.hasNext() && itpIterator.hasNext()) {
      ARGState childState = stateIterator.next();
      BooleanFormula childInterpolant = itpIterator.next();
      if (!curInterpolant.equals(childInterpolant)
          || (showTransitionsInFinalState && bFMgrView.isFalse(curInterpolant))) {
        itpAutomaton.addTransition(curState, curInterpolant, childState, childInterpolant);
      } else {
        itpAutomaton.addQueryToCache(curState, childState, curInterpolant);
      }

      curState = childState;
      curInterpolant = childInterpolant;
    }

    verify(!stateIterator.hasNext() && !itpIterator.hasNext());

    return itpAutomaton;
  }

  public void addAdditionalTransitions(
      InterpolationAutomaton pItpAutomaton, ARGPath pPath, List<BooleanFormula> pInterpolants)
      throws InterruptedException, CPAException {

    ImmutableList<BooleanFormula> distinctInterpolants = pItpAutomaton.getDistinctInterpolants();
    if (distinctInterpolants.size() > 1) {
      throw new UnsupportedOperationException(
          "Automata with more than one interpolation state are not yet supported.");
    }

    BooleanFormula interpolant = Iterables.getOnlyElement(distinctInterpolants);
    ImmutableList<AbstractionPredicate> predicates =
        ImmutableList.of(predicateAbstractionManager.getPredicateFor(interpolant));

    try {
      verify(pPath.asStatesList().size() == pInterpolants.size() + 1);

      PathIterator pathIterator = pPath.fullPathIterator();
      Iterator<BooleanFormula> itpIterator = pInterpolants.iterator();

      while (pathIterator.hasNext() && itpIterator.hasNext()) {

        BooleanFormula curItp = fMgrView.uninstantiate(itpIterator.next());
        Set<String> predVariables = fMgrView.extractVariableNames(curItp);

        ARGState currentState = pathIterator.getAbstractState();
        if (currentState.getChildren().size() > 1) {
          exploreChildren(pItpAutomaton, currentState, predicates, curItp, predVariables);
        }

        pathIterator.advance();
      }

    } catch (SolverException e) {
      throw new CPAException(e.getMessage(), e);
    }
  }

  private void exploreChildren(
      InterpolationAutomaton pItpAutomaton,
      ARGState pRootState,
      ImmutableList<AbstractionPredicate> pPredicates,
      BooleanFormula pCurrentInterpolant,
      Set<String> pPredVariables)
      throws SolverException, InterruptedException {
    ARGState currentState = pRootState;
    Collection<ARGState> children = currentState.getChildren();

    for (ARGState childState : children) {
      if (pItpAutomaton.isStateCovered(currentState, childState, pCurrentInterpolant)) {
        continue;
      }

      Optional<BooleanFormula> result =
          handleChild(
              pItpAutomaton,
              currentState,
              childState,
              pPredicates,
              pCurrentInterpolant,
              pPredVariables);
      if (result.isPresent()) {
        // The children states of this child have not been handled yet.
        // Continuing next to recursively explore on them
        exploreChildren(
            pItpAutomaton, childState, pPredicates, result.orElseThrow(), pPredVariables);
      }
    }
  }

  private Optional<BooleanFormula> handleChild(
      InterpolationAutomaton pItpAutomaton,
      ARGState pCurrentState,
      ARGState pChildState,
      ImmutableList<AbstractionPredicate> pPredicates,
      BooleanFormula pCurrentInterpolant,
      Set<String> pPredVariables)
      throws SolverException, InterruptedException {

    CFANode locationNode = AbstractStates.extractLocation(pChildState);
    logger.logf(
        Level.FINE,
        "Checking Node: %s",
        MoreStrings.lazyString(() -> pChildState.getStateId() + ":" + locationNode));

    PredicateAbstractState predChildState = PredicateAbstractState.getPredicateState(pChildState);
    PathFormula pathFormula = predChildState.getAbstractionFormula().getBlockFormula();
    BooleanFormula translatedFormula =
        fMgrView.translateFrom(pathFormula.getFormula(), predFormulaManagerView);
    pathFormula = pathFormula.withFormula(translatedFormula);

    AbstractionFormula abstractionResult =
        predicateAbstractionManager.buildAbstraction(
            locationNode, Optional.empty(), pathFormula.getFormula(), pathFormula, pPredicates);

    logger.logf(Level.FINE, "Current Itp: %s", pCurrentInterpolant);

    printHoareTriple(pPredicates, abstractionResult);

    // checking the validity of Hoare-triples subsequently:

    if (abstractionResult.isTrue()) {
      // Formula is true; stay in the current interpolation state
      // (i.e., true-state -> true-state, itp-state -> itp-state, or false-state -> false state)
      pItpAutomaton.addQueryToCache(pCurrentState, pChildState, pCurrentInterpolant);
      return Optional.of(pCurrentInterpolant);
    }

    PredicateAbstractState predicateParentState =
        PredicateAbstractState.getPredicateState(pCurrentState);
    SSAMap ssaParent = predicateParentState.getPathFormula().getSsa();
    SSAMap ssaChild = predChildState.getPathFormula().getSsa();
    logger.logf(Level.FINE, "SSA-Maps: Parent-node %s, Child-node: %s     ", ssaParent, ssaChild);

    if (anyVariableModified(ssaParent, ssaChild, pPredVariables)) {
      // Predicates do not share the same SSAs, i.e. a new assignment was made.
      // Depending on the current state, go either back to the init state (for
      // itp-states), or stay in the final-state (if current state is already
      // the final-state)
      if (bFMgrView.isFalse(pCurrentInterpolant)) {
        pItpAutomaton.addTransitionToFinalState(pCurrentState, pCurrentInterpolant, pChildState);
        return Optional.empty();
      } else if (!bFMgrView.isTrue(pCurrentInterpolant)) {
        pItpAutomaton.addTransitionToInitState(pCurrentState, pCurrentInterpolant, pChildState);
      } else {
        pItpAutomaton.addQueryToCache(pCurrentState, pChildState, pCurrentInterpolant);
      }

      return Optional.of(bFMgrView.makeTrue());
    }

    AbstractionPredicate preCondition = Iterables.getOnlyElement(pPredicates);
    BooleanFormula postCondition =
        fMgrView.uninstantiate(abstractionResult.asInstantiatedFormula());
    if (preCondition.getSymbolicAtom().equals(postCondition)) {
      // Pre- and postcondition are the same
      // -> when in the init-state, go to itp state and keep on exploring the
      // children states. Otherwise, stay in the current state.
      verify(pItpAutomaton.getDistinctInterpolants().contains(postCondition));

      pItpAutomaton.addTransition(pCurrentState, pCurrentInterpolant, pChildState, postCondition);
      return Optional.of(postCondition);

    } else if (preCondition.getSymbolicAtom().equals(bFMgrView.not(postCondition))) {
      // Pre- and postcondition are negated to each other.
      // The Hoare triple is of the following form:
      // [x=0] x==0 [x!=0]

      if (bFMgrView.isTrue(pCurrentInterpolant)) {

        // If in true-state, stay in it and keep on exploring
        pItpAutomaton.addQueryToCache(pCurrentState, pChildState, pCurrentInterpolant);
        return Optional.of(pCurrentInterpolant);

      } else {

        // Otherwise (if in itp-state), the next state is unsat to the current
        // one. Hence add a transition to the final state.
        pItpAutomaton.addTransitionToFinalState(pCurrentState, pCurrentInterpolant, pChildState);
        return Optional.empty();
      }
    } else {

      // The postcondition is ambiguous. We cannot tell anything about the child
      // state, hence a transition to the init state is added (if not already in
      // that state).

      if (bFMgrView.isTrue(pCurrentInterpolant)) {
        pItpAutomaton.addQueryToCache(pCurrentState, pChildState, pCurrentInterpolant);
        return Optional.of(pCurrentInterpolant);
      }

      pItpAutomaton.addTransitionToInitState(pCurrentState, pCurrentInterpolant, pChildState);
      logger.log(
          Level.INFO,
          "InterpolationAutomatonBuilder: Abstraction formula has an ambiguous postcondition.");

      return Optional.of(bFMgrView.makeTrue());
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

  private void printHoareTriple(
      ImmutableList<AbstractionPredicate> pPredicates, AbstractionFormula resultFormula) {
    Object prettyPrintPredicateList =
        MoreStrings.lazyString(
            () ->
                FluentIterable.from(pPredicates)
                    .transform(x -> x.getSymbolicAtom())
                    .join(Joiner.on(", ")));
    logger.logf(
        Level.FINE,
        "[%s] --- %s --- %s",
        prettyPrintPredicateList,
        resultFormula.getBlockFormula(),
        resultFormula.asInstantiatedFormula());
  }
}
