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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.replacing;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.util.predicates.interfaces.*;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;

import com.google.common.base.Function;


public class ReplacingFormulaManager implements FormulaManager {

  private FormulaManager rawFormulaManager;
  private BitvectorFormulaManager bitvectorTheory;
  private boolean replacedBitvectorTheory = false;
  private boolean replacedRationalTheory = false;
  private boolean replacedBooleanTheory = false;
  private FunctionFormulaManager functionTheory;
  private BooleanFormulaManager booleanTheory;
  private UnsafeFormulaManager unsafeManager;

  public ReplacingFormulaManager(
      FormulaManager rawFormulaManager,
      final boolean replaceBitvectorWithRationalAndFunctions,
      final boolean ignoreExtractConcat) {
    this.rawFormulaManager = rawFormulaManager;

    // Setup replacement environment
    Function<FormulaType<?>, FormulaType<?>>
    unwrapTypes = new
        Function<FormulaType<?>, FormulaType<?>>() {
          @Override
          public FormulaType<?> apply(FormulaType<?> pArg0) {
            if (pArg0.isBitvectorType()) {
              if (replaceBitvectorWithRationalAndFunctions) {
                return FormulaType.RationalType;
              }
            }

            return pArg0;
          }};

    functionTheory =
        new ReplaceHelperFunctionFormulaManager(
            this,
            rawFormulaManager.getFunctionFormulaManager(),
            unwrapTypes);
    booleanTheory =
        new ReplaceHelperBooleanFormulaManager(
            this,
            rawFormulaManager.getBooleanFormulaManager());
    unsafeManager =
        new ReplaceUnsafeFormulaManager(
            this,
            rawFormulaManager.getUnsafeFormulaManager(),
            unwrapTypes);

    if (replaceBitvectorWithRationalAndFunctions) {
      bitvectorTheory =
          new ReplaceBitvectorWithRationalAndFunctionTheory(
              this,
              rawFormulaManager.getRationalFormulaManager(),
              rawFormulaManager.getFunctionFormulaManager(),
              ignoreExtractConcat);
      replacedBitvectorTheory = true;
    } else {
      bitvectorTheory = rawFormulaManager.getBitvectorFormulaManager();
    }
  }

  @SuppressWarnings("unchecked")
  public <T1 extends Formula, T2 extends Formula> T1 simpleWrap(FormulaType<T1> type, T2 toWrap) {
    Formula f;
    if (type.isBitvectorType()) {
      f = new WrappingBitvectorFormula<>((FormulaType<BitvectorFormula>)type, toWrap);
    } else if (type.isIntegerType()) {
      f = new WrappingIntegerFormula<>((FormulaType<IntegerFormula>)type, toWrap);
    } else if (type.isRationalType()) {
      f = new WrappingRationalFormula<>((FormulaType<RationalFormula>)type, toWrap);
    } else if (type.isBooleanType()) {
      f = new WrappingBooleanFormula<>((FormulaType<BooleanFormula>)type, toWrap);
    } else {
      throw new IllegalArgumentException("cant wrap this type");
    }

    return (T1) f;
  }

  @SuppressWarnings("unchecked")
  public <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> type, T2 toWrap) {
    Class<T2> toWrapClazz = getInterface(toWrap);

    if (replacedBitvectorTheory && type.isBitvectorType()
        || replacedBooleanTheory && type.isBooleanType()
        || replacedRationalTheory && type.isRationalType()) {
      return simpleWrap(type, toWrap);
    } else if (toWrapClazz == type.getInterfaceType()) {
      return (T1) toWrap;
    } else {
      throw new IllegalArgumentException("invalid wrap call");
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends Formula> T unwrap(Formula f) {
    if (f instanceof WrappingFormula<?, ?>) {
      return ((WrappingFormula<T, ?>)f).getWrapped();
    } else {
      return (T) f;
    }
  }

  @Override
  public NumeralFormulaManager getIntegerFormulaManager() {
    return rawFormulaManager.getIntegerFormulaManager();
  }

  @Override
  public NumeralFormulaManager getRationalFormulaManager() {
    return rawFormulaManager.getRationalFormulaManager();
  }

  @Override
  public BooleanFormulaManager getBooleanFormulaManager() {
    return booleanTheory;
  }

  @Override
  public BitvectorFormulaManager getBitvectorFormulaManager() {
    return bitvectorTheory;
  }

  @Override
  public FunctionFormulaManager getFunctionFormulaManager() {
    return functionTheory;
  }

  @Override
  public UnsafeFormulaManager getUnsafeFormulaManager() {
    return unsafeManager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    FormulaType<?> t;
    if (pFormula instanceof WrappingFormula<?, ?>) {
      WrappingFormula<?, ?> castFormula = (WrappingFormula<?, ?>)pFormula;
      t = castFormula.getType();
    } else {
      t = rawFormulaManager.getFormulaType(pFormula);
    }

    return (FormulaType<T>) t;
  }

  @Override
  public BooleanFormula parse(String pS) throws IllegalArgumentException {
    if (replacedBooleanTheory) {
      throw new IllegalArgumentException("Can't parse a replaced theory, please change the replacement settings");
    }

    return rawFormulaManager.parse(pS);
  }

  @Override
  public <T extends Formula> Class<T> getInterface(T pInstance) {
    if (pInstance instanceof WrappingFormula<?, ?>) {
      return AbstractFormulaManager.getInterfaceHelper(pInstance);
    } else {
      return rawFormulaManager.getInterface(pInstance);
    }
  }

  @Override
  public Appender dumpFormula(Formula pT) {
    if (pT instanceof WrappingFormula<?, ?>) {
      pT = unwrap(pT);
    }

    return rawFormulaManager.dumpFormula(pT);
  }

  @Override
  public String getVersion() {
    return rawFormulaManager.getVersion();
  }

}
