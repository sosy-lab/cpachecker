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
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

public class SubstituteAssignmentTransformationVisitor extends org.sosy_lab.java_smt.api.visitors.BooleanFormulaTransformationVisitor {

  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgrView;
  private final FormulaManager fmgr;
  private final HashMap<Formula,Formula> fmap;
  private final SSAMap ssaMap;

  public SubstituteAssignmentTransformationVisitor(FormulaManager pFmgr, FormulaManagerView pFmgrView, HashMap<Formula,Formula> pFmap, SSAMap pSSAMap) {
    super(pFmgr);
    fmgr = pFmgr;
    fmgrView = pFmgrView;
    bfmgr = pFmgr.getBooleanFormulaManager();
    fmap = pFmap;
    ssaMap = pSSAMap;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula atom, FunctionDeclaration<BooleanFormula> decl) {
    // only substitute Assignments and Equal assumptions??? TODO also equal assumptions?
    if (decl.getKind() == FunctionDeclarationKind.EQ) {
      HashMap<Formula,Formula> tosubstiture = new HashMap<>();
      // modify fmap
      for (Formula key : fmap.keySet()) {
        if (!formulaInSSAMAP(key, ssaMap)) {
          tosubstiture.put(key, fmap.get(key));
        }
      }
      atom = fmgr.substitute(atom, tosubstiture);
    }
    return atom;
  }

  private boolean formulaInSSAMAP(Formula f, SSAMap pSSAMap){
    Map<String, Formula> vars = fmgr.extractVariables(f);
    Pair<String, OptionalInt> stringOptionalIntPair = FormulaManagerView.parseName(vars.keySet().iterator().next());
    // error
    if (stringOptionalIntPair.getFirst().isEmpty() || !stringOptionalIntPair.getSecond().isPresent())
      throw new IllegalArgumentException(
          "Error checking if variable index in SSAMAP: " + f.toString());
//    String[] parts = f.toString().split("@");
    return ssaMap.containsVariable(stringOptionalIntPair.getFirst()) && (ssaMap.getIndex(stringOptionalIntPair.getFirst()) == stringOptionalIntPair.getSecond().getAsInt());
  }
}
