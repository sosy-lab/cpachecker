// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Map;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

public class SubstituteAssumptionTransformationVisitor extends org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor {

  private final FormulaManager fmgr;
  private final Map<Formula,Formula> fmap;

  public SubstituteAssumptionTransformationVisitor(FormulaManager pFmgr, Map<Formula,Formula> pFmap) {
    super(pFmgr);
    fmgr = pFmgr;
    fmap = pFmap;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
    // Substitution:
    // if not an assignment, substitute every occurence
    if (decl.getKind() != FunctionDeclarationKind.EQ) {
      atom = fmgr.substitute(atom, fmap);
    }
    return atom;
  }
}
