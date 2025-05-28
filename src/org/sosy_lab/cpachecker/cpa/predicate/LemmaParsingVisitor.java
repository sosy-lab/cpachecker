// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaParsingVisitor extends DefaultFormulaVisitor<String> {
  FormulaManagerView fmgr;
  String id;

  public LemmaParsingVisitor(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    id = null;
  }

  @Override
  public String visitFunction(
      Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
    if (functionDeclaration.getName().startsWith("_L_")) {
      id = functionDeclaration.getName();
    }
    for (Formula arg : args) {
      fmgr.visit(arg, this);
    }
    return id;
  }

  @Override
  protected String visitDefault(Formula pFormula) {
    return null;
  }
}
