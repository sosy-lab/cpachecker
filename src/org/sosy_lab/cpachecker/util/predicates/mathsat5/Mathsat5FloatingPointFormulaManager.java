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

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.FloatingPointType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFloatingPointFormulaManager;

class Mathsat5FloatingPointFormulaManager
        extends AbstractFloatingPointFormulaManager<Long, Long, Long> {

  private final Mathsat5FunctionFormulaManager ffmgr;

  private final long mathsatEnv;

  private final long roundingMode;

  public Mathsat5FloatingPointFormulaManager(Mathsat5FormulaCreator pCreator,
      Mathsat5FunctionFormulaManager pFfmgr) {
    super(pCreator);

    ffmgr = pFfmgr;
    mathsatEnv = pCreator.getEnv();
    roundingMode = msat_make_fp_roundingmode_nearest_even(mathsatEnv);
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
  protected Long makeNumberImpl(String pN, FloatingPointType pType) {
    return msat_make_fp_rat_number(mathsatEnv, pN,
        pType.getExponentSize(), pType.getMantissaSize(), roundingMode);
  }

  @Override
  public Long makeVariableImpl(String var, FloatingPointType type) {
    return getFormulaCreator().makeVariable(
        getFormulaCreator().getFloatingPointType(type), var);
  }

  @Override
  protected Long makePlusInfinityImpl(FloatingPointType type) {
    return msat_make_fp_plus_inf(mathsatEnv, type.getExponentSize(), type.getMantissaSize());
  }

  @Override
  protected Long makeMinusInfinityImpl(FloatingPointType type) {
    return msat_make_fp_minus_inf(mathsatEnv, type.getExponentSize(), type.getMantissaSize());
  }

  @Override
  protected Long makeNaNImpl(FloatingPointType type) {
    return msat_make_fp_nan(mathsatEnv, type.getExponentSize(), type.getMantissaSize());
  }

  @Override
  protected Long castToImpl(Long pNumber, FormulaType<?> pTargetType) {
    if (pTargetType.isFloatingPointType()) {
      FormulaType.FloatingPointType targetType = (FormulaType.FloatingPointType)pTargetType;
      return msat_make_fp_cast(mathsatEnv,
          targetType.getExponentSize(), targetType.getMantissaSize(),
          roundingMode, pNumber);

    } else if (pTargetType.isBitvectorType()) {
      FormulaType.BitvectorType targetType = (FormulaType.BitvectorType)pTargetType;
      return msat_make_fp_to_bv(mathsatEnv, targetType.getSize(), roundingMode, pNumber);

    } else {
      return genericCast(pNumber, pTargetType);
    }
  }

  @Override
  protected Long castFromImpl(Long pNumber, boolean signed, FloatingPointType pTargetType) {
    FormulaType<?> formulaType = getFormulaCreator().getFormulaType(pNumber);

    if (formulaType.isFloatingPointType()) {
      return castToImpl(pNumber, pTargetType);

    } else if (formulaType.isBitvectorType()) {
      if (signed) {
        return msat_make_fp_from_sbv(mathsatEnv,
            pTargetType.getExponentSize(), pTargetType.getMantissaSize(),
            roundingMode, pNumber);
      } else {
        return msat_make_fp_from_ubv(mathsatEnv,
            pTargetType.getExponentSize(), pTargetType.getMantissaSize(),
            roundingMode, pNumber);
      }

    } else {
      return genericCast(pNumber, pTargetType);
    }
  }

  private Long genericCast(Long pNumber, FormulaType<?> pTargetType) {
    long argType = msat_term_get_type(pNumber);
    long castFuncDecl = ffmgr.createFunctionImpl(
        "__cast_" + argType + "_to_" + pTargetType,
        toSolverType(pTargetType), new long[]{argType});
    return ffmgr.createUIFCallImpl(castFuncDecl, new long[]{pNumber});
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
  protected Long assignment(Long pNumber1, Long pNumber2) {
    return msat_make_equal(mathsatEnv, pNumber1, pNumber2);
  }

  @Override
  public Long equalWithFPSemantics(Long pNumber1, Long pNumber2) {
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

  @Override
  protected Long isNaN(Long pParam) {
    return msat_make_fp_isnan(mathsatEnv, pParam);
  }

  @Override
  protected Long isInfinity(Long pParam) {
    return msat_make_fp_isinf(mathsatEnv, pParam);
  }

  @Override
  protected Long isZero(Long pParam) {
    return msat_make_fp_iszero(mathsatEnv, pParam);
  }

  @Override
  protected Long isSubnormal(Long pParam) {
    return msat_make_fp_issubnormal(mathsatEnv, pParam);
  }
}
