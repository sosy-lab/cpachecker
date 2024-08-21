// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Tactic;

/** Class for converting a formula to a C expression. */
public class FormulaToCExpressionConverter {
  private final FormulaManagerView fmgr;

  public FormulaToCExpressionConverter(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  /** Convert the input formula to a C expression. */
  public String formulaToCExpression(BooleanFormula input) throws InterruptedException {
    BooleanFormula nnfied = fmgr.applyTactic(input, Tactic.NNF);
    BooleanFormula simplified = fmgr.simplify(nnfied);
    FormulaToCExpressionVisitor visitor = new FormulaToCExpressionVisitor(fmgr);
    fmgr.transformRecursively(simplified, visitor);
    return visitor.getCExpressionForFormula(simplified);
  }
}
