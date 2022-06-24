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
      HashMap<Formula,Formula> tosubstiture = new HashMap<>();
//       modify fmap
//      for (Formula key : fmap.keySet()) {
//        if (!formulaInSsaMap(key)) {
//          tosubstiture.put(key, fmap.get(key));
//        }
//      }
//      atom = fmgr.substitute(atom, tosubstiture);
//      boolean changed = true;
//      while (changed) {
//        BooleanFormula atomOld = atom;
//        atom = fmgr.substitute(atom, fmap);
//        changed = !(atom.equals(atomOld));
//      }

      atom = fmgr.substitute(atom, fmap);
    }
    return atom;
  }


  /**
   * Checks if a variable along with it's index is in the SSAMap
   * @param f Formula consisting of only one variable in the form `name@index`
   * @return the result of this check
   */
  private boolean formulaInSsaMap(Formula f){
    Map<String, Formula> vars = fmgr.extractVariables(f);
    if (vars.size()!=1) {
      // TODO Martin reenable error
      return true;
//      throw new IllegalArgumentException("Error checking if variable index in SSAMAP: " + f.toString() +
//          "\nNot exactly one variable in f");
    }
    Pair<String, OptionalInt> stringOptionalIntPair = FormulaManagerView.parseName(vars.keySet().iterator().next());
    assert stringOptionalIntPair.getFirst() != null;
    if (stringOptionalIntPair.getFirst().isEmpty() || stringOptionalIntPair.getSecond().isEmpty()) {
      // TODO Martin Does javasmt have a unified logging system?
      // TODO Martin reenable error
      return true;
//      throw new IllegalArgumentException("Error checking if variable index in SSAMAP: " + f.toString());
    }
//    String[] parts = f.toString().split("@");
    return ssaMap.containsVariable(stringOptionalIntPair.getFirst()) && (ssaMap.getIndex(stringOptionalIntPair.getFirst()) == stringOptionalIntPair.getSecond().getAsInt());
  }
}
