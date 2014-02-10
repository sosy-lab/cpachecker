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
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractRationalFormulaManager;

import com.google.common.collect.ImmutableList;


class Mathsat5RationalFormulaManager extends AbstractRationalFormulaManager<Long> {

  private final Mathsat5FunctionType<RationalFormula> multUfDecl;
  private final Mathsat5FunctionType<RationalFormula> divUfDecl;
  private final Mathsat5FunctionType<RationalFormula> modUfDecl;
  private final Mathsat5FunctionFormulaManager functionManager;

  private final Mathsat5FormulaCreator creator;
  private final long mathsatEnv;

  private final boolean useIntegers;

  public Mathsat5RationalFormulaManager(
      Mathsat5FormulaCreator pCreator,
      Mathsat5FunctionFormulaManager functionManager,
      boolean pUseIntegers) {
    super(pCreator);

    this.creator = pCreator;
    this.mathsatEnv = creator.getEnv();
    this.functionManager = functionManager;
    this.useIntegers = pUseIntegers;
    FormulaType<RationalFormula> formulaType = getFormulaType();
    multUfDecl = functionManager.createFunction(MultUfName, formulaType, formulaType, formulaType);
    divUfDecl = functionManager.createFunction(DivUfName, formulaType, formulaType, formulaType);
    modUfDecl = functionManager.createFunction(ModUfName, formulaType, formulaType, formulaType);

  }

  private long makeUf(FunctionFormulaType<RationalFormula> decl, long t1, long t2) {
    return functionManager.createUninterpretedFunctionCallImpl(decl, ImmutableList.of(t1, t2));
  }

  private boolean isUf(Mathsat5FunctionType<RationalFormula> funcDecl, Long pBits) {
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


  @Override
  public Long makeVariableImpl(String var) {
    long numberType = creator.getNumberType();
    return creator.makeVariable(numberType, var);
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
  public Long divide(Long pNumber1, Long pNumber2) {
    long t1 = pNumber1;
    long t2 = pNumber2;

    long result;
    if (msat_term_is_number(mathsatEnv, t2)) {
      // invert t2 and multiply with it
      String n = msat_term_repr(t2);
      if (n.startsWith("(")) {
        n = n.substring(1, n.length() - 1);
      }
      String[] frac = n.split("/");
      if (frac.length == 1) {
        if (useIntegers) {
          // cannot multiply with term 1/n because the result will have type rat instead of int
          return makeUf(divUfDecl, t1, t2);
        }
        n = "1/" + n;
      } else {
        assert (frac.length == 2);
        n = frac[1] + "/" + frac[0];
      }
      t2 = msat_make_number(mathsatEnv, n);
      result = msat_make_times(mathsatEnv, t2, t1);
    } else {
      result = makeUf(divUfDecl, t1, t2);
    }

    return result;
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
