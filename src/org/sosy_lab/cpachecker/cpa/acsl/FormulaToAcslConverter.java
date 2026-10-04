// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/** Class for converting a formula to an Acsl expression. */
public class FormulaToAcslConverter {
  private final FormulaManagerView fmgr;

  public FormulaToAcslConverter(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  /** Convert the input formula to an Acsl expression. */
  public AcslPredicate formulaToAcslExpression(BooleanFormula input)
      throws InterruptedException, SolverException {
    // BooleanFormula nnfied = fmgr.applyTactic(input, Tactic.NNF);
    // BooleanFormula simplified = fmgr.simplify(nnfied);
    FormulaToAcslVisitor visitor = new FormulaToAcslVisitor(fmgr);
    fmgr.transformRecursively(input, visitor);
    return (AcslPredicate) visitor.getAcslExpressionForFormula(input);
  }
}
