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
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * A class representing formula that models a relation. It is a formula T(s,s') with two kinds of
 * variables. Variables representing the previous state s and variables representing the current
 * state s'.
 *
 * <p>Instances are immutable: {@link #withPrevVarsSuffixed} and {@link #withCurrVarsSuffixed}
 * return new instances rather than mutating the receiver.
 */
public class PartitionedRelationFormula {

  // Any real SSA index used by JavaSMT/CPAchecker's formula encoding is >= 0 (unindexed
  // variables are reported as absent from SSA and are treated as index 0 by callers), so -2
  // is used as an "not applicable / not SSA-indexed" sentinel that can never collide with a
  // real index and is distinguishable from the "no index found" case some parsers use (-1).
  private static final int NO_SSA_INDEX = -2;

  private final FormulaManagerView fmgr;
  private ImmutableSet<Formula> prevVariables;
  private ImmutableSet<Formula> currVariables;
  private BooleanFormula formula;

  public PartitionedRelationFormula(BooleanFormula pFormula, FormulaManagerView pFmgr) {
    formula = pFormula;
    fmgr = pFmgr;

    Map<String, Formula> varNamesToFormulas = fmgr.extractVariables(formula);
    boolean containsTransInv = usesTransInvKeyWord(varNamesToFormulas);

    // Order matters: "variables that occur only once" must be attributed to prevVariables, so
    // prevVariables has to be computed first and then excluded from currVariables.
    prevVariables =
        instantiatePartition(
            varNamesToFormulas,
            containsTransInv,
            /* instantiatePrevVars= */ true,
            Optional.empty());
    currVariables =
        instantiatePartition(
            varNamesToFormulas,
            containsTransInv,
            /* instantiatePrevVars= */ false,
            Optional.of(prevVariables));
  }

  /**
   * Constructor for the case, where we now some of the partition a priori. It is especially useful
   * for the cases where we want to substitute only some partition of the formula.
   */
  public PartitionedRelationFormula(
      BooleanFormula pFormula,
      FormulaManagerView pFmgr,
      ImmutableSet<Formula> pPrevVariables,
      ImmutableSet<Formula> pCurrVariables) {
    formula = pFormula;
    fmgr = pFmgr;
    prevVariables = pPrevVariables;
    currVariables = pCurrVariables;
  }

  /**
   * For the "previous state" partition we want, per variable, the occurrence with the
   * <em>smallest</em> SSA index (pickLargestIndex = false); for the "current state" partition we
   * want the <em>largest</em> SSA index (pickLargestIndex = true). Variables that occur only once
   * in the formula are attributed to the previous-state partition only, which is why the
   * current-state computation is given the already-computed previous-state set to exclude.
   */
  private ImmutableSet<Formula> instantiatePartition(
      Map<String, Formula> varNamesToFormulas,
      boolean containsTransInv,
      boolean instantiatePrevVars,
      Optional<ImmutableSet<Formula>> excludeIfAlreadyIn) {
    Map<Formula, String> foundIndex = new HashMap<>();

    // Search for the smallest SSA index of the variable
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      Formula pureVar = fmgr.uninstantiate(entry.getValue());

      if (containsTransInv == entry.getKey().contains(TransitionInvariantUtils.TRANS_INV_KEYWORD)
          && index > 0
          && (!foundIndex.containsKey(pureVar)
              || (instantiatePrevVars && getSSAIndex(foundIndex.get(pureVar)) > index)
              || (!instantiatePrevVars && getSSAIndex(foundIndex.get(pureVar)) < index))) {
        foundIndex.put(pureVar, entry.getKey());
      }
    }

    ImmutableSet.Builder<Formula> result = ImmutableSet.builder();
    for (Entry<String, Formula> entry : varNamesToFormulas.entrySet()) {
      int index = getSSAIndex(entry.getKey());
      if (containsTransInv == entry.getKey().contains(TransitionInvariantUtils.TRANS_INV_KEYWORD)
          && index > 0
          && getSSAIndex(foundIndex.get(fmgr.uninstantiate(entry.getValue()))) == index
          // The variables that occur only once in the formula should be in the prevVariables only
          && (excludeIfAlreadyIn.isEmpty()
              || !excludeIfAlreadyIn.orElseThrow().contains(entry.getValue()))) {
        result.add(entry.getValue());
      }
    }
    return result.build();
  }

  private boolean usesTransInvKeyWord(Map<String, Formula> varNamesToFormulas) {
    return varNamesToFormulas.keySet().stream()
        .anyMatch(varName -> varName.contains(TransitionInvariantUtils.TRANS_INV_KEYWORD));
  }

  private int getSSAIndex(String pFormula) {
    return FormulaManagerView.parseName(pFormula).getSecond().orElse(NO_SSA_INDEX);
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
   * Returns a new {@link PartitionedRelationFormula} with the previous-state variables substituted
   * to carry the given suffix. Does not mutate {@code this}.
   */
  public PartitionedRelationFormula withPrevVarsSuffixed(String suffix) {
    BooleanFormula substituted = fmgr.substitute(formula, getSubMap(prevVariables, suffix));
    Map<String, Formula> varNamesToFormulas = fmgr.extractVariables(substituted);
    boolean containsTransInv = usesTransInvKeyWord(varNamesToFormulas);

    ImmutableSet<Formula> newPrevVariables =
        instantiatePartition(
            varNamesToFormulas,
            containsTransInv,
            /* instantiatePrevVars= */ true,
            Optional.empty());
    return new PartitionedRelationFormula(substituted, fmgr, newPrevVariables, currVariables);
  }

  /**
   * Returns a new {@link PartitionedRelationFormula} with the current-state variables substituted
   * to carry the given suffix. Does not mutate {@code this}.
   */
  public PartitionedRelationFormula withCurrVarsSuffixed(String suffix) {
    BooleanFormula substituted = fmgr.substitute(formula, getSubMap(currVariables, suffix));
    Map<String, Formula> varNamesToFormulas = fmgr.extractVariables(substituted);
    boolean containsTransInv = usesTransInvKeyWord(varNamesToFormulas);

    ImmutableSet<Formula> newCurrVariables =
        instantiatePartition(
            varNamesToFormulas,
            containsTransInv,
            /* instantiatePrevVars= */ false,
            Optional.of(prevVariables));
    return new PartitionedRelationFormula(substituted, fmgr, prevVariables, newCurrVariables);
  }

  public BooleanFormula getFormula() {
    return formula;
  }
}
