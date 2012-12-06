/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.ImpactUtils.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;


public class ImpactGlobalRefiner<T> implements Refiner {

  private final LogManager logger;

  private FormulaManager fmgr;
  private Solver solver;
  private InterpolatingTheoremProver<T> itpProver;
  private ARGCPA argCpa;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static ImpactGlobalRefiner<?> create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(ImpactRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    Region initialRegion = predicateCpa.getInitialState(null).getAbstractionFormula().asRegion();
    if (!(initialRegion instanceof SymbolicRegionManager.SymbolicRegion)) {
      throw new InvalidConfigurationException(ImpactGlobalRefiner.class.getSimpleName() + " works only with a PredicateCPA configured to store abstractions as formulas (cpa.predicate.abstraction.type=FORMULA)");
    }

    return new ImpactGlobalRefiner(predicateCpa.getConfiguration(),
                                    predicateCpa.getLogger(),
                                    (ARGCPA)pCpa,
                                    predicateCpa.getFormulaManager(),
                                    predicateCpa.getSolver(),
                                    predicateCpa.getFormulaManagerFactory().createInterpolatingTheoremProver(false));
  }

  private ImpactGlobalRefiner(Configuration config, LogManager pLogger,
      ARGCPA pArgCpa, FormulaManager pFmgr, Solver pSolver,
      InterpolatingTheoremProver<T> pItpProver) {

    logger = pLogger;
    argCpa = pArgCpa;
    fmgr = pFmgr;
    solver = pSolver;
    itpProver = pItpProver;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    List<AbstractState> targets = from(pReached)
        .filter(AbstractStates.IS_TARGET_STATE)
        .toImmutableList();
    assert !targets.isEmpty();

    do {
      boolean successful = performRefinement0(pReached, targets);

      if (!successful) {
        return false;
      }

      // there might be target states which were previously covered
      // and are now uncovered

      targets = from(pReached)
          .filter(AbstractStates.IS_TARGET_STATE)
          .toImmutableList();

    } while (!targets.isEmpty());

    return true;
  }

  private boolean performRefinement0(ReachedSet pReached, List <AbstractState> targets) throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Starting refinement for", targets.size(), "elements.");

    Map<ARGState, ARGState> predecessors = Maps.newHashMap();
    SetMultimap<ARGState, ARGState> successors = HashMultimap.create();

    Deque<AbstractState> todo = new ArrayDeque<AbstractState>(targets);

    while (!todo.isEmpty()) {
      final ARGState currentAbstractionState = (ARGState)todo.removeFirst();
      assert currentAbstractionState.mayCover();

      ARGState currentState = currentAbstractionState;
      do {
        currentState = currentState.getParents().iterator().next();
      } while (!extractStateByType(currentState, PredicateAbstractState.class).isAbstractionState());

      if (!currentState.getParents().isEmpty()
          && !predecessors.containsKey(currentState)) {
        todo.add(currentState);
      }

      predecessors.put(currentAbstractionState, currentState);
      successors.put(currentState, currentAbstractionState);
    }
    final ARGState root = (ARGState)pReached.getFirstState();
    assert successors.containsKey(root);

    // Now predecessors/successors contains all abstraction states on all error
    // paths and their relations.
    // These states and the relation form a tree.
    // We now iterate through this tree in a depth-first order.
    // For each state, we check reachability.
    // We do not descend beyond unreachable states,
    // but instead perform refinement on them.

    itpProver.init();
    List<T> itpStack = new ArrayList<T>();
    boolean successful = step(root, itpStack, successors, predecessors, pReached, targets);
    assert itpStack.isEmpty();
    itpProver.reset();

    return successful;
  }

  private boolean step(ARGState current, List<T> itpStack, SetMultimap<ARGState, ARGState> successors,
      Map<ARGState, ARGState> predecessors, ReachedSet pReached, List<AbstractState> targets)
      throws InterruptedException, CPAException {

    for (ARGState succ : successors.get(current)) {
      assert succ.getChildren().isEmpty() == targets.contains(succ);
      assert succ.mayCover();

      Formula blockFormula = extractStateByType(succ, PredicateAbstractState.class).getAbstractionFormula().getBlockFormula();
      itpStack.add(itpProver.addFormula(blockFormula));
      try {
        if (itpProver.isUnsat()) {
          logger.log(Level.FINE, "Found unreachable state", succ);
          performRefinement(Lists.newArrayList(itpStack), succ, predecessors, pReached);

        } else if (targets.contains(succ)) {
          // We have found a reachable target state, immediately abort refinement.
          logger.log(Level.FINE, "Found reachable target state", succ);
          return false;

        } else {
          // Not yet infeasible, but path is longer,
          // so descend recursively.
          boolean successful = step(succ, itpStack, successors, predecessors, pReached, targets);

          if (!successful) {
            return false;
          }
        }

        if (!current.mayCover()) {
          // The refinement along the current path made the current part of the ARG covered.
          // This may happens if some state up in the path was strengthened with
          // an interpolant and is now covered.
          // In this case, we do not need to do anything further in this part.
          break;
        }
      } finally {
        itpStack.remove(itpStack.size()-1);
        itpProver.popFormula();
      }
    }
    return true;
  }

  /**
   * Actually perform refinement, i.e., strengthen states by adding interpolants
   * to their state formulas.
   * @param itpStack The list with the interpolation groups.
   * @param unreachableState The first state in the path which is infeasible.
   * @param predecessors The predecessor relation of abstraction states.
   * @param reached The reached set.
   * @throws CPAException
   */
  private void performRefinement(List<T> itpStack, final ARGState unreachableState,
      Map<ARGState, ARGState> predecessors, ReachedSet reached) throws CPAException {
    assert !itpStack.isEmpty();
    assert itpProver.getInterpolant(itpStack).isFalse(); // last interpolant is False

    List<ARGState> strengthenedStates = Lists.newArrayList();

    // going upwards from unreachableState strengthening states with interpolants
    ARGState currentState = unreachableState;
    do {
      itpStack.remove(itpStack.size()-1); // remove last
      currentState = predecessors.get(currentState);
      assert itpStack.isEmpty() == currentState.getParents().isEmpty();

      Formula currentItp = fmgr.uninstantiate(itpProver.getInterpolant(itpStack));

      if (currentItp.isTrue()) {
        // from here to the ARG root, all interpolants will be True
        break;
      }

      Formula stateFormula = getStateFormula(currentState);

      boolean isNewItp = !solver.implies(stateFormula, currentItp);

      if (isNewItp) {
        addFormulaToState(currentItp, currentState, fmgr);
        strengthenedStates.add(currentState);
      } else {
        // If the currentItp is implied by the stateFormula,
        // then we don't need any of the interpolants between the ARG root
        // and this state.
        break;
      }

    } while (!itpStack.isEmpty());

    ARGReachedSet arg = new ARGReachedSet(reached);

    for (ARGState w : strengthenedStates) {
      arg.removeCoverageOf(w);
    }

    // remove ARG part from unreachableState downwards
    removeInfeasiblePartofARG(unreachableState, arg);


    for (ARGState w : Lists.reverse(strengthenedStates)) {
      if (cover(w, arg, argCpa)) {
        break; // all further elements are covered anyway
      }
    }
  }
}