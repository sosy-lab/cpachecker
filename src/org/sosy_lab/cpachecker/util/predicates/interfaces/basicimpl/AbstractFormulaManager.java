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
import org.sosy_lab.cpachecker.util.predicates.interfaces.*;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;

/**
 * Simplifies building a solver from the specific theories.
 * @param <TFormulaInfo> The solver specific type.
 */
public abstract class AbstractFormulaManager<TFormulaInfo, TType, TEnv> implements FormulaManager {

  private final AbstractBooleanFormulaManager<TFormulaInfo, TType, TEnv> booleanManager;

  private final AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> integerManager;

  private final AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> rationalManager;

  private final AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> bitvectorManager;

  private final AbstractFunctionFormulaManager<TFormulaInfo, TType, TEnv> functionManager;

  private final FormulaCreator<TFormulaInfo> formulaCreator;

  private final AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> unsafeManager;

  private final TEnv environment;

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
      TEnv pEnvironment,
      FormulaCreator<TFormulaInfo> pFormulaCreator,
      AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> unsafeManager,
      AbstractFunctionFormulaManager<TFormulaInfo, TType, TEnv> functionManager,
      AbstractBooleanFormulaManager<TFormulaInfo, TType, TEnv> booleanManager,
      AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, IntegerFormula, IntegerFormula> pIntegerManager,
      AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv, NumeralFormula, RationalFormula> pRationalManager,
      AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv> bitvectorManager) {
    if (functionManager == null || booleanManager == null || unsafeManager == null) {
      throw new IllegalArgumentException("boolean, function and unsafe manager instances have to be valid!");
    }

    this.functionManager = functionManager;

    this.booleanManager = booleanManager;

    this.integerManager = pIntegerManager;

    this.rationalManager = pRationalManager;

    this.bitvectorManager = bitvectorManager;

    this.unsafeManager = unsafeManager;

    this.environment = pEnvironment;

    this.formulaCreator = pFormulaCreator;

    if (booleanManager.getFormulaCreator() != formulaCreator
        || unsafeManager.getFormulaCreator() != formulaCreator
        || functionManager.getFormulaCreator() != formulaCreator
            || (integerManager != null && integerManager.getFormulaCreator() != formulaCreator)
            || (rationalManager != null && rationalManager.getFormulaCreator() != formulaCreator)
        || (bitvectorManager != null && bitvectorManager.getFormulaCreator() != formulaCreator)
        ) {
      throw new IllegalArgumentException("The creator instances must match across the managers!");
    }

  }

  protected FormulaCreator<TFormulaInfo> getFormulaCreator() {
    return formulaCreator;
  }

  public TEnv getEnvironment() {
    return environment;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Formula> Class<T> getInterfaceHelper(T instance) {
    checkNotNull(instance);
    Class<?> c ;
    if (instance instanceof BooleanFormula) {
      c = BooleanFormula.class;
    } else if (instance instanceof IntegerFormula) {
      c = IntegerFormula.class;
    } else if (instance instanceof RationalFormula) {
      c = RationalFormula.class;
    } else if (instance instanceof BitvectorFormula) {
      c = BitvectorFormula.class;
    } else {
      throw new IllegalArgumentException("Invalid instance: " + instance.getClass());
    }

    return (Class<T>) c;
  }

  @Override
  public <T extends Formula> Class<T> getInterface(T pInstance) {
    return AbstractFormulaManager.getInterfaceHelper(pInstance);
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
  public AbstractFunctionFormulaManager<TFormulaInfo, TType, TEnv> getFunctionFormulaManager() {
    return functionManager;
  }

  @Override
  public AbstractUnsafeFormulaManager<TFormulaInfo, TType, TEnv> getUnsafeFormulaManager() {

    return unsafeManager;
  }

  @SuppressWarnings("unchecked")
  protected TFormulaInfo getTerm(Formula f) {
    return ((AbstractFormula<TFormulaInfo>)f).getFormulaInfo();
  }


  public abstract Appender dumpFormula(TFormulaInfo t);

  @Override
  public Appender dumpFormula(Formula t) {
    return dumpFormula(formulaCreator.extractInfo(t));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T formula) {
    checkNotNull(formula);
    Class<T> clazz = getInterface(formula);
    FormulaType<?> t;
    if (clazz==BooleanFormula.class) {
      t = booleanManager.getFormulaType();
    } else if (clazz == IntegerFormula.class) {
      t = integerManager.getFormulaType();
    } else if (clazz == RationalFormula.class) {
      t = rationalManager.getFormulaType();
    } else if (clazz == BitvectorFormula.class) {
      int size = bitvectorManager.getLength((BitvectorFormula)formula);
      t = bitvectorManager.getFormulaType(size);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }
    return (FormulaType<T>) t;
  }

}
