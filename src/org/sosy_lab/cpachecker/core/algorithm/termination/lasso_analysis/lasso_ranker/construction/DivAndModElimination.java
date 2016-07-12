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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.construction;

import static org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker.construction.LassoBuilder.TERMINATION_AUX_VARS_PREFIX;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.DIV;
import static org.sosy_lab.solver.api.FunctionDeclarationKind.MODULO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import de.uni_freiburg.informatik.ultimate.lassoranker.preprocessors.RewriteDivision;

import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FunctionDeclaration;
import org.sosy_lab.solver.visitors.DefaultFormulaVisitor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class DivAndModElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgrView;
  private final FormulaManager fmgr;

  DivAndModElimination(FormulaManagerView pFmgrView, FormulaManager pFmgr) {
    super(pFmgrView);
    fmgrView = pFmgrView;
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    DivAndModTransformation divAndModTransformation = new DivAndModTransformation(fmgrView, fmgr);
    BooleanFormula result = (BooleanFormula) fmgrView.visit(pAtom, divAndModTransformation);

    BooleanFormulaManagerView booleanFormulaManager = fmgrView.getBooleanFormulaManager();
    BooleanFormula additionalAxioms =
        booleanFormulaManager.and(divAndModTransformation.getAdditionalAxioms());
    return fmgrView.makeAnd(result, additionalAxioms);
  }

  /**
   * Replaces division and modulo by linear formulas and auxiliary variables.
   *
   * <pre>
   * Note: The remainder will be always non negative as defined in the SMT-LIB standard
   *       (http://smtlib.cs.uiowa.edu/theories-Ints.shtml)
   * <pre>
   */
  private static class DivAndModTransformation extends DefaultFormulaVisitor<Formula> {

    private final static UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();

    private final FormulaManagerView fmgrView;
    private final FormulaManager fmgr;

    private final Collection<BooleanFormula> additionalAxioms;

    private DivAndModTransformation(FormulaManagerView pFmgrView, FormulaManager pFmgr) {
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

      List<Formula> newArgs =
          pArgs.stream().map(f -> fmgrView.visit(f, this)).collect(Collectors.toList());

      if (pFunctionDeclaration.getKind().equals(DIV)
          || pFunctionDeclaration.getName().equalsIgnoreCase("div")) {
        assert newArgs.size() == 2;
        return transformDivision(newArgs.get(0), newArgs.get(1), pFunctionDeclaration.getType());

      } else if (pFunctionDeclaration.getKind().equals(MODULO)
          || pFunctionDeclaration.getName().equalsIgnoreCase("mod")) {
        assert newArgs.size() == 2;
        return transformModulo(newArgs.get(0), newArgs.get(1), pFunctionDeclaration.getType());

      } else {
        return fmgr.makeApplication(pFunctionDeclaration, newArgs);
      }
    }

    /**
     * Transform a modulo operation into a new linear {@link Formula}
     * and adds it to {@link #additionalAxioms}.
     * The returned {@link Formula} represents the modulo opertion's result
     * if that {@link Formula} is satisfied.
     *
     * @return a {@link Formula} representing the result of the modulo operation
     *
     * @see RewriteDivision
     */
    private Formula transformModulo(Formula dividend, Formula divisor, FormulaType<?> formulaType) {
      BooleanFormulaManagerView booleanFormulaManager = fmgrView.getBooleanFormulaManager();

      Formula quotientAuxVar =
          fmgr.makeVariable(
              formulaType,
              TERMINATION_AUX_VARS_PREFIX + "QUOTIENT_AUX_VAR_" + ID_GENERATOR.getFreshId());
      Formula remainderAuxVar =
          fmgr.makeVariable(
              formulaType,
              TERMINATION_AUX_VARS_PREFIX + "REMAINDER_AUX_VAR_" + ID_GENERATOR.getFreshId());
      /*
       * dividend = quotientAuxVar * divisor + remainderAuxVar
       * divisor > 0 ==> 0 <= remainderAuxVar < divisor
       * divisor < 0 ==> 0 <= remainderAuxVar < -divisor
       */

      Formula one = fmgrView.makeNumber(formulaType, 1);
      Formula zero = fmgrView.makeNumber(formulaType, 0);

      BooleanFormula divisorIsNegative = fmgrView.makeLessThan(divisor, zero, true);
      BooleanFormula divisorIsPositive = fmgrView.makeGreaterThan(divisor, zero, true);
      BooleanFormula isLowerBound = fmgrView.makeLessOrEqual(zero, remainderAuxVar, true);
      Formula upperBoundPosDivisor = fmgrView.makeMinus(divisor, one);
      BooleanFormula isUpperBoundPosDivisor =
          fmgrView.makeLessOrEqual(remainderAuxVar, upperBoundPosDivisor, true);
      Formula upperBoundNegDivisor = fmgrView.makeMinus(one, divisor);
      BooleanFormula isUpperBoundNegDivisor =
          fmgrView.makeLessOrEqual(remainderAuxVar, upperBoundNegDivisor, true);
      BooleanFormula equality =
          fmgrView.makeEqual(
              dividend,
              fmgrView.makePlus(fmgrView.makeMultiply(quotientAuxVar, divisor), remainderAuxVar));

      BooleanFormula divisorIsPositiveFormula =
          booleanFormulaManager.and(
              divisorIsPositive, isLowerBound, isUpperBoundPosDivisor, equality);
      BooleanFormula divisorIsNegativeFormula =
          booleanFormulaManager.and(
              divisorIsNegative, isLowerBound, isUpperBoundNegDivisor, equality);

      additionalAxioms.add(fmgrView.makeOr(divisorIsPositiveFormula, divisorIsNegativeFormula));
      return remainderAuxVar;
    }

    /**
     * Transform a division operation into a new linear {@link Formula}
     * and adds it to {@link #additionalAxioms}.
     * The returned {@link Formula} represents the divsion's result
     * if that {@link Formula} is satisfied.
     *
     * @return a {@link Formula} representing the result of the division operation
     *
     * @see RewriteDivision
     */
    private Formula transformDivision(
        Formula dividend, Formula divisor, FormulaType<?> formulaType) {
      BooleanFormulaManagerView booleanFormulaManager = fmgrView.getBooleanFormulaManager();

      Formula quotientAuxVar =
          fmgr.makeVariable(
              formulaType,
              LassoBuilder.TERMINATION_AUX_VARS_PREFIX
                  + "QUOTIENT_AUX_VAR_"
                  + ID_GENERATOR.getFreshId());

      /*
       * (divisor > 0 ==> quotientAuxVar * divisor <= dividend < (quotientAuxVar+1) * divisor)
       * and
       * (divisor < 0 ==> quotientAuxVar * divisor <= dividend < (quotientAuxVar-1) * divisor)
       */

      Formula one = fmgrView.makeNumber(formulaType, 1);
      Formula zero = fmgrView.makeNumber(formulaType, 0);

      BooleanFormula divisorIsNegative = fmgrView.makeLessThan(divisor, zero, true);
      BooleanFormula divisorIsPositive = fmgrView.makeGreaterThan(divisor, zero, true);
      Formula quotientMulDivisor = fmgrView.makeMultiply(quotientAuxVar, divisor);
      BooleanFormula isLowerBound = fmgrView.makeLessOrEqual(quotientMulDivisor, dividend, true);

      Formula strictUpperBoundPosDivisor =
          fmgrView.makeMultiply(fmgrView.makePlus(quotientAuxVar, one), divisor);
      Formula strictUpperBoundNegDivisor =
          fmgrView.makeMultiply(fmgrView.makeMinus(quotientAuxVar, one), divisor);
      BooleanFormula isUpperBoundPosDivisor =
          fmgrView.makeLessThan(dividend, strictUpperBoundPosDivisor, true);
      BooleanFormula isUpperBoundNegDivisor =
          fmgrView.makeLessThan(dividend, strictUpperBoundNegDivisor, true);

      BooleanFormula divisorIsPositiveFormula =
          booleanFormulaManager.and(divisorIsPositive, isLowerBound, isUpperBoundPosDivisor);
      BooleanFormula divisorIsNegativeFormula =
          booleanFormulaManager.and(divisorIsNegative, isLowerBound, isUpperBoundNegDivisor);

      additionalAxioms.add(fmgrView.makeOr(divisorIsPositiveFormula, divisorIsNegativeFormula));

      return quotientAuxVar;
    }
  }
}
