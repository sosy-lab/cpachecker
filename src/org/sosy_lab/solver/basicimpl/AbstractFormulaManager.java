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

import org.sosy_lab.common.Appender;
import org.sosy_lab.solver.api.ArrayFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.FloatingPointFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.NumeralFormula;
import org.sosy_lab.solver.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.solver.api.NumeralFormula.RationalFormula;

/**
 * Simplifies building a solver from the specific theories.
 * @param <TFormulaInfo> The solver specific type.
 */
public abstract class AbstractFormulaManager<TFormulaInfo, TType, TEnv> implements FormulaManager {

  private final @Nullable AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv> arrayManager;

  private final AbstractBooleanFormulaManager<TFormulaInfo, TType, TEnv> booleanManager;

  private final @Nullable AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> integerManager;

  private final @Nullable AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> rationalManager;

  private final @Nullable AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> bitvectorManager;

  private final @Nullable AbstractFloatingPointFormulaManager<TFormulaInfo, TType, TEnv> floatingPointManager;

  private final AbstractFunctionFormulaManager<TFormulaInfo, ?, TType, TEnv> functionManager;

  private final AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> unsafeManager;

  private final @Nullable AbstractQuantifiedFormulaManager<TFormulaInfo, TType, TEnv> quantifiedManager;

  private final FormulaCreator<TFormulaInfo, TType, TEnv> formulaCreator;

  /**
   * Builds a solver from the given theory implementations
   * @param unsafeManager the unsafe manager
   * @param functionManager the function theory
   * @param booleanManager the boolean theory
   * @param pIntegerManager the integer theory
   * @param pRationalManager the rational theory
   * @param bitvectorManager the bitvector theory
   */
  protected AbstractFormulaManager(
      FormulaCreator<TFormulaInfo, TType, TEnv> pFormulaCreator,
      AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> unsafeManager,
      AbstractFunctionFormulaManager<TFormulaInfo, ?, TType, TEnv> functionManager,
      AbstractBooleanFormulaManager<TFormulaInfo, TType, TEnv> booleanManager,
      @Nullable AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> pIntegerManager,
      @Nullable AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> pRationalManager,
      @Nullable AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> bitvectorManager,
      @Nullable AbstractFloatingPointFormulaManager<TFormulaInfo, TType, TEnv> floatingPointManager,
      @Nullable AbstractQuantifiedFormulaManager<TFormulaInfo, TType, TEnv> quantifiedManager,
      @Nullable AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv> arrayManager) {

    if (functionManager == null || booleanManager == null || unsafeManager == null) {
      throw new IllegalArgumentException("boolean, function and unsafe manager instances have to be valid!");
    }

    this.arrayManager = arrayManager;

    this.quantifiedManager = quantifiedManager;

    this.functionManager = functionManager;

    this.booleanManager = booleanManager;

    this.integerManager = pIntegerManager;

    this.rationalManager = pRationalManager;

    this.bitvectorManager = bitvectorManager;

    this.floatingPointManager = floatingPointManager;

    this.unsafeManager = unsafeManager;

    this.formulaCreator = pFormulaCreator;

    if (booleanManager.getFormulaCreator() != formulaCreator
        || unsafeManager.getFormulaCreator() != formulaCreator
        || functionManager.getFormulaCreator() != formulaCreator
            || (integerManager != null && integerManager.getFormulaCreator() != formulaCreator)
            || (rationalManager != null && rationalManager.getFormulaCreator() != formulaCreator)
        || (bitvectorManager != null && bitvectorManager.getFormulaCreator() != formulaCreator)
        || (floatingPointManager != null && floatingPointManager.getFormulaCreator() != formulaCreator)
        ) {
      throw new IllegalArgumentException("The creator instances must match across the managers!");
    }

  }

  public final FormulaCreator<TFormulaInfo, TType, TEnv> getFormulaCreator() {
    return formulaCreator;
  }

  @Override
  public AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> getIntegerFormulaManager() {
    if (integerManager == null) {
      // TODO fallback to rationalManager?
      throw new UnsupportedOperationException();
    }
    return integerManager;
  }

  @Override
  public AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> getRationalFormulaManager() {
    if (rationalManager == null) {
      // TODO fallback to integerManager?
      throw new UnsupportedOperationException();
    }
    return rationalManager;
  }

  @Override
  public AbstractBooleanFormulaManager<TFormulaInfo, TType, TEnv> getBooleanFormulaManager() {
    return booleanManager;
  }

  @Override
  public AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> getBitvectorFormulaManager() {
    if (bitvectorManager == null) {
      throw new UnsupportedOperationException();
    }
    return bitvectorManager;
  }

  @Override
  public FloatingPointFormulaManager getFloatingPointFormulaManager() {
    if (floatingPointManager == null) {
      throw new UnsupportedOperationException();
    }
    return floatingPointManager;
  }

  @Override
  public AbstractFunctionFormulaManager<TFormulaInfo, ?, TType, TEnv> getFunctionFormulaManager() {
    return functionManager;
  }

  @Override
  public AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> getUnsafeFormulaManager() {
    return unsafeManager;
  }

  @Override
  public AbstractQuantifiedFormulaManager<TFormulaInfo, TType, TEnv> getQuantifiedFormulaManager() {
    if (quantifiedManager == null){
      throw new UnsupportedOperationException();
    }
    return quantifiedManager;
  }

  @Override
  public ArrayFormulaManager getArrayFormulaManager() {
    if (arrayManager == null) {
      throw new UnsupportedOperationException();
    }
    return arrayManager;
  }

  public abstract Appender dumpFormula(TFormulaInfo t);

  @Override
  public Appender dumpFormula(BooleanFormula t) {
    return dumpFormula(formulaCreator.extractInfo(t));
  }

  @Override
  public final <T extends Formula> FormulaType<T> getFormulaType(T formula) {
    return formulaCreator.getFormulaType(checkNotNull(formula));
  }

  // Utility methods that are handy for subclasses

  public final TEnv getEnvironment() {
    return getFormulaCreator().getEnv();
  }

  public final TFormulaInfo extractInfo(Formula f) {
    return formulaCreator.extractInfo(f);
  }
}