// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.CurrStateIndices;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.PrevStateIndices;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.QuantifiedFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

public class DecreasingCardinalityChecker implements WellFoundednessChecker {

  private final Solver solver;
  private final Scope scope;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final QuantifiedFormulaManagerView qfmgr;

  public DecreasingCardinalityChecker(
      final FormulaManagerView pFmgr,
      final BooleanFormulaManagerView pBfmrg,
      final Solver pSolver,
      final Scope pScope) {
    solver = pSolver;
    scope = pScope;
    fmgr = pFmgr;
    bfmgr = pBfmrg;
    qfmgr = fmgr.getQuantifiedFormulaManager();
  }

  /**
   * This method checks whether one concrete subformula from transition invariant is well-founded.
   * In the following, the transition invariant is T and, the conjunction of its supporting
   * invariants is denoted with I. The high-level idea is to check that for every two states s,s',
   * for which there is a transition with respect to T, we have to find another state s1, that is
   * in-between. This means that we can get from s to s1 but not from s' to s1. Moreover, all the
   * reachable states from s with respect to T, must be also reachable from s'. We denote these by
   * s2. The check is then T(s,s') ∧ I(s) ∧ I(s') => [∃s1.T(s,s1) ∧ I(s1) ∧ ¬T(s',s1)] ∧
   * [∀s2.T(s',s2) ∧ I(s2) => T(s,s2)] If this holds, it means that the number of states reachable
   * from s is decreasing. In other words, the cardinality of the set of reachable states is rank
   * for every state.
   *
   * @param pFormula representing the transition invariant
   * @param pSupportingInvariants that can strengthen the transition invariant
   * @return true if the formula really is well-founded, false otherwise
   */
  @Override
  public boolean isWellFounded(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      Loop pLoop,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevVarsToCurrVars)
      throws InterruptedException, CPAException {
    SSAMap ssaMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(
            pFormula,
            PrevStateIndices.INDEX_S,
            CurrStateIndices.INDEX_S_PRIME,
            fmgr,
            scope,
            mapPrevVarsToCurrVars);

    // T(s,s') ∧ I(s) ∧ I(s')
    BooleanFormula sToSPrimeStep =
        buildSToSPrimeStep(pFormula, ssaMap, pSupportingInvariants, mapPrevVarsToCurrVars);

    // T(s,s1) ∧ I(s1), ¬T(s',s1)
    BooleanFormula sToS1Step =
        buildStepFormulaWithInvariants(
            pFormula,
            pSupportingInvariants,
            mapPrevVarsToCurrVars,
            PrevStateIndices.INDEX_S,
            CurrStateIndices.INDEX_S1);
    BooleanFormula notSPrimeToS1Step = buildNotSPrimeToS1Step(pFormula, mapPrevVarsToCurrVars);

    // ∃s1. T(s,s1) ∧ ¬T(s',s1) ∧ I(s1)
    final ImmutableList<Formula> quantifiedVars =
        collectAllCurrVariables(
            sToS1Step,
            pFormula,
            PrevStateIndices.INDEX_S,
            CurrStateIndices.INDEX_S1,
            mapPrevVarsToCurrVars);
    BooleanFormula existsS1BetweenSAndSPrime =
        buildExistsS1BetweenSAndSPrime(sToS1Step, notSPrimeToS1Step, quantifiedVars);

    // T(s,s2), T(s',s2) ∧ I(s2)
    BooleanFormula sToS2Step = buildSToS2Step(pFormula, mapPrevVarsToCurrVars);
    BooleanFormula sPrimeToS2Step =
        buildStepFormulaWithInvariants(
            pFormula,
            pSupportingInvariants,
            mapPrevVarsToCurrVars,
            PrevStateIndices.INDEX_S_PRIME,
            CurrStateIndices.INDEX_S2);

    // ∀s2.T(s',s2) ∧ I(s2) => T(s,s2)
    final ImmutableList<Formula> quantifiedVars2 =
        collectAllCurrVariables(
            sPrimeToS2Step,
            pFormula,
            PrevStateIndices.INDEX_S_PRIME,
            CurrStateIndices.INDEX_S2,
            mapPrevVarsToCurrVars);
    BooleanFormula allReachableStatesFromSPrimeAreFromS =
        buildAllReachableStatesFromSPrimeAreFromS(sToS2Step, sPrimeToS2Step, quantifiedVars2);

    // T(s,s') ∧ I(s) ∧ I(s') => [∃s1.T(s,s1) ∧ I(s1) ∧ ¬T(s',s1)] ∧ [∀s2.T(s',s2) ∧ I(s1) =>
    // T(s,s2)]
    BooleanFormula wellFoundedness =
        buildSetDecreasingFormula(
            existsS1BetweenSAndSPrime,
            allReachableStatesFromSPrimeAreFromS,
            sToSPrimeStep,
            notSPrimeToS1Step,
            mapPrevVarsToCurrVars);

    boolean isWellfounded;
    try {
      isWellfounded = solver.isUnsat(wellFoundedness);
    } catch (SolverException e) {
      throw new CPAException("Well-Foundedness check failed due to a solver crash!", e);
    }

    return isWellfounded;
  }

