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

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.FloatingPointType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFloatingPointFormulaManager;

class Mathsat5FloatingPointFormulaManager
        extends AbstractFloatingPointFormulaManager<Long, Long, Long> {

  private final long mathsatEnv;

  private final long roundingMode;

  public Mathsat5FloatingPointFormulaManager(Mathsat5FormulaCreator pCreator) {
    super(pCreator);

    mathsatEnv = pCreator.getEnv();
    roundingMode = msat_make_fp_roundingmode_nearest_even(mathsatEnv);
  }

  private long getMathsatType(FloatingPointType pType) {
    return msat_get_fp_type(mathsatEnv, pType.getExponentSize(), pType.getMantissaSize());
  }

  @Override
  public Long makeNumberImpl(double pN, FloatingPointType pType) {
    return makeNumberImpl(Double.toString(pN), pType);
  }

  @Override
  public Long makeNumberImpl(BigDecimal pN, FloatingPointType pType) {
    return makeNumberImpl(pN.toPlainString(), pType);
  }

  @Override
  public Long makeNumberImpl(String pN, FloatingPointType pType) {
    return msat_make_fp_rat_number(mathsatEnv, pN,
        pType.getExponentSize(), pType.getMantissaSize(), roundingMode);
  }

  @Override
  public Long makeVariableImpl(String var, FloatingPointType type) {
    return getFormulaCreator().makeVariable(getMathsatType(type), var);
  }

  @Override
  protected Long castToImpl(Long pNumber, FloatingPointType pTargetType) {
    return msat_make_fp_cast(mathsatEnv,
        pTargetType.getExponentSize(), pTargetType.getMantissaSize(),
        roundingMode, pNumber);
  }

  @Override
  public Long negate(Long pNumber) {
    return msat_make_fp_neg(mathsatEnv, pNumber);
  }

  @Override
  public Long add(Long pNumber1, Long pNumber2) {
    return msat_make_fp_plus(mathsatEnv, roundingMode, pNumber1, pNumber2);
  }

  @Override
  public Long subtract(Long pNumber1, Long pNumber2) {
    return msat_make_fp_minus(mathsatEnv, roundingMode, pNumber1, pNumber2);
  }

  @Override
  public Long multiply(Long pNumber1, Long pNumber2) {
    return msat_make_fp_times(mathsatEnv, roundingMode, pNumber1, pNumber2);
  }

  @Override
  protected Long divide(Long pNumber1, Long pNumber2) {
    return msat_make_fp_div(mathsatEnv, roundingMode, pNumber1, pNumber2);
  }

  @Override
  public Long equal(Long pNumber1, Long pNumber2) {
    return msat_make_fp_equal(mathsatEnv, pNumber1, pNumber2);
  }

  @Override
  public Long greaterThan(Long pNumber1, Long pNumber2) {
    return lessThan(pNumber2, pNumber1);
  }

  @Override
  public Long greaterOrEquals(Long pNumber1, Long pNumber2) {
    return lessOrEquals(pNumber2, pNumber1);
  }

  @Override
  public Long lessThan(Long pNumber1, Long pNumber2) {
    return msat_make_fp_lt(mathsatEnv, pNumber1, pNumber2);
  }

  @Override
  public Long lessOrEquals(Long pNumber1, Long pNumber2) {
    return msat_make_fp_leq(mathsatEnv, pNumber1, pNumber2);
  }
}
