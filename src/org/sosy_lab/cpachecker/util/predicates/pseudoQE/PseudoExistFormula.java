// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pseudoQE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

/**
 * A class to represent an existential quantified formula, without using Solver build in mechanisms
 * Designed for package-level storing of the quantified Formula, for easier manipulation
 */
class PseudoExistFormula {
  private final Map<String, Formula> quantifiedVars;
  private final BooleanFormula quantifiedFormula;
  private final List<BooleanFormula> conjunctsWithoutQuantifiedVars;
  private final List<BooleanFormula> conjunctsWithQuantifiedVars;

  /**
   * Create a new PseudoExistFormula
   *
   * @param pQuantifiedVars A Map of the quantified Variables, names as keys and formulas as values
   * @param pQuantifiedFormula A Boolean Formula in which the variables of pQuantifiedVars would be
   *     quantified
   * @param pFmgr Pass instance of the FormulaManager for internal operations
   */
  PseudoExistFormula(
      final Map<String, Formula> pQuantifiedVars,
      final BooleanFormula pQuantifiedFormula,
      final FormulaManagerView pFmgr) {
    quantifiedVars = ImmutableMap.copyOf(pQuantifiedVars);
    quantifiedFormula = pQuantifiedFormula;

    // Divide the conjuncts in those with and those without quantified Variables
    List<BooleanFormula> tmpWithoutQuantifiedVars = new ArrayList<>();
    List<BooleanFormula> tmpWithQuantifiedVars = new ArrayList<>();
    for (BooleanFormula conjunct :
        pFmgr.getBooleanFormulaManager().toConjunctionArgs(quantifiedFormula, true)) {
      if (Sets.intersection(pFmgr.extractVariableNames(conjunct), quantifiedVars.keySet())
          .isEmpty()) {
        tmpWithoutQuantifiedVars.add(conjunct);
      } else {
        tmpWithQuantifiedVars.add(conjunct);
      }
    }

    // Make lists immutable:
    conjunctsWithoutQuantifiedVars = ImmutableList.copyOf(tmpWithoutQuantifiedVars);
    conjunctsWithQuantifiedVars = ImmutableList.copyOf(tmpWithQuantifiedVars);
  }

  /**
   * Get the bound variables
   *
   * @return A Map with variable names as keys and the formulas as value
   */
  Map<String, Formula> getQuantifiedVars() {
    return quantifiedVars;
  }

  /**
   * Get the bound variables as Collection of formulas
   *
   * @return A Collection of formulas representing the
   */
  Collection<Formula> getQuantifiedVarFormulas() {
    return quantifiedVars.values();
  }

  /**
   * Get the inner formula in its not quantified form
   *
   * @return the inner part of the quantified Formula
   */
  BooleanFormula getInnerFormula() {
    return quantifiedFormula;
  }

  /**
   * Has the Formula Quantifiers?
   *
   * @return True if the Formula contains 1 or more Quantified variables
   */
  boolean hasQuantifiers() {
    return !quantifiedVars.isEmpty();
  }

  /**
   * Get the number of quantified Variables in the formula
   *
   * @return the number of quantified variables
   */
  int getNumberOfQuantifiers() {
    return quantifiedVars.size();
  }

  /**
   * Get list of all parts of a Conjunction not containing bound variables
   *
   * @return List of not quantified parts of the conjunction
   */
  List<BooleanFormula> getConjunctsWithoutQuantifiedVars() {
    return conjunctsWithoutQuantifiedVars;
  }

  /**
   * Get list of all parts of a Conjunction containing bound variables
   *
   * @return List of quantified parts of the conjunction
   */
  List<BooleanFormula> getConjunctsWithQuantifiedVars() {
    return conjunctsWithQuantifiedVars;
  }

  @Override
  public String toString() {
    return String.format("EXISTS %s : %s", quantifiedVars, quantifiedFormula);
  }
}
