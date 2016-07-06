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

import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.api.FunctionDeclarationKind;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

class IfThenElseElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgr;

  private final IfThenElseElimination.IfThenElseTransformation ifThenElseTransformation;

  IfThenElseElimination(FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr;
    ifThenElseTransformation = new IfThenElseTransformation(pFmgr);
  }

  @Override
  public BooleanFormula visitAtom(
      BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    if (LassoBuilder.IF_THEN_ELSE_FUNCTIONS.contains(pDecl.getKind())) {
      return fmgr.visit(ifThenElseTransformation, pAtom);
    } else {
      return pAtom;
    }
  }

  private static class IfThenElseTransformation extends DefaultFormulaVisitor<BooleanFormula> {

    private final FormulaManagerView fmgr;

    private IfThenElseTransformation(FormulaManagerView pFmgr) {
      fmgr = pFmgr;
    }

    @Override
    protected BooleanFormula visitDefault(Formula pF) {
      return (BooleanFormula) pF;
    }

    @Override
    public BooleanFormula visitFunction(
        Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {

      FunctionDeclarationKind kind = pFunctionDeclaration.getKind();
      if (LassoBuilder.IF_THEN_ELSE_FUNCTIONS.contains(kind)) {

        Optional<Triple<BooleanFormula, Formula, Formula>> ifThenElse =
            fmgr.splitIfThenElse(pArgs.get(1));

        // right hand side is if-then-else
        if (ifThenElse.isPresent()) {
          return transformIfThenElse(getFunction(kind, false), pArgs.get(0), ifThenElse);

        } else { // check left hand side
          ifThenElse = fmgr.splitIfThenElse(pArgs.get(0));

          // left hand side is if-then-else
          if (ifThenElse.isPresent()) {
            return transformIfThenElse(getFunction(kind, false), pArgs.get(1), ifThenElse);
          }
        }
      }

      return (BooleanFormula) pF;
    }

    private BooleanFormula transformIfThenElse(
        BiFunction<Formula, Formula, BooleanFormula> function,
        Formula otherArg,
        Optional<Triple<BooleanFormula, Formula, Formula>> ifThenElse) {

      BooleanFormula condition = ifThenElse.get().getFirst();
      Formula thenFomula = ifThenElse.get().getSecond();
      Formula elseFomula = ifThenElse.get().getThird();
      return fmgr.makeOr(
          fmgr.makeAnd(function.apply(otherArg, thenFomula), condition),
          fmgr.makeAnd(function.apply(otherArg, elseFomula), fmgr.makeNot(condition)));
    }

    private BiFunction<Formula, Formula, BooleanFormula> getFunction(
        FunctionDeclarationKind functionKind, boolean swapArguments) {
      BiFunction<Formula, Formula, BooleanFormula> baseFunction;
      switch (functionKind) {
        case EQ:
          baseFunction = fmgr::makeEqual;
          break;
        case GT:
          baseFunction = (f1, f2) -> fmgr.makeLessOrEqual(f1, f2, true);
          break;
        case GTE:
          baseFunction = (f1, f2) -> fmgr.makeLessOrEqual(f1, f2, true);
          break;
        case LT:
          baseFunction = (f1, f2) -> fmgr.makeLessOrEqual(f1, f2, true);
          break;
        case LTE:
          baseFunction = (f1, f2) -> fmgr.makeGreaterThan(f1, f2, true);
          break;

        default:
          throw new AssertionError();
      }

      BiFunction<Formula, Formula, BooleanFormula> function;
      if (swapArguments) {
        function = (f1, f2) -> baseFunction.apply(f2, f1);
      } else {
        function = baseFunction;
      }
      return function;
    }
  }
}