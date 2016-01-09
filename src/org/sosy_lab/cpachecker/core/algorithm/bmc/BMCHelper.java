/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;


final class BMCHelper {

  private BMCHelper() {

  }

  public static Iterable<BooleanFormula> assertAt(Iterable<AbstractState> pStates, final BooleanFormula pUninstantiatedFormula, final FormulaManagerView pFMGR) {
    return from(pStates).transform(new Function<AbstractState, BooleanFormula>() {

      @Override
      public BooleanFormula apply(AbstractState pInput) {
        return assertAt(pInput, pUninstantiatedFormula, pFMGR, 1);
      }

    });
  }

  public static Iterable<BooleanFormula> assertAtWithIncrementingDefaultIndex(Iterable<AbstractState> pStates, final BooleanFormula pUninstantiatedFormula, final FormulaManagerView pFMGR, int firstDefault) {
    ImmutableSet.Builder<BooleanFormula> formulas = ImmutableSet.builder();
    int defaultIndex = firstDefault;
    for (AbstractState state : pStates) {
      formulas.add(assertAt(state, pUninstantiatedFormula, pFMGR, defaultIndex));
      ++defaultIndex;
    }
    return formulas.build();
  }

  public static BooleanFormula assertAt(AbstractState pState, BooleanFormula pUninstantiatedFormula, FormulaManagerView pFMGR, int pDefaultIndex) {
    PredicateAbstractState pas = AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    PathFormula pathFormula = pas.getPathFormula();
    BooleanFormula instantiatedFormula = pFMGR.instantiate(pUninstantiatedFormula, pathFormula.getSsa().withDefault(pDefaultIndex));
    BooleanFormula stateFormula = pathFormula.getFormula();
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    return bfmgr.or(bfmgr.not(stateFormula), instantiatedFormula);
  }

  /**
   * Create a disjunctive formula of all the path formulas in the supplied iterable.
   */
  public static BooleanFormula createFormulaFor(Iterable<AbstractState> states, BooleanFormulaManager pBFMGR) {
    BooleanFormula f = pBFMGR.makeBoolean(false);

    for (PredicateAbstractState e : AbstractStates.projectToType(states, PredicateAbstractState.class)) {
      f = pBFMGR.or(f, e.getPathFormula().getFormula());
    }

    return f;
  }

  /**
   * Unrolls the given reached set using the algorithm provided to this
   * instance of the bounded model checking algorithm.
   *
   * @param pReachedSet the reached set to unroll.
   *
   * @return {@code true} if the unrolling was sound, {@code false} otherwise.
   *
   * @throws CPAException if an exception occurred during unrolling the reached
   * set.
   * @throws InterruptedException if the unrolling is interrupted.
   */
  public static AlgorithmStatus unroll(LogManager pLogger, ReachedSet pReachedSet, Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA) throws CPAException, InterruptedException {
    return unroll(pLogger, pReachedSet, new ReachedSetInitializer() {

      @Override
      public void initialize(ReachedSet pReachedSet) {
        // Do nothing
      }

    }, pAlgorithm, pCPA);
  }

  public static AlgorithmStatus unroll(LogManager pLogger, ReachedSet pReachedSet, ReachedSetInitializer pInitializer, Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA) throws CPAException, InterruptedException {
    adjustReachedSet(pLogger, pReachedSet, pInitializer, pCPA);
    return pAlgorithm.run(pReachedSet);
  }

  /**
   * Adjusts the given reached set so that the involved adjustable condition
   * CPAs are able to operate properly without being negatively influenced by
   * states generated earlier under different conditions while trying to
   * retain as many states as possible.
   *
   * @param pReachedSet the reached set to be adjusted.
   * @param pInitializer initializes the reached set.
   */
  public static void adjustReachedSet(LogManager pLogger, ReachedSet pReachedSet, ReachedSetInitializer pInitializer, ConfigurableProgramAnalysis pCPA) throws CPAException, InterruptedException {
    Preconditions.checkArgument(!pReachedSet.isEmpty());
    CFANode initialLocation = extractLocation(pReachedSet.getFirstState());
    for (AdjustableConditionCPA conditionCPA : CPAs.asIterable(pCPA).filter(AdjustableConditionCPA.class)) {
      if (conditionCPA instanceof ReachedSetAdjustingCPA) {
        ((ReachedSetAdjustingCPA) conditionCPA).adjustReachedSet(pReachedSet);
      } else {
        pReachedSet.clear();
        pLogger.log(Level.WARNING, "Completely clearing the reached set after condition adjustment due to " + conditionCPA.getClass()
            + ". This may drastically impede the efficiency of iterative deepening. Implement ReachedSetAdjustingCPA to avoid this problem.");
        break;
      }
    }
    if (pReachedSet.isEmpty()) {
      pInitializer.initialize(pReachedSet);
      pReachedSet.add(
          pCPA.getInitialState(initialLocation, StateSpacePartition.getDefaultPartition()),
          pCPA.getInitialPrecision(initialLocation, StateSpacePartition.getDefaultPartition()));
    }
  }

}
