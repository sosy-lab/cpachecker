// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BooleanVarFeatureEncoding<T> implements DiscreteFeatureEncoding<T> {
  private Table<TaDeclaration, T, String> variableNames;
  protected final FormulaManagerView fmgr;

  public BooleanVarFeatureEncoding(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    variableNames = HashBasedTable.create();
  }

  public void addEntry(TaDeclaration pAutomaton, T pValue, String pVariableName) {
    variableNames.put(pAutomaton, pValue, pVariableName);
  }

  /**
   * Adds the same value entry to every automaton. This is useful for shared objects (e.g.
   * synchronizing actions). This is the same as iterating over each automaton and calling addEntry
   */
  public void addEntryToAllAutomata(T pValue, String pVariableName) {
    // using variable names here is really unsafe!
    variableNames.rowKeySet().forEach(automaton -> addEntry(automaton, pValue, pVariableName));
  }

  private BooleanFormula makeVariableFormula(String variableName, int variableIndex) {
    return fmgr.getBooleanFormulaManager().makeVariable(variableName, variableIndex);
  }

  @Override
  public BooleanFormula makeEqualsFormula(TaDeclaration pAutomaton, int pVariableIndex, T feature) {
    return makeVariableFormula(variableNames.get(pAutomaton, feature), pVariableIndex);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var unchangedFormulas =
        from(variableNames.row(pAutomaton).values())
            .transform(
                variable -> {
                  var variableBefore = makeVariableFormula(variable, pIndexBefore);
                  var variableAfter = makeVariableFormula(variable, pIndexBefore + 1);
                  return fmgr.makeEqual(variableAfter, variableBefore);
                });

    return fmgr.getBooleanFormulaManager().and(unchangedFormulas.toSet());
  }
}
