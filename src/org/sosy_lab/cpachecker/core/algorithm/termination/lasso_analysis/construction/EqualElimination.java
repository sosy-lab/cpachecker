/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

import java.util.List;

class EqualElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgr;

  EqualElimination(FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    if (pDecl.getKind().equals(FunctionDeclarationKind.EQ)) {
      List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pAtom);

      if (split.size() == 1) {
        return split.get(0);

      } else if (split.size() == 2) {
        return fmgr.makeAnd(split.get(0), split.get(1));

      } else {
        throw new AssertionError();
      }

    } else {
      return super.visitAtom(pAtom, pDecl);
    }
  }
}
