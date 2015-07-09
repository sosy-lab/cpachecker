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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractNumeralFormulaManager;

import com.google.common.primitives.Longs;

abstract class Z3NumeralFormulaManager
        <ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
        extends AbstractNumeralFormulaManager<Long, Long, Long, ParamFormulaType, ResultFormulaType> {

  protected final long z3context;

  public Z3NumeralFormulaManager(
          Z3FormulaCreator pCreator,
          Z3FunctionFormulaManager functionManager) {
    super(pCreator, functionManager);

    this.z3context = pCreator.getEnv();
  }

  abstract protected long getNumeralType();

  @Override
  protected Long makeNumberImpl(long i) {
    long sort = getNumeralType();
    return mk_int64(z3context, i, sort);
  }

  @Override
  protected Long makeNumberImpl(BigInteger pI) {
    return makeNumberImpl(pI.toString());
  }

  @Override
  protected Long makeNumberImpl(String pI) {
    long sort = getNumeralType();
    return mk_numeral(z3context, pI, sort);
  }

  @Override
  protected Long makeVariableImpl(String varName) {
    long type = getNumeralType();
    return getFormulaCreator().makeVariable(type, varName);
  }

  @Override
  public Long negate(Long pNumber) {
    long sort = get_sort(z3context, pNumber);
    long minusOne = mk_int(z3context, -1, sort);
    return mk_mul(z3context, minusOne, pNumber);
  }

  @Override
  public Long add(Long pNumber1, Long pNumber2) {
    return mk_add(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long sumImpl(List<Long> operands) {
    return mk_add(z3context, operands.size(), Longs.toArray(operands));
  }

  @Override
  public Long subtract(Long pNumber1, Long pNumber2) {
    return mk_sub(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long multiply(Long pNumber1, Long pNumber2) {
    long result;
    if (is_numeral_ast(z3context, pNumber1) || is_numeral_ast(z3context, pNumber2)) {
      result = mk_mul(z3context, pNumber1, pNumber2);
    } else {
      result = super.multiply(pNumber1, pNumber2);
    }
    return result;
  }

  @Override
  public Long divide(Long pNumber1, Long pNumber2) {
    return mk_div(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long modulo(Long pNumber1, Long pNumber2) {
    return mk_mod(z3context, pNumber1, pNumber2);
  }

  @Override
  protected Long modularCongruence(Long pNumber1, Long pNumber2, long pModulo) {
    return mk_true(z3context);
  }

  @Override
  public Long equal(Long pNumber1, Long pNumber2) {
    return mk_eq(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long greaterThan(Long pNumber1, Long pNumber2) {
    return mk_gt(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long greaterOrEquals(Long pNumber1, Long pNumber2) {
    return mk_ge(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long lessThan(Long pNumber1, Long pNumber2) {
    return mk_lt(z3context, pNumber1, pNumber2);
  }

  @Override
  public Long lessOrEquals(Long pNumber1, Long pNumber2) {
    return mk_le(z3context, pNumber1, pNumber2);
  }
}
