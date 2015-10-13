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
package org.sosy_lab.solver.princess;

import static org.sosy_lab.solver.princess.PrincessUtil.castToTerm;

import org.sosy_lab.solver.TermType;
import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.basicimpl.AbstractNumeralFormulaManager;

import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.ITerm;

abstract class PrincessNumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<IExpression, TermType, PrincessEnvironment, ParamFormulaType, ResultFormulaType> {

  PrincessNumeralFormulaManager(
          PrincessFormulaCreator pCreator,
          PrincessFunctionFormulaManager pFunctionManager,
          boolean useNonLinearArithmetic) {
    super(pCreator, pFunctionManager, useNonLinearArithmetic);
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
