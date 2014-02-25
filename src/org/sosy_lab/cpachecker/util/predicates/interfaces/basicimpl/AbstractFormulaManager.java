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
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;

/**
 * Simplifies building a solver from the specific theories.
 * @param <TFormulaInfo> The solver specific type.
 */
public abstract class AbstractFormulaManager<TFormulaInfo> implements FormulaManager {

  private final AbstractBooleanFormulaManager<TFormulaInfo> booleanManager;

  private final AbstractRationalFormulaManager<TFormulaInfo> rationalManager;

  private final AbstractBitvectorFormulaManager<TFormulaInfo> bitvectorManager;

  private final AbstractFunctionFormulaManager<TFormulaInfo> functionManager;

  private final FormulaCreator<TFormulaInfo> formulaCreator;

  private final AbstractUnsafeFormulaManager<TFormulaInfo> unsafeManager;

  /**
   * Builds a solver from the given theory implementations
   * @param unsafeManager the unsafe manager
   * @param functionManager the function theory
   * @param booleanManager the boolean theory
   * @param rationalManager the rational theory
   * @param bitvectorManager the bitvector theory
   */
  protected AbstractFormulaManager(
      AbstractUnsafeFormulaManager<TFormulaInfo> unsafeManager,
      AbstractFunctionFormulaManager<TFormulaInfo> functionManager,
      AbstractBooleanFormulaManager<TFormulaInfo> booleanManager,
      AbstractRationalFormulaManager<TFormulaInfo> rationalManager,
      AbstractBitvectorFormulaManager<TFormulaInfo> bitvectorManager) {
    if (functionManager == null || booleanManager == null || unsafeManager == null) {
      throw new IllegalArgumentException("boolean, function and unsafe manager instances have to be valid!");
    }

    this.functionManager = functionManager;

    this.booleanManager = booleanManager;

    this.rationalManager = rationalManager;

    this.bitvectorManager = bitvectorManager;

    this.unsafeManager = unsafeManager;

    formulaCreator = functionManager.getFormulaCreator();
    if (booleanManager.getFormulaCreator() != formulaCreator
        || unsafeManager.getFormulaCreator() != formulaCreator
        || functionManager.getFormulaCreator() != formulaCreator
        || (rationalManager != null && rationalManager.getFormulaCreator() != formulaCreator)
        || (bitvectorManager != null && bitvectorManager.getFormulaCreator() != formulaCreator)
        ) {
      throw new IllegalArgumentException("The creator instances must match across the managers!");
    }

  }

  protected FormulaCreator<TFormulaInfo> getFormulaCreator() {
    return formulaCreator;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Formula> Class<T> getInterfaceHelper(T instance) {
    checkNotNull(instance);
    Class<?> c ;
    if (instance instanceof BooleanFormula) {
      c = BooleanFormula.class;
    } else if (instance instanceof RationalFormula) {
      c = RationalFormula.class;
    } else if (instance instanceof BitvectorFormula) {
      c = BitvectorFormula.class;
    } else {
      throw new IllegalArgumentException("Invalid instance");
    }

    return (Class<T>) c;
  }

  @Override
  public <T extends Formula> Class<T> getInterface(T pInstance) {
    return AbstractFormulaManager.getInterfaceHelper(pInstance);
  }
  @Override
  public AbstractRationalFormulaManager<TFormulaInfo> getRationalFormulaManager() {
    if (rationalManager == null) {
      throw new UnsupportedOperationException();
    }
    return rationalManager;
  }

  @Override
  public AbstractBooleanFormulaManager<TFormulaInfo> getBooleanFormulaManager() {
    return booleanManager;
  }

  @Override
  public AbstractBitvectorFormulaManager<TFormulaInfo> getBitvectorFormulaManager() {
    if (bitvectorManager == null) {
      throw new UnsupportedOperationException();
    }
    return bitvectorManager;
  }

  @Override
  public AbstractFunctionFormulaManager<TFormulaInfo> getFunctionFormulaManager() {
    return functionManager;
  }

  @Override
  public AbstractUnsafeFormulaManager<TFormulaInfo> getUnsafeFormulaManager() {

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
