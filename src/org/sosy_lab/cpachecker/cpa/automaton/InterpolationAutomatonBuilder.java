/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.dca.DCARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class InterpolationAutomatonBuilder {

  private static final String ITP_AUTOMATON_NAME = "ItpAut";

  private final DCARefiner refiner;
  private final FormulaManagerView predFormulaManagerView;

  private final LogManager logger;
  private final FormulaManagerView formulaManagerView;

  public InterpolationAutomatonBuilder(
      DCARefiner pRefiner,
      FormulaManagerView pFormulaManagerView,
      LogManager pLogger,
      FormulaManagerView pPredFormulaManagerView) {
    refiner = checkNotNull(pRefiner);
    formulaManagerView = checkNotNull(pFormulaManagerView);
    logger = checkNotNull(pLogger);
    predFormulaManagerView = checkNotNull(pPredFormulaManagerView);
  }

  public InterpolationAutomaton buildAutomatonFromPath(
      ARGPath pPath, List<BooleanFormula> pInterpolants, int automatonIndex) {
    checkArgument(pPath.asStatesList().size() == pInterpolants.size() + 1);

    ImmutableList<BooleanFormula> distinctInterpolants =
        pInterpolants
            .stream()
            .filter(x -> !formulaManagerView.getBooleanFormulaManager().isTrue(x))
            .filter(x -> !formulaManagerView.getBooleanFormulaManager().isFalse(x))
            .distinct()
            .collect(ImmutableList.toImmutableList());

    logger.logf(
        Level.SEVERE,
        "Refining the arg with automaton using interpolant: %s",
        distinctInterpolants);

    ImmutableList<BooleanFormula> interpolants =
        ImmutableList.<BooleanFormula>builder()
            .addAll(pInterpolants)
            .add(formulaManagerView.getBooleanFormulaManager().makeFalse())
            .build();

    String automatonName = ITP_AUTOMATON_NAME + automatonIndex;

    UnmodifiableIterator<ARGState> stateIterator = pPath.asStatesList().iterator();
    Iterator<BooleanFormula> itpIterator = interpolants.iterator();
    ARGState curState = stateIterator.next();
    BooleanFormula curInterpolant = itpIterator.next();

    InterpolationAutomaton itpAutomaton =
        new InterpolationAutomaton(formulaManagerView, automatonName, distinctInterpolants);

    while (stateIterator.hasNext() && itpIterator.hasNext()) {
      ARGState childState = stateIterator.next();
      BooleanFormula childInterpolant = itpIterator.next();
      itpAutomaton.addTransition(curState, curInterpolant, childState, childInterpolant);

      curState = childState;
      curInterpolant = childInterpolant;
    }

    verify(!stateIterator.hasNext() && !itpIterator.hasNext());

    return itpAutomaton;
  }

  public void addAdditionalTransitions(
      InterpolationAutomaton pItpAutomaton, ARGPath pPath, List<BooleanFormula> pPathFormulaList)
      throws InterruptedException, CPAException {

    ImmutableList<BooleanFormula> distinctInterpolants = pItpAutomaton.getDistinctInterpolants();
    if (distinctInterpolants.size() > 1) {
      throw new UnsupportedOperationException(
          "Automata with more than one interpolation state are not yet supported.");
    }
    BooleanFormula interpolant = Iterables.getOnlyElement(distinctInterpolants);
    Set<String> predVariables = formulaManagerView.extractVariableNames(interpolant);
    interpolant = formulaManagerView.uninstantiate(interpolant);

    ImmutableList<AbstractionPredicate> predicates = refiner.makePredicates(interpolant);

    try {
      Set<ARGState> coveredStates = new LinkedHashSet<>(pPath.asStatesList());

      verify(pPath.asStatesList().size() == pPathFormulaList.size() + 1);
      PathIterator pathIterator = pPath.fullPathIterator();
      Iterator<BooleanFormula> formulaIterator = pPathFormulaList.iterator();
      while (pathIterator.hasNext() && formulaIterator.hasNext()) {
        BooleanFormula curItp = formulaIterator.next();
        //        if (formulaManagerView.getBooleanFormulaManager().isFalse(curItp)) {
        //          break;
        //        }

        ARGState currentState = pathIterator.getAbstractState();
        if (currentState.getChildren().size() > 1) {
          exploreChildren(
              pItpAutomaton, currentState, coveredStates, predicates, predVariables, curItp);
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
      Set<ARGState> pCoveredStates,
      ImmutableList<AbstractionPredicate> pPredicates,
      Set<String> pPredVariables,
      BooleanFormula pCurrentInterpolant)
      throws SolverException, InterruptedException {
    ARGState currentState = pRootState;
    Collection<ARGState> children = currentState.getChildren();

    for (ARGState childState : children) {
      if (pCoveredStates.contains(childState)) {
        continue;
      }

      pCoveredStates.add(childState);
      Optional<BooleanFormula> bfOpt =
          handleChild(
              pItpAutomaton,
              currentState,
              childState,
              pPredicates,
              pPredVariables,
              pCurrentInterpolant);
      if (bfOpt.isPresent()) {
        // The children states of this child have not been handled yet.
        // Continue to recursively explore on them
        exploreChildren(
            pItpAutomaton,
            childState,
            pCoveredStates,
            pPredicates,
            pPredVariables,
            bfOpt.orElseThrow());
      }
    }
  }

  private Optional<BooleanFormula> handleChild(
      InterpolationAutomaton pItpAutomaton,
      ARGState pCurrentState,
      ARGState pChildState,
      ImmutableList<AbstractionPredicate> pPredicates,
      Set<String> pPredVariables,
      BooleanFormula pCurrentInterpolant)
      throws SolverException, InterruptedException {

    CFANode locationNode = AbstractStates.extractLocation(pChildState);
    logger.logf(
        Level.INFO,
        "Checking Node: %s",
        MoreStrings.lazyString(() -> pChildState.getStateId() + ":" + locationNode));

    PredicateAbstractState predicateState = PredicateAbstractState.getPredicateState(pChildState);
    PathFormula pathFormula = predicateState.getAbstractionFormula().getBlockFormula();
    BooleanFormula translateFrom =
        formulaManagerView.translateFrom(pathFormula.getFormula(), predFormulaManagerView);
    pathFormula = pathFormula.updateFormula(translateFrom);

    AbstractionFormula resultFormula =
        refiner
            .getPredicateAbstractionManager()
            .buildAbstraction(
                locationNode, Optional.empty(), pathFormula.getFormula(), pathFormula, pPredicates);

    printAbstractionFormula(pPredicates, resultFormula);

    if (resultFormula.isTrue()) {
      pItpAutomaton.addTransition(
          pCurrentState, pCurrentInterpolant, pChildState, pCurrentInterpolant);
      // Formula is true; stay in the current interpolation state
      // (i.e., true-state -> true-state, itp-state -> itp-state, or false-state -> false state)
      return Optional.of(pCurrentInterpolant);
    }

    Set<String> resultVariables =
        formulaManagerView.extractVariableNames(resultFormula.asInstantiatedFormula());
    SetView<String> intersection = Sets.intersection(pPredVariables, resultVariables);
    if (intersection.isEmpty()) {
      // Predicates do not share the same SSAs.
      // Depending on the current itp state, go either into the sink state (for init-, itp-states),
      // or stay in the final-state (if current itp-state is already the final-state)
      if (formulaManagerView.getBooleanFormulaManager().isFalse(pCurrentInterpolant)) {
        pItpAutomaton.addTransitionToFinalState(pCurrentState, pCurrentInterpolant, pChildState);
      } else {
        pItpAutomaton.addTransitionToSinkState(pCurrentState, pCurrentInterpolant, pChildState);
      }
      return Optional.empty();
    }

    if (!resultFormula.asInstantiatedFormula().equals(pathFormula.getFormula())) {
      boolean unsat =
          refiner.isUnsat(
              formulaManagerView
                  .getBooleanFormulaManager()
                  .not(resultFormula.asInstantiatedFormula()),
              resultFormula.getBlockFormula().getFormula());
      if (unsat) {
        // Result formula is unsat. Going to error-state (independent from source-state)
        pItpAutomaton.addTransitionToFinalState(pCurrentState, pCurrentInterpolant, pChildState);
        return Optional.empty();
      } else {
        throw new UnsupportedOperationException(
            "InterpolationAutomatonBuilder has reached a state that has not yet been handled");
      }
    } else {
      boolean unsat =
          refiner.isUnsat(
              resultFormula.asInstantiatedFormula(), resultFormula.getBlockFormula().getFormula());
      if (unsat) {
        // Result formula is unsat. Going to error-state (independent from source-state)
        pItpAutomaton.addTransitionToFinalState(pCurrentState, pCurrentInterpolant, pChildState);
        return Optional.empty();
      } else {
        // TODO:
        //        // Going from current itp state into the next
        //        pItpAutomaton.addTransition(
        //            pCurrentState, pCurrentInterpolant, pChildState,
        // resultFormula.asInstantiatedFormula());
        //        // return the updated interpolant
        //        return Optional.of(resultFormula.asInstantiatedFormula());
        return Optional.empty();
      }
    }
  }

  // For debug only; will be removed eventually
  private void printAbstractionFormula(
      ImmutableList<AbstractionPredicate> pPredicates, AbstractionFormula resultFormula) {
    ImmutableList<BooleanFormula> prettyPrintPredicateList =
        Collections3.transformedImmutableListCopy(pPredicates, x -> x.getSymbolicAtom());
    //    logger.logf(
    //        Level.INFO,
    //        "ResultFormula: %s %n ResultFormula as Formula: %s %n "
    //            + "InstantiatedFormula: %s %n isFalse: %s %n isTrue: %s %n Predicates: %s %n "
    //            + "BlockFormula: %s %n Region: %s",
    //        resultFormula,
    //        resultFormula.asFormula(),
    //        resultFormula.asInstantiatedFormula(),
    //        resultFormula.isFalse(),
    //        resultFormula.isTrue(),
    //        prettyPrintPredicateList,
    //        resultFormula.getBlockFormula(),
    //        resultFormula.asRegion());
    logger.logf(
        Level.INFO,
        "%s --- %s --- %s",
        prettyPrintPredicateList,
        resultFormula.getBlockFormula(),
        resultFormula.asInstantiatedFormula());
  }
}
