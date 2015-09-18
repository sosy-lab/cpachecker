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
package org.sosy_lab.solver.basicimpl;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FloatingPointFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.NumeralFormula.RationalFormula;


/**
 * This is a helper class with several methods that are commonly used
 * throughout the basicimpl package and may have solver-specific implementations.
 * Each solver package is expected to provide an instance of this class,
 * with the appropriate methods overwritten.
 * Depending on the solver, some or all non-final methods in this class
 * may need to be overwritten.
 * @param <TFormulaInfo> the solver specific type for formulas.
 * @param <TType> the solver specific type for formula types.
 * @param <TEnv> the solver specific type for the environment/context.
 */
public abstract class FormulaCreator<TFormulaInfo, TType, TEnv> {

  private final TType boolType;
  private final @Nullable TType integerType;
  private final @Nullable TType rationalType;
  private final TEnv environment;

  protected FormulaCreator(
      TEnv env,
      TType boolType,
      @Nullable TType pIntegerType,
      @Nullable TType pRationalType
      ) {
    this.environment = env;
    this.boolType = boolType;
    this.integerType = pIntegerType;
    this.rationalType = pRationalType;
  }

  public final TEnv getEnv() {
    return environment;
  }

  public final TType getBoolType() {
    return boolType;
  }

  public final TType getIntegerType() {
    if (integerType == null) {
      throw new UnsupportedOperationException("Integer theory is not supported by this solver.");
    }
    return integerType;
  }

  public final TType getRationalType() {
    if (rationalType == null) {
      throw new UnsupportedOperationException("Rational theory is not supported by this solver.");
    }
    return rationalType;
  }

  public abstract TType getBitvectorType(int bitwidth);

  public abstract TType getFloatingPointType(FormulaType.FloatingPointType type);

  public abstract TType getArrayType(TType indexType, TType elementType);

  public abstract TFormulaInfo makeVariable(TType type, String varName);

  public BooleanFormula encapsulateBoolean(TFormulaInfo pTerm) {
    return new BooleanFormulaImpl<>(pTerm);
  }

  protected BitvectorFormula encapsulateBitvector(TFormulaInfo pTerm) {
    return new BitvectorFormulaImpl<>(pTerm);
  }

  protected FloatingPointFormula encapsulateFloatingPoint(TFormulaInfo pTerm) {
    return new FloatingPointFormulaImpl<>(pTerm);
  }

  protected <TI extends Formula, TE extends Formula>
  ArrayFormula<TI, TE>
  encapsulateArray(TFormulaInfo pTerm, FormulaType<TI> pIndexType, FormulaType<TE> pElementType) {
    return new ArrayFormulaImpl<>(pTerm, pIndexType, pElementType);
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T encapsulate(FormulaType<T> pType, TFormulaInfo pTerm) {
    if (pType.isBooleanType()) {
      return (T)new BooleanFormulaImpl<>(pTerm);
    } else if (pType.isIntegerType()) {
      return (T)new IntegerFormulaImpl<>(pTerm);
    } else if (pType.isRationalType()) {
      return (T)new RationalFormulaImpl<>(pTerm);
    } else if (pType.isBitvectorType()) {
      return (T)new BitvectorFormulaImpl<>(pTerm);
    } else if (pType.isFloatingPointType()) {
      return (T)new FloatingPointFormulaImpl<>(pTerm);
    } else if (pType.isArrayType()) {
      ArrayFormulaType<?, ?> arrayType = (ArrayFormulaType<?, ?>) pType;
      return (T) encapsulateArray(pTerm, arrayType.getIndexType(), arrayType.getElementType());
    }
    throw new IllegalArgumentException("Cannot create formulas of type " + pType + " in the Solver!");
  }

  @SuppressWarnings("unchecked")
  protected TFormulaInfo extractInfo(Formula pT) {
    if (pT instanceof AbstractFormula) {
      return ((AbstractFormula<TFormulaInfo>)pT).getFormulaInfo();
    }
    throw new IllegalArgumentException("Cannot get the formula info of type " + pT.getClass().getSimpleName() + " in the Solver!");
  }

  @SuppressWarnings("unchecked")
  protected <TI extends Formula, TE extends Formula>
  FormulaType<TE> getArrayFormulaElementType(ArrayFormula<TI, TE> pArray) {
    return ((ArrayFormulaImpl<TI, TE, TFormulaInfo>)pArray).getElementType();
  }

  @SuppressWarnings("unchecked")
  protected <TI extends Formula, TE extends Formula>
  FormulaType<TI> getArrayFormulaIndexType(ArrayFormula<TI, TE> pArray) {
    return ((ArrayFormulaImpl<TI, TE, TFormulaInfo>)pArray).getIndexType();
  }

  /**
   * Returns the type of the given Formula.
   */
  @SuppressWarnings("unchecked")
  protected <T extends Formula> FormulaType<T> getFormulaType(T formula) {
    checkNotNull(formula);
    FormulaType<?> t;
    if (formula instanceof BooleanFormula) {
      t = FormulaType.BooleanType;
    } else if (formula instanceof IntegerFormula) {
      t = FormulaType.IntegerType;
    } else if (formula instanceof RationalFormula) {
      t = FormulaType.RationalType;
    } else if (formula instanceof ArrayFormula) {
      throw new UnsupportedOperationException("SMT solvers with support for arrays needs to overwrite FormulaCreator.getFormulaType()");
    } else if (formula instanceof BitvectorFormula) {
      throw new UnsupportedOperationException("SMT solvers with support for bitvectors needs to overwrite FormulaCreator.getFormulaType()");
    } else {
      throw new IllegalArgumentException("Formula with unexpected type " + formula.getClass());
    }
    return (FormulaType<T>) t;
  }

  public abstract FormulaType<?> getFormulaType(TFormulaInfo formula);

}