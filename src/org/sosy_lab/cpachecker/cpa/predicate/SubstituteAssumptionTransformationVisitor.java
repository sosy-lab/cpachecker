// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import java.util.HashMap;

public class SubstituteAssumptionTransformationVisitor extends org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor {

  private final BooleanFormulaManager bfmgr;
  private final FormulaManager fmgr;
  private HashMap<Formula,Formula> fmap;

  public SubstituteAssumptionTransformationVisitor(FormulaManager pFmgr, HashMap<Formula,Formula> pFmap) {
    super(pFmgr);
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
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
