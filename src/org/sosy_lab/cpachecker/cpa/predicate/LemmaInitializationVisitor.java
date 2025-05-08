// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaInitializationVisitor extends DefaultFormulaVisitor<Formula> {
  private final Map<Formula, Formula> variableMap;
  private final FormulaManager fmgr;

  public LemmaInitializationVisitor(Map<Formula, Formula> pMap, FormulaManager pFmgr) {
    variableMap = pMap;
    fmgr = pFmgr;
  }

  @Override
  public Formula visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    List<Formula> newArgs = new ArrayList<>();
    for (Formula arg : args) {
      newArgs.add(fmgr.visit(arg, this));
    }
    return fmgr.makeApplication(functionDeclaration, newArgs);
  }

  @Override
  public Formula visitFreeVariable(Formula f, String name) {
    if (variableMap.containsKey(f)) {
      return variableMap.get(f);
    }
    return f;
  }

  @Override
  protected Formula visitDefault(Formula pFormula) {
    return pFormula;
  }
}
