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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractNumeralFormulaManager;

import com.google.common.collect.ImmutableList;


abstract class Mathsat5NumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<Long, Long, Long, ParamFormulaType, ResultFormulaType> {

  private final Mathsat5FunctionType<? extends NumeralFormula> multUfDecl;
  protected final Mathsat5FunctionType<? extends NumeralFormula> divUfDecl;
  private final Mathsat5FunctionType<? extends NumeralFormula> modUfDecl;
  private final Mathsat5FunctionFormulaManager functionManager;

  private final long mathsatEnv;

  public Mathsat5NumeralFormulaManager(
          Mathsat5FormulaCreator pCreator,
          Mathsat5FunctionFormulaManager functionManager,
          final Class<ResultFormulaType> pFormulaType) {
    super(pCreator, pFormulaType);

    this.mathsatEnv = pCreator.getEnv();
    this.functionManager = functionManager;
    FormulaType<? extends NumeralFormula> formulaType = getFormulaType();
    multUfDecl = functionManager.createFunction(formulaType + "_" + MultUfName, formulaType, formulaType, formulaType);
    divUfDecl = functionManager.createFunction(formulaType + "_" + DivUfName, formulaType, formulaType, formulaType);
    modUfDecl = functionManager.createFunction(formulaType + "_" + ModUfName, formulaType, formulaType, formulaType);

  }

  protected long makeUf(FunctionFormulaType decl, long t1, long t2) {
    return functionManager.createUninterpretedFunctionCallImpl(decl, ImmutableList.of(t1, t2));
  }

  private boolean isUf(Mathsat5FunctionType funcDecl, Long pBits) {
    return functionManager.isUninterpretedFunctionCall(funcDecl, pBits);
  }

  @Override
  public Long makeNumberImpl(long pI) {
    return msat_make_number(mathsatEnv, Long.toString(pI));
  }

  @Override
  public Long makeNumberImpl(BigInteger pI) {
    return msat_make_number(mathsatEnv, pI.toString());
  }

  @Override
  public Long makeNumberImpl(String pI) {
    return msat_make_number(mathsatEnv, pI);
  }

  abstract protected long getNumeralType();

  @Override
  public Long makeVariableImpl(String var) {
    return getFormulaCreator().makeVariable(getNumeralType(), var);
  }

  @Override
  public Long negate(Long pNumber) {
    return msat_make_times(mathsatEnv, pNumber, msat_make_number(mathsatEnv, "-1"));
  }

  @Override
  public Long add(Long pNumber1, Long pNumber2) {
    return msat_make_plus(mathsatEnv, pNumber1, pNumber2);
  }

  @Override
  public Long subtract(Long pNumber1, Long pNumber2) {
    return msat_make_plus(mathsatEnv, pNumber1, negate(pNumber2));
  }

  @Override
  public Long modulo(Long pNumber1, Long pNumber2) {
    return makeUf(modUfDecl, pNumber1, pNumber2);
  }

  @Override
  public Long multiply(Long pNumber1, Long pNumber2) {
    long t1 = pNumber1;
    long t2 = pNumber2;

    long result;
    if (msat_term_is_number(mathsatEnv, t1)) {
      result = msat_make_times(mathsatEnv, t1, t2);
    } else if (msat_term_is_number(mathsatEnv, t2)) {
      result = msat_make_times(mathsatEnv, t2, t1);
    } else {
      result = makeUf(multUfDecl, t1, t2);
    }

    return result;
  }

  @Override
  public Long equal(Long pNumber1, Long pNumber2) {
    return msat_make_equal(mathsatEnv, pNumber1, pNumber2);
  }

  @Override
  public Long greaterThan(Long pNumber1, Long pNumber2) {
    return makeNot(lessOrEquals(pNumber1, pNumber2));
  }

  @Override
  public Long greaterOrEquals(Long pNumber1, Long pNumber2) {
    return lessOrEquals(pNumber2, pNumber1);
  }

  private long makeNot(long n) {
    return msat_make_not(mathsatEnv, n);
  }

  @Override
  public Long lessThan(Long pNumber1, Long pNumber2) {
    return makeNot(lessOrEquals(pNumber2, pNumber1));
  }

  @Override
  public Long lessOrEquals(Long pNumber1, Long pNumber2) {
    return msat_make_leq(mathsatEnv, pNumber1, pNumber2);
  }

  @Override
  public boolean isNegate(Long pNumber) {
    boolean isMult = isMultiply(pNumber);
    if (!isMult) {
      return false;
    }
    long arg = msat_term_get_arg(pNumber, 1);
    if (msat_term_is_number(mathsatEnv, arg)) {
      String n = msat_term_repr(arg);
      if (n.startsWith("(")) {
        n = n.substring(1, n.length() - 1);
      }
      if (n.equals("-1")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAdd(Long pNumber) {
    return msat_term_is_plus(mathsatEnv, pNumber);
  }

  @Override
  public boolean isSubtract(Long pNumber) {
    boolean isPlus = isAdd(pNumber);
    if (!isPlus) {
      return false;
    }

    long arg = msat_term_get_arg(pNumber, 1);
    return isNegate(arg);
  }

  @Override
  public boolean isDivide(Long pNumber) {
    return isMultiply(pNumber) || isUf(divUfDecl, pNumber);
  }

  @Override
  public boolean isModulo(Long pNumber) {
    return isUf(modUfDecl, pNumber);
  }

  @Override
  public boolean isMultiply(Long pNumber) {
    return msat_term_is_times(mathsatEnv, pNumber) || isUf(multUfDecl, pNumber);
  }

  @Override
  public boolean isEqual(Long pNumber) {
    return msat_term_is_equal(mathsatEnv, pNumber);
  }

  private boolean isBoolNot(long n) {
    return msat_term_is_not(mathsatEnv, n);
  }

  @Override
  public boolean isGreaterThan(Long pNumber) {
    boolean isNot = isBoolNot(pNumber);
    if (!isNot) {
      return false;
    }

    long arg = msat_term_get_arg(pNumber, 0);
    return isLessOrEquals(arg);
  }

  @Override
  public boolean isGreaterOrEquals(Long pNumber) {
    return isLessOrEquals(pNumber);
  }

  @Override
  public boolean isLessThan(Long pNumber) {
    boolean isNot = isBoolNot(pNumber);
    if (!isNot) {
      return false;
    }

    long arg = msat_term_get_arg(pNumber, 0);
    return isLessOrEquals(arg);
  }

  @Override
  public boolean isLessOrEquals(Long pNumber) {
    return msat_term_is_leq(mathsatEnv, pNumber);
  }

}
