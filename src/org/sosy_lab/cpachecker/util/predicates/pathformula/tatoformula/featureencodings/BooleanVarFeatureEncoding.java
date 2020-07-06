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

public abstract class BooleanVarFeatureEncoding<T> {
  private Table<TaDeclaration, T, String> variableNames;
  protected final FormulaManagerView fmgr;

  public BooleanVarFeatureEncoding(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    variableNames = HashBasedTable.create();
  }

  protected void addEntry(TaDeclaration pAutomaton, T pValue, String pVariableName) {
    variableNames.put(pAutomaton, pValue, pVariableName);
  }

  protected void addEntryToAllAutomata(T pValue, String pVariableName) {
    variableNames.rowKeySet().forEach(automaton -> addEntry(automaton, pValue, pVariableName));
  }

  private BooleanFormula makeVariableFormula(String variableName, int variableIndex) {
    return fmgr.getBooleanFormulaManager().makeVariable(variableName, variableIndex);
  }

  public BooleanFormula makeEqualsFormula(T feature, TaDeclaration pAutomaton, int pVariableIndex) {
    return makeVariableFormula(variableNames.get(pAutomaton, feature), pVariableIndex);
  }

  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pNextIndex) {
    var unchangedFormulas =
        from(variableNames.row(pAutomaton).values())
            .transform(
                variable -> {
                  var variableBefore = makeVariableFormula(variable, pNextIndex - 1);
                  var variableAfter = makeVariableFormula(variable, pNextIndex);
                  return fmgr.makeEqual(variableAfter, variableBefore);
                });

    return fmgr.getBooleanFormulaManager().and(unchangedFormulas.toSet());
  }
}
