// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * A class representing formula that models a relation. It is a formula T(s,s') with two kinds of
 * variables. Variables representing the previous state s and variables representing the current
 * state s'.
 */
public class PartitionedRelationFormula {
  private final FormulaManagerView fmgr;
  private ImmutableSet<Formula> prevVariables;
  private ImmutableSet<Formula> currVariables;
  private BooleanFormula formula;

  public PartitionedRelationFormula(BooleanFormula pFormula, FormulaManagerView pFmgr) {
    formula = pFormula;
    fmgr = pFmgr;
    instantiateVariablesPartition();
  }

  /** Instantiates the map for the previous variables s for the relation formula T(s,s') */
  private void instantiateTheCurrVariables() {
    Map<String, Formula> varNamesToFormulas = fmgr.extractVariables(formula);
    Map<Formula, String> currIndex = new HashMap<>();
    boolean containsTransInv = usesTransInvKeyWord();

    // Search for the smallest SSA index of the variable
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      Formula pureVar = fmgr.uninstantiate(entry.getValue());

      if (containsTransInv == entry.getKey().contains(TransitionInvariantUtils.TRANS_INV_KEYWORD)
          && index > 0
          && (!currIndex.containsKey(pureVar) || getSSAIndex(currIndex.get(pureVar)) < index)) {
        currIndex.put(pureVar, entry.getKey());
      }
    }

    ImmutableSet.Builder<Formula> currSet = ImmutableSet.builder();
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      if (containsTransInv == entry.getKey().contains(TransitionInvariantUtils.TRANS_INV_KEYWORD)
          && index > 0
          && getSSAIndex(currIndex.get(fmgr.uninstantiate(entry.getValue()))) == index
          // The variables that occur only once in the formula should be in the prevVariables only
          && !prevVariables.contains(entry.getValue())) {
        currSet.add(entry.getValue());
      }
    }
    currVariables = currSet.build();
  }

  /** Instantiates the map for the current variables s' for the relation formula T(s,s') */
  private void instantiateThePrevVariables() {
    Map<String, Formula> varNamesToFormulas = fmgr.extractVariables(formula);
    Map<Formula, String> prevIndex = new HashMap<>();
    boolean containsTransInv = usesTransInvKeyWord();

    // Search for the smallest SSA index of the variable
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      Formula pureVar = fmgr.uninstantiate(entry.getValue());

      if (containsTransInv == entry.getKey().contains(TransitionInvariantUtils.TRANS_INV_KEYWORD)
          && index > 0
          && (!prevIndex.containsKey(pureVar) || getSSAIndex(prevIndex.get(pureVar)) > index)) {
        prevIndex.put(pureVar, entry.getKey());
      }
    }

    ImmutableSet.Builder<Formula> prevSet = ImmutableSet.builder();
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      if (containsTransInv == entry.getKey().contains(TransitionInvariantUtils.TRANS_INV_KEYWORD)
          && index > 0
          && getSSAIndex(prevIndex.get(fmgr.uninstantiate(entry.getValue()))) == index) {
        prevSet.add(entry.getValue());
      }
    }
    prevVariables = prevSet.build();
  }

  /**
   * Instantiates maps for the previous s and the current variables s' for the relation formula
   * T(s,s')
   */
  private void instantiateVariablesPartition() {
    instantiateThePrevVariables();
    instantiateTheCurrVariables();
  }

  private boolean usesTransInvKeyWord() {
    return fmgr.extractVariables(formula).keySet().stream()
        .anyMatch(varName -> varName.contains(TransitionInvariantUtils.TRANS_INV_KEYWORD));
  }

  private int getSSAIndex(String pFormula) {
    return FormulaManagerView.parseName(pFormula).getSecond().orElse(-2);
  }

  private ImmutableMap<Formula, Formula> getSubMap(ImmutableSet<Formula> variables, String suffix) {
    return variables.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                var -> var,
                var ->
                    fmgr.makeVariable(
                        fmgr.getFormulaType(var),
                        TransitionInvariantUtils.removeTransInvKeyWord(
                                fmgr.uninstantiate(var).toString())
                            + suffix)));
  }

  /**
   * Constructs a formula that expresses equality between unchanged variables by the provided
   * formula in comparison to the formula in this class. This method is meant to be used when
   * initializing prev and curr variables. While renaming, we loose the information about the
   * variables that are not changed by the given formula. Example: Let us assume two path formulas
   * y@0 >= 1 and x@1 = x@0 + y@0. After renaming, both of them we would obtain the following
   * y__TransInv@1 >= 1 and x__TransInv@3 = x__TransInv@2 + y__TransInv@2. Since y did not change,
   * we have to construct a formula saying y__TransInv@1 = y__TransInv@2.
   *
   * @param updateFormula that changes the variables in the formula of this class
   * @return An equality formula that expresses that the unchanged variables have the same value
   */
  public BooleanFormula constructFormulaForUnchangedVars(
      BooleanFormula updateFormula, String suffix) {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    BooleanFormula equivalenceFormula = bfmgr.makeTrue();
    Map<String, Formula> varsInUpdateFormula = fmgr.extractVariables(updateFormula);

    for (Formula variable : currVariables) {
      String pureName =
          TransitionInvariantUtils.removeTransInvKeyWord(fmgr.uninstantiate(variable).toString());

      if (varsInUpdateFormula.values().stream()
          .noneMatch(
              updateVar ->
                  TransitionInvariantUtils.removeTransInvKeyWord(
                          fmgr.uninstantiate(updateVar).toString())
                      .equals(pureName))) {
        equivalenceFormula =
            bfmgr.and(
                equivalenceFormula,
                fmgr.assignment(
                    variable, fmgr.makeVariable(fmgr.getFormulaType(variable), pureName + suffix)));
      }
    }
    return equivalenceFormula;
  }

  /**
   * It can happen that there is only one version of a variable. By default, we put such variables
   * into the prevVariables set. It might be that we want to switch this view and treat them as
   * current variables.
   */
  public void treatPrevVarsAsCurrVars() {
    ImmutableSet.Builder<Formula> builder = ImmutableSet.builder();
    builder.addAll(currVariables);
    for (Formula var : prevVariables) {
      String pureName =
          TransitionInvariantUtils.removeTransInvKeyWord(fmgr.uninstantiate(var).toString());
      if (currVariables.stream()
          .noneMatch(
              currVar ->
                  TransitionInvariantUtils.removeTransInvKeyWord(
                          fmgr.uninstantiate(currVar).toString())
                      .equals(pureName))) {
        builder.add(var);
      }
    }
    currVariables = builder.build();
  }

  public void extendPrevVarsWithSuffix(String suffix) {
    formula = fmgr.substitute(formula, getSubMap(prevVariables, suffix));
    instantiateThePrevVariables();
  }

  public void extendCurrVarsWithSuffix(String suffix) {
    formula = fmgr.substitute(formula, getSubMap(currVariables, suffix));
    instantiateTheCurrVariables();
  }

  public BooleanFormula getFormula() {
    return formula;
  }
}
