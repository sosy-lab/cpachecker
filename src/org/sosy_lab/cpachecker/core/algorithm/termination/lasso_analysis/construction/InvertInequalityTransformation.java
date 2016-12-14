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
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

import java.util.List;

class InvertInequalityTransformation extends DefaultFormulaVisitor<BooleanFormula> {

  private final FormulaManagerView fmgr;

  InvertInequalityTransformation(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  @Override
  protected BooleanFormula visitDefault(Formula pF) {
    return (BooleanFormula) pF;
  }

  @Override
  public BooleanFormula visitFunction(
      Formula pF, List<Formula> pNewArgs, FunctionDeclaration<?> pFunctionDeclaration) {

    if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GTE)
        || pFunctionDeclaration.getName().equals(">=")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeLessThan(pNewArgs.get(0), pNewArgs.get(1), true);

    } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LTE)
        || pFunctionDeclaration.getName().equals("<=")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeGreaterThan(pNewArgs.get(0), pNewArgs.get(1), true);

    } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GT)
        || pFunctionDeclaration.getName().equals(">")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeLessOrEqual(pNewArgs.get(0), pNewArgs.get(1), true);

    } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LT)
        || pFunctionDeclaration.getName().equals("<")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeGreaterOrEqual(pNewArgs.get(0), pNewArgs.get(1), true);

    } else {
      return super.visitFunction(pF, pNewArgs, pFunctionDeclaration);
    }
  }
}
