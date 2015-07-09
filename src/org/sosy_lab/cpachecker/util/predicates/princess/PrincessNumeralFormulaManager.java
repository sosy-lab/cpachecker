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

import static org.sosy_lab.cpachecker.util.predicates.princess.PrincessUtil.*;

import org.sosy_lab.cpachecker.util.predicates.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractNumeralFormulaManager;

import ap.basetypes.IdealInt;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IIntLit;
import ap.parser.ITerm;


abstract class PrincessNumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<IExpression, TermType, PrincessEnvironment, ParamFormulaType, ResultFormulaType> {

  PrincessNumeralFormulaManager(
          PrincessFormulaCreator pCreator,
          PrincessFunctionFormulaManager pFunctionManager) {
    super(pCreator, pFunctionManager);
  }

  @Override
  public ITerm negate(IExpression pNumber) {
    return castToTerm(pNumber).unary_$minus();
  }

  @Override
  public ITerm add(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$plus(castToTerm(pNumber2));
  }

  @Override
  public ITerm subtract(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$minus(castToTerm(pNumber2));
  }

  @Override
  public IExpression divide(IExpression pNumber1, IExpression pNumber2) {
    IExpression result;
    if (isNumber(pNumber2)) {
      result = castToTerm(pNumber1).$times(IdealInt.ONE().$div(((IIntLit) pNumber2).value()));
      // TODO div is the euclidian division (with remainder), do we want this here?
    } else {
      result = super.divide(pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public IExpression multiply(IExpression pNumber1, IExpression pNumber2) {
    IExpression result;
    if (isNumber(pNumber1) || isNumber(pNumber2)) {
      result = castToTerm(pNumber1).$times(castToTerm(pNumber2));
    } else {
      result = super.multiply(pNumber1, pNumber2);
    }

    return result;
  }

  @Override
  public IFormula equal(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$eq$eq$eq(castToTerm(pNumber2));
  }

  @Override
  public IFormula greaterThan(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$greater(castToTerm(pNumber2));
  }

  @Override
  public IFormula greaterOrEquals(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$greater$eq(castToTerm(pNumber2));
  }

  @Override
  public IFormula lessThan(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$less(castToTerm(pNumber2));
  }

  @Override
  public IFormula lessOrEquals(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$less$eq(castToTerm(pNumber2));
  }
}
