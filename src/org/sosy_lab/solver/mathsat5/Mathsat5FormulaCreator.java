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
package org.sosy_lab.solver.mathsat5;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.solver.mathsat5.Mathsat5NativeApi.*;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.solver.api.FormulaType.FloatingPointType;
import org.sosy_lab.solver.basicimpl.FormulaCreator;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


class Mathsat5FormulaCreator extends FormulaCreator<Long, Long, Long> {

  private final Table<Long, Long, Long> allocatedArraySorts = HashBasedTable.create();

  public Mathsat5FormulaCreator(final Long msatEnv) {
    super(msatEnv,
        msat_get_bool_type(msatEnv),
        msat_get_integer_type(msatEnv),
        msat_get_rational_type(msatEnv));
  }

  @Override
  public Long makeVariable(Long type, String varName) {
    long funcDecl = msat_declare_function(getEnv(), varName, type);
    return msat_make_constant(getEnv(), funcDecl);
  }

  @Override
  public Long extractInfo(Formula pT) {
    return Mathsat5FormulaManager.getMsatTerm(pT);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    long env = getEnv();
    if (pFormula instanceof BitvectorFormula) {
      long type = msat_term_get_type(extractInfo(pFormula));
      checkArgument(msat_is_bv_type(env, type),
          "BitvectorFormula with actual type " + msat_type_repr(type) + ": " + pFormula);
      return (FormulaType<T>) FormulaType.getBitvectorTypeWithSize(
          msat_get_bv_type_size(env, type));

    } else if (pFormula instanceof FloatingPointFormula) {
      long type = msat_term_get_type(extractInfo(pFormula));
      checkArgument(msat_is_fp_type(env, type),
          "FloatingPointFormula with actual type " + msat_type_repr(type) + ": " + pFormula);
      return (FormulaType<T>)FormulaType.getFloatingPointType(
          msat_get_fp_type_exp_width(env, type),
          msat_get_fp_type_mant_width(env, type));
    } else if (pFormula instanceof ArrayFormula<?,?>) {
      FormulaType<T> arrayIndexType = getArrayFormulaIndexType(
          (ArrayFormula<T, T>) pFormula);
      FormulaType<T> arrayElementType = getArrayFormulaElementType(
          (ArrayFormula<T, T>) pFormula);
      return (FormulaType<T>) FormulaType.getArrayType(arrayIndexType,
          arrayElementType);
    }
    return super.getFormulaType(pFormula);
  }

  @Override
  public FormulaType<?> getFormulaType(Long pFormula) {
    long env = getEnv();
    long type = msat_term_get_type(pFormula);
    if (msat_is_bool_type(env, type)) {
      return FormulaType.BooleanType;
    } else if (msat_is_integer_type(env, type)) {
      return FormulaType.IntegerType;
    } else if (msat_is_rational_type(env, type)) {
      return FormulaType.RationalType;
    } else if (msat_is_bv_type(env, type)) {
      return FormulaType.getBitvectorTypeWithSize(msat_get_bv_type_size(env, type));
    } else if (msat_is_fp_type(env, type)) {
      return FormulaType.getFloatingPointType(
          msat_get_fp_type_exp_width(env, type),
          msat_get_fp_type_mant_width(env, type));
    }
    throw new IllegalArgumentException("Unknown formula type " + msat_type_repr(type) + " for term " + msat_term_repr(pFormula));
  }

  @Override
  public Long getBitvectorType(int pBitwidth) {
    return msat_get_bv_type(getEnv(), pBitwidth);
  }

  @Override
  public Long getFloatingPointType(FloatingPointType pType) {
    return msat_get_fp_type(getEnv(), pType.getExponentSize(), pType.getMantissaSize());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(FormulaType<T> pType, Long pTerm) {
    if (pType.isBooleanType()) {
      return (T)new Mathsat5BooleanFormula(pTerm);
    } else if (pType.isIntegerType()) {
      return (T)new Mathsat5IntegerFormula(pTerm);
    } else if (pType.isRationalType()) {
      return (T)new Mathsat5RationalFormula(pTerm);
    } else if (pType.isArrayType()) {
      ArrayFormulaType<?, ?> arrFt = (ArrayFormulaType<?, ?>) pType;
      return (T)new Mathsat5ArrayFormula(pTerm,
          arrFt.getIndexType(), arrFt.getElementType());
    } else if (pType.isBitvectorType()) {
      return (T)new Mathsat5BitvectorFormula(pTerm);
    } else if (pType.isFloatingPointType()) {
      return (T)new Mathsat5FloatingPointFormula(pTerm);
    }
    throw new IllegalArgumentException("Cannot create formulas of type " + pType + " in MathSAT");
  }

  @Override
  public BooleanFormula encapsulateBoolean(Long pTerm) {
    return new Mathsat5BooleanFormula(pTerm);
  }

  @Override
  public BitvectorFormula encapsulateBitvector(Long pTerm) {
    return new Mathsat5BitvectorFormula(pTerm);
  }

  @Override
  protected FloatingPointFormula encapsulateFloatingPoint(Long pTerm) {
    return new Mathsat5FloatingPointFormula(pTerm);
  }

  @Override
  protected <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE>
      encapsulateArray(Long pTerm, FormulaType<TI> pIndexType,
          FormulaType<TE> pElementType) {
    return new Mathsat5ArrayFormula<>(pTerm, pIndexType, pElementType);
  }

  @Override
  protected <TI extends Formula, TE extends Formula>
  FormulaType<TE> getArrayFormulaElementType(ArrayFormula<TI, TE> pArray) {
    return ((Mathsat5ArrayFormula<TI, TE>) pArray).getElementType();
  }

  @Override
  protected <TI extends Formula, TE extends Formula>
  FormulaType<TI> getArrayFormulaIndexType(ArrayFormula<TI, TE> pArray) {
    return ((Mathsat5ArrayFormula<TI, TE>) pArray).getIndexType();
  }

  @Override
  public Long getArrayType(Long pIndexType, Long pElementType) {
//    Long allocatedArraySort = allocatedArraySorts.get(pIndexType, pElementType);
//    if (allocatedArraySort == null) {
//      allocatedArraySort = msat_make_array_const(getEnv(), pIndexType, pElementType);
//      allocatedArraySorts.put(pIndexType, pElementType, allocatedArraySort);
//    }
//    return allocatedArraySort;
    return msat_get_array_type(getEnv(), pIndexType, pElementType);
    //throw new IllegalArgumentException("MathSAT5.getArrayType(): Implement me!");
  }
}
