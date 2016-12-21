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

import static org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder.TERMINATION_AUX_VARS_PREFIX;
import static org.sosy_lab.java_smt.api.FunctionDeclarationKind.ITE;

import com.google.common.collect.Lists;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

import java.util.Collection;
import java.util.List;

class IfThenElseElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgrView;
  private final FormulaManager fmgr;

  IfThenElseElimination(FormulaManagerView pFmgrView, FormulaManager pFmgr) {
    super(pFmgrView);
    fmgrView = pFmgrView;
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    IfThenElseTransformation ifThenElseTransformation =
        new IfThenElseTransformation(fmgrView, fmgr);
    BooleanFormula result = (BooleanFormula) fmgrView.visit(pAtom, ifThenElseTransformation);

    BooleanFormulaManagerView booleanFormulaManager = fmgrView.getBooleanFormulaManager();
    BooleanFormula additionalAxioms =
        booleanFormulaManager.and(ifThenElseTransformation.getAdditionalAxioms());
    return fmgrView.makeAnd(result, additionalAxioms);
  }

  private static class IfThenElseTransformation extends DefaultFormulaVisitor<Formula> {

    private final static UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

    private final FormulaManagerView fmgrView;
    private final FormulaManager fmgr;

    private final Collection<BooleanFormula> additionalAxioms;

    private IfThenElseTransformation(FormulaManagerView pFmgrView, FormulaManager pFmgr) {
      fmgrView = pFmgrView;
      fmgr = pFmgr;
      additionalAxioms = Lists.newArrayList();
    }

    public Collection<BooleanFormula> getAdditionalAxioms() {
      return additionalAxioms;
    }

    @Override
    protected Formula visitDefault(Formula pF) {
      return pF;
    }

    @Override
    public Formula visitFunction(
        Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
      List<Formula> newArgs = Lists.transform(pArgs, f -> fmgrView.visit(f, this));

      if (pFunctionDeclaration.getKind().equals(ITE)
          || pFunctionDeclaration.getName().equalsIgnoreCase("ite")) {
        return transformIfThenElse(
            newArgs.get(0), newArgs.get(1), newArgs.get(2), pFunctionDeclaration.getType());

      } else {
        return fmgr.makeApplication(pFunctionDeclaration, newArgs);
      }
    }

    private Formula transformIfThenElse(
        Formula pCondition, Formula pThen, Formula pElse, FormulaType<?> pFormulaType) {

      BooleanFormula condition = (BooleanFormula) pCondition;
      Formula auxVar =
          fmgr.makeVariable(
              pFormulaType,
              TERMINATION_AUX_VARS_PREFIX + "IF_THEN_ELSE_AUX_VAR_" + ID_GENERATOR.getFreshId());

      // (auxVar == pThen AND condition) OR (auxVar == pThen AND (NOT(condition)))
      additionalAxioms.add(
          fmgrView.makeOr(
              fmgrView.makeAnd(fmgrView.makeEqual(auxVar, pThen), condition),
              fmgrView.makeAnd(fmgrView.makeEqual(auxVar, pElse), fmgrView.makeNot(condition))));

      return auxVar;
    }
  }
}
