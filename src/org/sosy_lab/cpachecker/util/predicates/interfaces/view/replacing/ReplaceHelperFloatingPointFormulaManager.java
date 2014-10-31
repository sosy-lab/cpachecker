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

import java.math.BigDecimal;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.FloatingPointType;

import com.google.common.base.Function;


class ReplaceHelperFloatingPointFormulaManager implements FloatingPointFormulaManager {

  private final Function<FormulaType<?>, FormulaType<?>> unwrapTypes;
  private final ReplacingFormulaManager replaceManager;
  private final FloatingPointFormulaManager rawFloatingPointManager;

  public ReplaceHelperFloatingPointFormulaManager(
      ReplacingFormulaManager pReplacingFormulaManager,
      FloatingPointFormulaManager pRawFloatingPointManager,
    Function<FormulaType<?>, FormulaType<?>> pUnwrapTypes) {
    replaceManager = pReplacingFormulaManager;
    rawFloatingPointManager = pRawFloatingPointManager;
    unwrapTypes = pUnwrapTypes;
  }

  @Override
  public <T extends Formula> T castTo(FloatingPointFormula pNumber, FormulaType<T> pTargetType) {
    return replaceManager.wrap(pTargetType,
        rawFloatingPointManager.castTo(pNumber, unwrapTypes.apply(pTargetType)));
  }

  @Override
  public FloatingPointFormula castFrom(Formula pNumber, boolean pSigned, FloatingPointType pTargetType) {
    return rawFloatingPointManager.castFrom(
        replaceManager.unwrap(pNumber), pSigned, pTargetType);
  }

  @Override
  public FloatingPointFormula negate(FloatingPointFormula pNumber) {
    return rawFloatingPointManager.negate(pNumber);
  }

  @Override
  public FloatingPointFormula add(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.add(pNumber1, pNumber2);
  }

  @Override
  public FloatingPointFormula subtract(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.subtract(pNumber1, pNumber2);
  }
  @Override
  public FloatingPointFormula divide(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.divide(pNumber1, pNumber2);
  }
  @Override
  public FloatingPointFormula multiply(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.multiply(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula assignment(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.assignment(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula equalWithFPSemantics(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.equalWithFPSemantics(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula greaterThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.greaterThan(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula greaterOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.greaterOrEquals(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula lessThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.lessThan(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula lessOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return rawFloatingPointManager.lessOrEquals(pNumber1, pNumber2);
  }


  @Override
  public FloatingPointFormula makeNumber(double pN, FormulaType.FloatingPointType type) {
    return rawFloatingPointManager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FormulaType.FloatingPointType type) {
    return rawFloatingPointManager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FormulaType.FloatingPointType type) {
    return rawFloatingPointManager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType pType) {
    return rawFloatingPointManager.makeVariable(pVar, pType);
  }
}