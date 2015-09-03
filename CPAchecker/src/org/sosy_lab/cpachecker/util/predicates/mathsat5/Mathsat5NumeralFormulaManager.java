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

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractNumeralFormulaManager;


abstract class Mathsat5NumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<Long, Long, Long, ParamFormulaType, ResultFormulaType> {

  private final long mathsatEnv;

  public Mathsat5NumeralFormulaManager(
          Mathsat5FormulaCreator pCreator,
          Mathsat5FunctionFormulaManager functionManager) {
    super(pCreator, functionManager);

    this.mathsatEnv = pCreator.getEnv();
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
  public Long multiply(Long pNumber1, Long pNumber2) {
    long t1 = pNumber1;
    long t2 = pNumber2;

    long result;
    if (msat_term_is_number(mathsatEnv, t1)) {
      result = msat_make_times(mathsatEnv, t1, t2);
    } else if (msat_term_is_number(mathsatEnv, t2)) {
      result = msat_make_times(mathsatEnv, t2, t1);
    } else {
      result = super.multiply(pNumber1, pNumber2);
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
}