  private BooleanFormula buildSetDecreasingFormula(
      BooleanFormula middleStep,
      BooleanFormula middleStep2,
      BooleanFormula oneStep,
      BooleanFormula stepFromSPrime,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevVarsToCurr) {
    BooleanFormula conclusion = bfmgr.and(middleStep, middleStep2);
    oneStep =
        bfmgr.and(
            TransitionInvariantUtils.makeStatesEquivalent(
                stepFromSPrime, oneStep, bfmgr, fmgr, pMapPrevVarsToCurr),
            oneStep);
    BooleanFormula wellFoundedness = bfmgr.implication(oneStep, conclusion);
    wellFoundedness = bfmgr.not(wellFoundedness);
    return wellFoundedness;
  }

  private BooleanFormula buildAllReachableStatesFromSPrimeAreFromS(
      BooleanFormula stepFromS2,
      BooleanFormula stepFromSPrime2,
      ImmutableList<Formula> quantifiedVars) {
    BooleanFormula middleStep2 = bfmgr.implication(stepFromSPrime2, stepFromS2);
    if (!quantifiedVars.isEmpty()) {
      middleStep2 = qfmgr.forall(quantifiedVars, middleStep2);
    }
    return middleStep2;
  }

  private BooleanFormula buildStepFormulaWithInvariants(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars,
      PrevStateIndices fromState,
      CurrStateIndices toState) {
    BooleanFormula fromStateToStateStep =
        instantiateWithNewIndices(pFormula, fromState, toState, pMapPrevToCurrVars);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      supportingInvariant =
          instantiateWithNewIndices(supportingInvariant, fromState, toState, pMapPrevToCurrVars);
      fromStateToStateStep = bfmgr.and(fromStateToStateStep, supportingInvariant);
    }
    return fromStateToStateStep;
  }

  private BooleanFormula buildSToS2Step(
      BooleanFormula pFormula,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    return instantiateWithNewIndices(
        pFormula, PrevStateIndices.INDEX_S, CurrStateIndices.INDEX_S2, pMapPrevToCurrVars);
  }

  private BooleanFormula buildExistsS1BetweenSAndSPrime(
      BooleanFormula stepFromS,
      BooleanFormula stepFromSPrime,
      ImmutableList<Formula> quantifiedVars) {
    BooleanFormula middleStep = fmgr.makeAnd(stepFromS, stepFromSPrime);
    if (!quantifiedVars.isEmpty()) {
      middleStep = qfmgr.exists(quantifiedVars, middleStep);
    }
    return middleStep;
  }

  private BooleanFormula buildNotSPrimeToS1Step(
      BooleanFormula pFormula,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    BooleanFormula stepFromSPrime =
        instantiateWithNewIndices(
            pFormula,
            PrevStateIndices.INDEX_S_PRIME,
            CurrStateIndices.INDEX_S1,
            pMapPrevToCurrVars);
    stepFromSPrime = fmgr.makeNot(stepFromSPrime);
    return stepFromSPrime;
  }

  private BooleanFormula buildSToSPrimeStep(
      BooleanFormula pFormula,
      SSAMap pSSAMap,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    BooleanFormula oneStep = fmgr.instantiate(pFormula, pSSAMap);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      supportingInvariant =
          instantiateWithNewIndices(
              supportingInvariant,
              PrevStateIndices.INDEX_S,
              CurrStateIndices.INDEX_S_PRIME,
              pMapPrevToCurrVars);
      oneStep = bfmgr.and(oneStep, supportingInvariant);
      supportingInvariant =
          instantiateWithNewIndices(
              supportingInvariant,
              PrevStateIndices.INDEX_S,
              CurrStateIndices.INDEX_S,
              pMapPrevToCurrVars);
      oneStep = bfmgr.and(oneStep, supportingInvariant);

      oneStep =
          bfmgr.and(
              TransitionInvariantUtils.makeStatesEquivalent(
                  oneStep, oneStep, bfmgr, fmgr, pMapPrevToCurrVars),
              oneStep);
    }
    return oneStep;
  }

  /**
   * Collects all the previous variables.
   *
   * @param pFormula containing all the variables
   * @return List of the previous variables.
   */
  private ImmutableList<Formula> collectAllCurrVariables(
      Formula pFormula,
      Formula pInitialFormula,
      PrevStateIndices pPrevIndex,
      CurrStateIndices pCurrIndex,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    SSAMap ssaMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(
            pInitialFormula, pPrevIndex, pCurrIndex, fmgr, scope, pMapPrevToCurrVars);
    ImmutableList.Builder<Formula> builder = ImmutableList.builder();
    Map<String, Formula> mapNamesToVariables = fmgr.extractVariables(fmgr.uninstantiate(pFormula));
    builder.addAll(
        FluentIterable.from(mapNamesToVariables.keySet())
            .filter(
                name ->
                    ssaMap.containsVariable(name) && ssaMap.getIndex(name) == pPrevIndex.getIndex())
            .transform(mapNamesToVariables::get)
            .toSet());
    return builder.build();
  }

  /**
   * Checks whether the formula can be divided into disjunction of formulas expressing relations
   * that are well-founded. We do it by transformation into DNF and then checking each respective
   * subformula.
   *
   * @param pFormula that is to be checked for disjunctive well-foundedness.
   * @param pSupportingInvariants that can strengthen the transition invariant
   * @return true if the formula is disjunctively well-founded, false otherwise.
   */
  @Override
  public boolean isDisjunctivelyWellFounded(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      Loop pLoop,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapPrevVarsToCurrVars)
      throws InterruptedException, CPAException {
    Set<BooleanFormula> invariantInDNF = bfmgr.toDisjunctionArgs(pFormula, true);

    for (BooleanFormula candidateInvariant : invariantInDNF) {
      if (!isTheFormulaSimplyWellFounded(
          candidateInvariant, pSupportingInvariants, mapPrevVarsToCurrVars)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks whether the formula is simply well-founded, i.e. T(s,s) holds. We can do it as R^+ => T
   * means that T(s,s) violates well-foundedness on reachable states.
   */
  private boolean isTheFormulaSimplyWellFounded(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars)
      throws InterruptedException, CPAException {
    pFormula = buildSameStateComparisonFormula(pFormula, pSupportingInvariants, pMapPrevToCurrVars);

    try {
      // Checks well-foundedness as
      if (solver.isUnsat(pFormula)) {
        return true;
      }
    } catch (SolverException e) {
      throw new CPAException("Well-Foundedness check failed due to a solver crash!", e);
    }
    return false;
  }

  private BooleanFormula buildSameStateComparisonFormula(
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    pFormula =
        instantiateWithNewIndices(
            pFormula, PrevStateIndices.INDEX_S, CurrStateIndices.INDEX_S, pMapPrevToCurrVars);
    BooleanFormula sameState =
        TransitionInvariantUtils.makeStatesEquivalent(
            pFormula, pFormula, bfmgr, fmgr, pMapPrevToCurrVars);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      supportingInvariant =
          instantiateWithNewIndices(
              supportingInvariant,
              PrevStateIndices.INDEX_S,
              CurrStateIndices.INDEX_S,
              pMapPrevToCurrVars);
      sameState = bfmgr.and(sameState, supportingInvariant);
    }
    pFormula = bfmgr.and(sameState, pFormula);
    return pFormula;
  }

  private BooleanFormula instantiateWithNewIndices(
      BooleanFormula pFormula,
      PrevStateIndices prevStateIndex,
      CurrStateIndices currStateIndex,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> pMapPrevToCurrVars) {
    SSAMap ssaMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(
            pFormula, prevStateIndex, currStateIndex, fmgr, scope, pMapPrevToCurrVars);
    return fmgr.instantiate(pFormula, ssaMap);
  }
}
