// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
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
   * It does it using the check T(s,s') => [∃s1.T(s,s1) ∧ ¬T(s',s1)] ∧ [∀s2.T(s',s2) => T(s,s2)] If
   * this holds, it means that the number of states reachable from s is decreasing. In other words,
   * the cardinality of the set of reachable states is rank for every state.
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
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars)
      throws InterruptedException, CPAException {
    SSAMap ssaMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(pFormula, 1, 2, fmgr, scope);

    // T(s,s') ∧ I(s) ∧ I(s')
    BooleanFormula oneStep = buildOneStepFormula(pFormula, ssaMap, pSupportingInvariants);

    // T(s,s1) ∧ I(s1), ¬T(s',s1)
    BooleanFormula stepFromS = buildStepFromS(pFormula, pSupportingInvariants);
    BooleanFormula stepFromSPrime = buildStepFromSPrime(pFormula);

    // ∃s1. T(s,s1) ∧ ¬T(s',s1) ∧ I(s1)
    final ImmutableList<Formula> quantifiedVars =
        collectAllCurrVariables(stepFromS, pFormula, 1, 3);
    BooleanFormula middleStep = buildMiddleStepFormula(stepFromS, stepFromSPrime, quantifiedVars);

    // T(s,s2), T(s',s2) ∧ I(s2)
    BooleanFormula stepFromS2 = buildSecondStepFromS(pFormula);
    BooleanFormula stepFromSPrime2 = buildSecondStepFromSPrime(pFormula, pSupportingInvariants);

    // ∀s2.T(s',s2) ∧ I(s2) => T(s,s2)
    final ImmutableList<Formula> quantifiedVars2 =
        collectAllCurrVariables(stepFromSPrime2, pFormula, 4, 5);
    BooleanFormula middleStep2 =
        buildSecondMiddleFormula(stepFromS2, stepFromSPrime2, quantifiedVars2);

    // T(s,s') ∧ I(s) ∧ I(s') => [∃s1.T(s,s1) ∧ I(s1) ∧ ¬T(s',s1)] ∧ [∀s2.T(s',s2) ∧ I(s1) =>
    // T(s,s2)]
    BooleanFormula wellFoundedness =
        buildSetDecreasingFormula(middleStep, middleStep2, oneStep, stepFromSPrime);

    boolean isWellfounded;
    try {
      isWellfounded = solver.isUnsat(wellFoundedness);
    } catch (SolverException e) {
      throw new CPAException("Well-Foundedness check failed due to a solver crash!");
    }

    return isWellfounded;
  }

  private BooleanFormula buildSetDecreasingFormula(
      BooleanFormula middleStep,
      BooleanFormula middleStep2,
      BooleanFormula oneStep,
      BooleanFormula stepFromSPrime) {
    BooleanFormula conclusion = bfmgr.and(middleStep, middleStep2);
    oneStep =
        bfmgr.and(
            TransitionInvariantUtils.makeStatesEquivalent(
                stepFromSPrime, oneStep, 4, 2, bfmgr, fmgr),
            oneStep);
    BooleanFormula wellFoundedness = bfmgr.implication(oneStep, conclusion);
    wellFoundedness = bfmgr.not(wellFoundedness);
    return wellFoundedness;
  }

  private BooleanFormula buildSecondMiddleFormula(
      BooleanFormula stepFromS2,
      BooleanFormula stepFromSPrime2,
      ImmutableList<Formula> quantifiedVars) {
    BooleanFormula middleStep2 = bfmgr.implication(stepFromSPrime2, stepFromS2);
    if (!quantifiedVars.isEmpty()) {
      middleStep2 = qfmgr.forall(quantifiedVars, middleStep2);
    }
    return middleStep2;
  }

  private BooleanFormula buildSecondStepFromSPrime(
      BooleanFormula pFormula, ImmutableList<BooleanFormula> pSupportingInvariants) {
    SSAMap ssaMapForSPrime2 =
        TransitionInvariantUtils.setIndicesToDifferentValues(pFormula, 4, 5, fmgr, scope);
    BooleanFormula stepFromSPrime2 = fmgr.instantiate(pFormula, ssaMapForSPrime2);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      SSAMap ssaMap =
          TransitionInvariantUtils.setIndicesToDifferentValues(
              supportingInvariant, 1, 5, fmgr, scope);
      stepFromSPrime2 = bfmgr.and(stepFromSPrime2, fmgr.instantiate(supportingInvariant, ssaMap));
    }
    return stepFromSPrime2;
  }

  private BooleanFormula buildSecondStepFromS(BooleanFormula pFormula) {
    SSAMap ssaMapForS2 =
        TransitionInvariantUtils.setIndicesToDifferentValues(pFormula, 1, 5, fmgr, scope);
    return fmgr.instantiate(pFormula, ssaMapForS2);
  }

  private BooleanFormula buildMiddleStepFormula(
      BooleanFormula stepFromS,
      BooleanFormula stepFromSPrime,
      ImmutableList<Formula> quantifiedVars) {
    BooleanFormula middleStep = fmgr.makeAnd(stepFromS, stepFromSPrime);
    if (!quantifiedVars.isEmpty()) {
      middleStep = qfmgr.exists(quantifiedVars, middleStep);
    }
    return middleStep;
  }

  private BooleanFormula buildStepFromSPrime(BooleanFormula pFormula) {
    SSAMap ssaMapForSPrime =
        TransitionInvariantUtils.setIndicesToDifferentValues(pFormula, 4, 3, fmgr, scope);
    BooleanFormula stepFromSPrime = fmgr.instantiate(pFormula, ssaMapForSPrime);
    stepFromSPrime = fmgr.makeNot(stepFromSPrime);
    return stepFromSPrime;
  }

  private BooleanFormula buildStepFromS(
      BooleanFormula pFormula, ImmutableList<BooleanFormula> pSupportingInvariants) {
    SSAMap ssaMapForS =
        TransitionInvariantUtils.setIndicesToDifferentValues(pFormula, 1, 3, fmgr, scope);
    BooleanFormula stepFromS = fmgr.instantiate(pFormula, ssaMapForS);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      SSAMap ssaMap =
          TransitionInvariantUtils.setIndicesToDifferentValues(
              supportingInvariant, 1, 3, fmgr, scope);
      stepFromS = bfmgr.and(stepFromS, fmgr.instantiate(supportingInvariant, ssaMap));
    }
    return stepFromS;
  }

  private BooleanFormula buildOneStepFormula(
      BooleanFormula pFormula,
      SSAMap pSSAMap,
      ImmutableList<BooleanFormula> pSupportingInvariants) {
    BooleanFormula oneStep = fmgr.instantiate(pFormula, pSSAMap);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      pSSAMap =
          TransitionInvariantUtils.setIndicesToDifferentValues(
              supportingInvariant, 1, 2, fmgr, scope);
      oneStep = bfmgr.and(oneStep, fmgr.instantiate(supportingInvariant, pSSAMap));
      pSSAMap =
          TransitionInvariantUtils.setIndicesToDifferentValues(
              supportingInvariant, 1, 6, fmgr, scope);
      oneStep = bfmgr.and(oneStep, fmgr.instantiate(supportingInvariant, pSSAMap));

      oneStep =
          bfmgr.and(
              TransitionInvariantUtils.makeStatesEquivalent(oneStep, oneStep, 1, 6, bfmgr, fmgr),
              oneStep);
    }
    return oneStep;
  }

  /**
   * Collects all the variables without the __PREV suffix.
   *
   * @param pFormula containing all the variables
   * @return List of the variables without __PREV suffix.
   */
  private ImmutableList<Formula> collectAllCurrVariables(
      Formula pFormula, Formula pInitialFormula, int pPrevIndex, int pCurrIndex) {
    SSAMap ssaMap =
        TransitionInvariantUtils.setIndicesToDifferentValues(
            pInitialFormula, pPrevIndex, pCurrIndex, fmgr, scope);
    ImmutableList.Builder<Formula> builder = ImmutableList.builder();
    Map<String, Formula> mapNamesToVariables = fmgr.extractVariables(fmgr.uninstantiate(pFormula));
    for (Map.Entry<String, Formula> entry : mapNamesToVariables.entrySet()) {
      if (ssaMap.containsVariable(entry.getKey())
          && ssaMap.getIndex(entry.getKey()) == pPrevIndex) {
        builder.add(mapNamesToVariables.get(entry.getKey()));
      }
    }
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
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars)
      throws InterruptedException, CPAException {
    Set<BooleanFormula> invariantInDNF = bfmgr.toDisjunctionArgs(pFormula, true);

    for (BooleanFormula candidateInvariant : invariantInDNF) {
      if (!isTheFormulaSimplyWellFounded(candidateInvariant, pSupportingInvariants)) {
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
      BooleanFormula pFormula, ImmutableList<BooleanFormula> pSupportingInvariants)
      throws InterruptedException, CPAException {
    pFormula = buildSameStateComparisonFormula(pFormula, pSupportingInvariants);

    try {
      // Checks well-foundedness as
      if (solver.isUnsat(pFormula)) {
        return true;
      }
    } catch (SolverException e) {
      throw new CPAException("Well-Foundedness check failed due to a solver crash!");
    }
    return false;
  }

  private BooleanFormula buildSameStateComparisonFormula(
      BooleanFormula pFormula, ImmutableList<BooleanFormula> pSupportingInvariants) {
    pFormula =
        fmgr.instantiate(
            pFormula,
            TransitionInvariantUtils.setIndicesToDifferentValues(pFormula, 1, 1, fmgr, scope));
    BooleanFormula sameState =
        TransitionInvariantUtils.makeStatesEquivalent(pFormula, pFormula, 1, 1, bfmgr, fmgr);
    for (BooleanFormula supportingInvariant : pSupportingInvariants) {
      SSAMap ssaMap =
          TransitionInvariantUtils.setIndicesToDifferentValues(
              supportingInvariant, 1, 1, fmgr, scope);
      sameState = bfmgr.and(sameState, fmgr.instantiate(supportingInvariant, ssaMap));
    }
    pFormula = bfmgr.and(sameState, pFormula);
    return pFormula;
  }
}
