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
package org.sosy_lab.solver.smtInterpol;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;


class SmtInterpolIntegerFormulaManager extends SmtInterpolNumeralFormulaManager<IntegerFormula, IntegerFormula> {

  SmtInterpolIntegerFormulaManager(
          SmtInterpolFormulaCreator pCreator,
          SmtInterpolFunctionFormulaManager pFunctionManager,
          boolean useNonLinearArithmetic) {
    super(pCreator, pFunctionManager, useNonLinearArithmetic);
  }

  @Override
  public FormulaType<IntegerFormula> getFormulaType() {
    return FormulaType.IntegerType;
  }

  @Override
  protected Term makeNumberImpl(long i) {
    return getFormulaCreator().getEnv().numeral(BigInteger.valueOf(i));
  }

  @Override
  protected Term makeNumberImpl(BigInteger pI) {
    return getFormulaCreator().getEnv().numeral(pI);
  }

  @Override
  protected Term makeNumberImpl(String pI) {
    return getFormulaCreator().getEnv().numeral(pI);
  }

  @Override
  protected Term makeNumberImpl(double pNumber) {
    return makeNumberImpl((long)pNumber);
  }

  @Override
  protected Term makeNumberImpl(BigDecimal pNumber) {
    return decimalAsInteger(pNumber);
  }

  @Override
  protected Term makeVariableImpl(String varName) {
    Sort t = getFormulaCreator().getIntegerType();
    return getFormulaCreator().makeVariable(t, varName);
  }

  @Override
  public Term linearDivide(Term pNumber1, Term pNumber2) {
    assert isNumeral(pNumber2);
    Sort intSort = pNumber1.getTheory().getNumericSort();
    assert intSort.equals(pNumber1.getSort()) && intSort.equals(pNumber2.getSort());
    return getFormulaCreator().getEnv().term("div", pNumber1, pNumber2);
  }

  @Override
  protected Term linearModulo(Term pNumber1, Term pNumber2) {
    assert isNumeral(pNumber2);
    Sort intSort = pNumber1.getTheory().getNumericSort();
    assert intSort.equals(pNumber1.getSort()) && intSort.equals(pNumber2.getSort());
    return getFormulaCreator().getEnv().term("mod", pNumber1, pNumber2);
  }
}
