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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;

/**
 * Simplifies building a solver from the specific theories.
 * @param <TFormulaInfo> The solver specific type.
 */
public abstract class AbstractFormulaManager<TFormulaInfo, TType, TEnv> implements FormulaManager {

  private final AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv> arrayManager;

  private final AbstractBooleanFormulaManager<TFormulaInfo, TType, TEnv> booleanManager;

  private final AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> integerManager;

  private final AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> rationalManager;

  private final AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> bitvectorManager;

  private final AbstractFloatingPointFormulaManager<TFormulaInfo, TType, TEnv> floatingPointManager;

  private final AbstractFunctionFormulaManager<TFormulaInfo, ?, TType, TEnv> functionManager;

  private final AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> unsafeManager;

  private final AbstractQuantifiedFormulaManager<TFormulaInfo, TType, TEnv> quantifiedManager;

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
      AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> pIntegerManager,
      AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> pRationalManager,
      AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> bitvectorManager,
      AbstractFloatingPointFormulaManager<TFormulaInfo, TType, TEnv> floatingPointManager,
      AbstractQuantifiedFormulaManager<TFormulaInfo, TType, TEnv> quantifiedManager,
      AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv> arrayManager) {

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
    return quantifiedManager;
  }

  @Override
  public ArrayFormulaManager getArrayFormulaManager() {
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

  @Override
  public SmtAstMatcher getSmtAstMatcher() {
    throw new UnsupportedOperationException("There is not yet an implementation for formula-ast matching for this solver!");
  }
}