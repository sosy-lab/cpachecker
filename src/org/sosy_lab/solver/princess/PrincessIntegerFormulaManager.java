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

import static org.sosy_lab.solver.princess.PrincessUtil.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.solver.TermType;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;

import ap.basetypes.IdealInt;
import ap.parser.IBoolLit;
import ap.parser.IExpression;
import ap.parser.IIntLit;
import ap.parser.ITerm;
import ap.theories.BitShiftMultiplication;


class PrincessIntegerFormulaManager extends org.sosy_lab.solver.princess.PrincessNumeralFormulaManager<IntegerFormula, IntegerFormula> {

  PrincessIntegerFormulaManager(
          PrincessFormulaCreator pCreator,
          PrincessFunctionFormulaManager pFunctionManager,
          boolean useNonLinearArithmetic) {
    super(pCreator, pFunctionManager, useNonLinearArithmetic);
  }

  @Override
  public FormulaType<IntegerFormula> getFormulaType() {
    return FormulaType.IntegerType;
  }

  @Override
  protected ITerm makeNumberImpl(long i) {
    return new IIntLit(IdealInt.apply(i));
  }

  @Override
  protected ITerm makeNumberImpl(BigInteger pI) {
    return new IIntLit(IdealInt.apply(pI.toString()));
  }

  @Override
  protected ITerm makeNumberImpl(String pI) {
    return new IIntLit(IdealInt.apply(pI));
  }

  @Override
  protected ITerm makeNumberImpl(double pNumber) {
    return makeNumberImpl((long)pNumber);
  }

  @Override
  protected IExpression makeNumberImpl(BigDecimal pNumber) {
    return decimalAsInteger(pNumber);
  }

  @Override
  protected IExpression makeVariableImpl(String varName) {
    TermType t = getFormulaCreator().getIntegerType();
    return getFormulaCreator().makeVariable(t, varName);
  }

  @Override
  protected IExpression modularCongruence(IExpression pNumber1, IExpression pNumber2, long pModulo) {
    // ((_ divisible n) x)   <==>   (= x (* n (div x n)))
    if (pModulo > 0) {
      ITerm n = makeNumberImpl(pModulo);
      ITerm x = subtract(pNumber1, pNumber2);
      return x.$eq$eq$eq(n.$times(BitShiftMultiplication.eDiv(x, n)));
    }
    return new IBoolLit(true);
  }

  @Override
  public IExpression linearDivide(IExpression pNumber1, IExpression pNumber2) {
    return nonLinearDivide(pNumber1, pNumber2);
  }

  @Override
  public IExpression nonLinearDivide(IExpression pNumber1, IExpression pNumber2) {
    return BitShiftMultiplication.eDiv(castToTerm(pNumber1), castToTerm(pNumber2));
  }

  @Override
  public IExpression linearModulo(IExpression pNumber1, IExpression pNumber2) {
    return nonLinearModulo(pNumber1, pNumber2);
  }

  @Override
  public IExpression nonLinearModulo(IExpression pNumber1, IExpression pNumber2) {
    return BitShiftMultiplication.eMod(castToTerm(pNumber1), castToTerm(pNumber2));
  }

  @Override
  public IExpression linearMultiply(IExpression pNumber1, IExpression pNumber2) {
    return castToTerm(pNumber1).$times(castToTerm(pNumber2));
  }

  @Override
  protected boolean isNumeral(IExpression val) {
    return isNumber(val);
  }
}
