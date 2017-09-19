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
import static org.sosy_lab.java_smt.api.FunctionDeclarationKind.MUL;
import static org.sosy_lab.java_smt.api.FunctionDeclarationKind.UF;

import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

class NonLinearMultiplicationElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgrView;
  private final FormulaManager fmgr;

  NonLinearMultiplicationElimination(FormulaManagerView pFmgrView, FormulaManager pFmgr) {
    super(pFmgrView);
    fmgrView = pFmgrView;
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    NonLinearMultiplicationTransformation multiplicationTransformation =
        new NonLinearMultiplicationTransformation(fmgrView, fmgr);
    BooleanFormula result = (BooleanFormula) fmgrView.visit(pAtom, multiplicationTransformation);

    BooleanFormulaManagerView booleanFormulaManager = fmgrView.getBooleanFormulaManager();
    BooleanFormula additionalAxioms =
        booleanFormulaManager.and(multiplicationTransformation.getAdditionalAxioms());
    return fmgrView.makeAnd(result, additionalAxioms);
  }

  /**
   * Replaces non-linear multiplication by linear formulas and an auxiliary variable.
   */
  private static class NonLinearMultiplicationTransformation
      extends DefaultFormulaVisitor<Formula> {

    private final static UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

    private final FormulaManagerView fmgrView;
    private final FormulaManager fmgr;

    private final Collection<BooleanFormula> additionalAxioms;

    private NonLinearMultiplicationTransformation(
        FormulaManagerView pFmgrView, FormulaManager pFmgr) {
      fmgrView = pFmgrView;
      fmgr = pFmgr;
      additionalAxioms = Lists.newArrayList();
    }

    public Collection<BooleanFormula> getAdditionalAxioms() {
      return ImmutableList.copyOf(additionalAxioms);
    }

    @Override
    protected Formula visitDefault(Formula pF) {
      return pF;
    }

    @Override
    public Formula visitFunction(
        Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {

      List<Formula> newArgs = Lists.transform(pArgs, f -> fmgrView.visit(f, this));

      if ((pFunctionDeclaration.getKind().equals(UF)
              && pFunctionDeclaration.getName().equalsIgnoreCase("Integer__*_"))
          || (pFunctionDeclaration.getKind().equals(MUL)
              && !isConstant(newArgs.get(0))
              && !isConstant(newArgs.get(1)))) {
        assert newArgs.size() == 2;
        return transformNonLinearMultiplication(
            newArgs.get(0), newArgs.get(1), pFunctionDeclaration.getType());

      } else {
        return fmgr.makeApplication(pFunctionDeclaration, newArgs);
      }
    }

    private boolean isConstant(Formula pF) {
      AtomicBoolean constant = new AtomicBoolean(true);
      fmgrView.visitRecursively(
          pF,
          new DefaultFormulaVisitor<TraversalProcess>() {

            @Override
            protected TraversalProcess visitDefault(Formula pF) {
              constant.set(false);
              return TraversalProcess.ABORT;
            }

            @Override
            public TraversalProcess visitConstant(Formula pF, Object pValue) {
              return TraversalProcess.CONTINUE;
            }
          });

      return constant.get();
    }

    /**
     * Transform a non linear multiplication operation into a new linear {@link Formula}
     * and adds it to {@link #additionalAxioms}.
     * The returned {@link Formula} represents the multiplication opertion's result
     * if that {@link Formula} is satisfied.
     *
     * @return a {@link Formula} representing the result of the multiplication operation
     */
    private Formula transformNonLinearMultiplication(
        Formula a, Formula b, FormulaType<?> formulaType) {
      BooleanFormulaManagerView bfmgr = fmgrView.getBooleanFormulaManager();

      Formula multAux =
          fmgr.makeVariable(
              formulaType,
              TERMINATION_AUX_VARS_PREFIX + "MULT_AUX_VAR_" + ID_GENERATOR.getFreshId());

      List<BooleanFormula> cases = Lists.newArrayList();
      Formula one = fmgrView.makeNumber(formulaType, 1);
      Formula minusOne = fmgrView.makeNumber(formulaType, -1);
      Formula zero = fmgrView.makeNumber(formulaType, 0);

      // a * 0 = 0, 0 * b = 0
      BooleanFormula aIsZero = fmgrView.makeEqual(a, zero);
      BooleanFormula bIsZero = fmgrView.makeEqual(b, zero);
      BooleanFormula factorIsZero = fmgrView.makeOr(aIsZero, bIsZero);
      cases.add(fmgrView.makeAnd(factorIsZero, fmgrView.makeEqual(multAux, zero)));

      // a * 1 = a, 1 * b = b
      BooleanFormula aIsOne = fmgrView.makeEqual(a, minusOne);
      BooleanFormula bIsOne = fmgrView.makeEqual(b, minusOne);
      cases.add(fmgrView.makeAnd(bIsOne, fmgrView.makeEqual(multAux, a)));
      cases.add(fmgrView.makeAnd(aIsOne, fmgrView.makeEqual(multAux, b)));

      // a * (-1) = -a, (-1) * b = -b
      BooleanFormula aIsminusOne = fmgrView.makeEqual(a, one);
      BooleanFormula bIsminusOne = fmgrView.makeEqual(b, one);
      Formula minusA = fmgrView.makeNegate(a);
      Formula minusB = fmgrView.makeNegate(b);
      cases.add(fmgrView.makeAnd(aIsminusOne, fmgrView.makeEqual(multAux, minusA)));
      cases.add(fmgrView.makeAnd(bIsminusOne, fmgrView.makeEqual(multAux, minusB)));

      // 0 < a < 1,  0 < b < 1, -1 < a < 0, -1 < b < 0, a > 1, ...
      BooleanFormula zeroLessALessOne =
          bfmgr.and(fmgrView.makeLessThan(zero, a, true), fmgrView.makeLessThan(a, one, true));
      BooleanFormula zeroLessBLessOne =
          bfmgr.and(fmgrView.makeLessThan(zero, b, true), fmgrView.makeLessThan(b, one, true));
      BooleanFormula minusOneLessALessZero =
          bfmgr.and(fmgrView.makeLessThan(minusOne, a, true), fmgrView.makeLessThan(a, zero, true));
      BooleanFormula minusOneLessBLessZero =
          bfmgr.and(fmgrView.makeLessThan(minusOne, b, true), fmgrView.makeLessThan(b, zero, true));
      BooleanFormula aGreaterOne = bfmgr.and(fmgrView.makeGreaterThan(a, one, true));
      BooleanFormula bGreaterOne = bfmgr.and(fmgrView.makeGreaterThan(b, one, true));
      BooleanFormula aLessMinuseOne = bfmgr.and(fmgrView.makeLessThan(a, one, true));
      BooleanFormula bLessMinuseOne = bfmgr.and(fmgrView.makeLessThan(b, one, true));

      // 0 < multAux < 1, -1 < multAux < 0, multAux > 1, ...
      BooleanFormula zeroLessResultLessOne =
          bfmgr.and(
              fmgrView.makeLessThan(zero, multAux, true),
              fmgrView.makeLessThan(multAux, one, true));
      BooleanFormula minusOneLessResultLessZero =
          bfmgr.and(
              fmgrView.makeLessThan(minusOne, multAux, true),
              fmgrView.makeLessThan(multAux, zero, true));
      BooleanFormula resultGreaterOne = bfmgr.and(fmgrView.makeGreaterThan(multAux, one, true));
      BooleanFormula resultLessMinuseOne = bfmgr.and(fmgrView.makeLessThan(multAux, one, true));
      BooleanFormula positiveResult = fmgrView.makeGreaterThan(multAux, zero, true);
      BooleanFormula negativeResult = fmgrView.makeLessThan(multAux, zero, true);

      cases.add(bfmgr.and(zeroLessALessOne, zeroLessBLessOne, zeroLessResultLessOne));
      cases.add(bfmgr.and(zeroLessALessOne, minusOneLessBLessZero, minusOneLessResultLessZero));
      cases.add(bfmgr.and(zeroLessALessOne, bGreaterOne, positiveResult));
      cases.add(bfmgr.and(zeroLessALessOne, bLessMinuseOne, negativeResult));
      cases.add(bfmgr.and(minusOneLessALessZero, zeroLessBLessOne, minusOneLessResultLessZero));
      cases.add(bfmgr.and(minusOneLessALessZero, minusOneLessBLessZero, zeroLessResultLessOne));
      cases.add(bfmgr.and(minusOneLessALessZero, bGreaterOne, negativeResult));
      cases.add(bfmgr.and(minusOneLessALessZero, bLessMinuseOne, positiveResult));
      cases.add(bfmgr.and(aGreaterOne, zeroLessBLessOne, positiveResult));
      cases.add(bfmgr.and(aGreaterOne, minusOneLessBLessZero, negativeResult));
      cases.add(bfmgr.and(aGreaterOne, bGreaterOne, resultGreaterOne));
      cases.add(bfmgr.and(aGreaterOne, bLessMinuseOne, resultLessMinuseOne));
      cases.add(bfmgr.and(aLessMinuseOne, zeroLessBLessOne, negativeResult));
      cases.add(bfmgr.and(aLessMinuseOne, minusOneLessBLessZero, positiveResult));
      cases.add(bfmgr.and(aLessMinuseOne, bGreaterOne, resultLessMinuseOne));
      cases.add(bfmgr.and(aLessMinuseOne, bLessMinuseOne, resultGreaterOne));

      additionalAxioms.add(bfmgr.or(cases));
      return multAux;
    }
  }
}
