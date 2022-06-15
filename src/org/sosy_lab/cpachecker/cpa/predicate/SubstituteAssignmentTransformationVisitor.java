// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

public class SubstituteAssignmentTransformationVisitor extends org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor {

  private final FormulaManager fmgr;
  private final HashMap<Formula,Formula> fmap;
  private final SSAMap ssaMap;

  /**
   * Transform a formula using a map of substitutions constructed by SubstituteVisitor
   *
   * We replace only those variable occurences that do not have the highest SSA index for their variable
   * @param pFmgr {@link FormulaManager}
   * @param pFmap map of substitutions as constructed by {@link SubstituteVisitor}
   * @param pSSAMap {@link SSAMap} containing the most recent SSA Indices for all variables
   */
  public SubstituteAssignmentTransformationVisitor(FormulaManager pFmgr, HashMap<Formula,Formula> pFmap, SSAMap pSSAMap) {
    super(pFmgr);
    fmgr = pFmgr;
    fmap = pFmap;
    ssaMap = pSSAMap;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
    // only substitute Assignments and Equal assumptions??? TODO also equal assumptions?
    if (decl.getKind() == FunctionDeclarationKind.EQ) {
//      HashMap<Formula,Formula> tosubstiture = new HashMap<>();
//      // modify fmap
//      for (Formula key : fmap.keySet()) {
//        if (!formulaInSsaMap(key)) {
//          tosubstiture.put(key, fmap.get(key));
//        }
//      }
      atom = fmgr.substitute(atom, fmap);
    }
    return atom;
  }
}
