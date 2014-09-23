/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.princess;

import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;
import static org.sosy_lab.cpachecker.util.predicates.princess.PrincessUtil.*;

import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractNumeralFormulaManager;

import ap.basetypes.IdealInt;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IIntFormula;
import ap.parser.IIntLit;
import ap.parser.IIntRelation;
import ap.parser.IPlus;
import ap.parser.ITerm;
import ap.parser.ITimes;

import com.google.common.collect.ImmutableList;


abstract class PrincessNumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<IExpression, TermType, PrincessEnvironment, ParamFormulaType, ResultFormulaType> {

  private final PrincessFunctionType<ResultFormulaType> multUfDecl;
  private final PrincessFunctionType<ResultFormulaType> divUfDecl;
  private final PrincessFunctionType<ResultFormulaType> modUfDecl;
  private final PrincessFunctionFormulaManager functionManager;

  PrincessNumeralFormulaManager(
          PrincessFormulaCreator pCreator,
          PrincessFunctionFormulaManager pFunctionManager) {
    super(pCreator);
    functionManager = pFunctionManager;

    FormulaType<ResultFormulaType> formulaType = getFormulaType();
    multUfDecl = functionManager.createFunction(formulaType + "_" + MultUfName, formulaType, formulaType, formulaType);
    divUfDecl = functionManager.createFunction(formulaType + "_" + DivUfName, formulaType, formulaType, formulaType);
    modUfDecl = functionManager.createFunction(formulaType + "_" + ModUfName, formulaType, formulaType, formulaType);
  }

  private IExpression makeUf(FunctionFormulaType<?> decl, IExpression t1, IExpression t2) {
    return functionManager.createUninterpretedFunctionCallImpl(decl, ImmutableList.of(t1, t2));
  }

  private boolean isUf(PrincessFunctionType<?> funcDecl, IExpression pBits) {
    return functionManager.isUninterpretedFunctionCall(funcDecl, pBits);
  }

  @Override
  public ITerm negate(IExpression pNumber) {
    return castToTerm(pNumber).unary_$minus();
  }

  @Override
  public boolean isNegate(IExpression pNumber) {
    boolean mult = isMultiply(pNumber);
    if (!mult) {
      return false;
    }
    IExpression arg = PrincessUtil.getArg(pNumber, 0);
    if (PrincessUtil.isNumber(arg)) {
      // TODO: BUG: possible bug
      return PrincessUtil.toNumber(arg) == -1;
    }
    return false;
  }

  @Override
  public ITerm add(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$plus(castToTerm(pNumber2));
  }

  @Override
  public boolean isAdd(IExpression pNumber) {
    return pNumber instanceof IPlus;
  }

  @Override
  public ITerm subtract(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$minus(castToTerm(pNumber2));
  }

  @Override
  public boolean isSubtract(IExpression pNumber) {
    // Princess does not support Minus.
    // Formulas are converted from "a-b" to "a+(-b)".
    return false;
  }

  @Override
  public IExpression divide(IExpression pNumber1, IExpression pNumber2) {
    IExpression result;
    if (isNumber(pNumber2)) {
      result = castToTerm(pNumber1).$times(IdealInt.ONE().$div(((IIntLit) pNumber2).value()));
      // TODO div is the euclidian division (with remainder), do we want this here?
    } else {
      result = makeUf(divUfDecl, pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public boolean isDivide(IExpression pNumber) {
    return isUf(divUfDecl, pNumber);
  }

  @Override
  public IExpression modulo(IExpression pNumber1, IExpression pNumber2) {
    return makeUf(modUfDecl, pNumber1, pNumber2);
  }

  @Override
  public boolean isModulo(IExpression pNumber) {
    return isUf(modUfDecl, pNumber);
  }

  @Override
  public IExpression multiply(IExpression pNumber1, IExpression pNumber2) {
    IExpression result;
    if (isNumber(pNumber1) || isNumber(pNumber2)) {
      result = castToTerm(pNumber1).$times(castToTerm(pNumber2));
    } else {
      result = makeUf(multUfDecl, pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public boolean isMultiply(IExpression pNumber) {
    return pNumber instanceof ITimes || isUf(multUfDecl, pNumber);
  }

  @Override
  public IFormula equal(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$eq$eq$eq(castToTerm(pNumber2));
  }

  @Override
  public boolean isEqual(IExpression pNumber) {
    // Princess does not support Equal.
    // Formulas are converted from "a==b" to "a+(-b)==0".
    // So this will never return true for the original terms, but only for a intermediate result.
    return pNumber instanceof IIntFormula && ((IIntFormula)pNumber).rel() == IIntRelation.EqZero();
  }

  @Override
  public IFormula greaterThan(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$greater(castToTerm(pNumber2));
  }

  @Override
  public boolean isGreaterThan(IExpression pNumber) {
    // Princess does not support >.
    // Formulas are converted from "a>b" to "a+(-b)+(-1)>=0".
    return false;
  }

  @Override
  public IFormula greaterOrEquals(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$greater$eq(castToTerm(pNumber2));
  }

  @Override
  public boolean isGreaterOrEquals(IExpression pNumber) {
    // Princess does not support >=.
    // Formulas are converted from "a>=b" to "a+(-b)>=0".
    // So this will never return true for the original terms, but only for a intermediate result.
    return pNumber instanceof IIntFormula && ((IIntFormula)pNumber).rel() == IIntRelation.GeqZero();
  }

  @Override
  public IFormula lessThan(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$less(castToTerm(pNumber2));
  }

  @Override
  public boolean isLessThan(IExpression pNumber) {
    // Princess does not support <.
    // Formulas are converted from "a<b" to "b+(-a)+(-1)>=0".
    return false;
  }

  @Override
  public IFormula lessOrEquals(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$less$eq(castToTerm(pNumber2));
  }

  @Override
  public boolean isLessOrEquals(IExpression pNumber) {
    // Princess does not support <=.
    // Formulas are converted from "a<=b" to "b+(-a)>=0".
    return false;
  }
}
