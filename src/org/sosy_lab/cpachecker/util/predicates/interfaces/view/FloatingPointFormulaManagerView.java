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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import java.math.BigDecimal;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.FloatingPointType;


public class FloatingPointFormulaManagerView
        extends BaseManagerView
        implements FloatingPointFormulaManager {

  private final FloatingPointFormulaManager manager;

  public FloatingPointFormulaManagerView(FormulaWrappingHandler pWrappingHandler,
      FloatingPointFormulaManager pManager) {
    super(pWrappingHandler);
    this.manager = pManager;
  }

  @Override
  public <T extends Formula> T castTo(FloatingPointFormula pNumber, FormulaType<T> pTargetType) {
    // no wrapping/unwrapping, done inside ReplaceFloatingPointWithNumeralAndFunctionTheory
    return manager.castTo(pNumber, pTargetType);
  }

  @Override
  public FloatingPointFormula castFrom(Formula pNumber, boolean pSigned, FloatingPointType pTargetType) {
    // no wrapping/unwrapping, done inside ReplaceFloatingPointWithNumeralAndFunctionTheory
    return manager.castFrom(pNumber, pSigned, pTargetType);
  }

  @Override
  public FloatingPointFormula negate(FloatingPointFormula pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public FloatingPointFormula add(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.add(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula subtract(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.subtract(pNumber1, pNumbe2);
  }
  @Override
  public FloatingPointFormula divide(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.divide(pNumber1, pNumbe2);
  }
  @Override
  public FloatingPointFormula multiply(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.multiply(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula assignment(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return manager.assignment(pNumber1, pNumber2);
  }
  @Override
  public BooleanFormula equalWithFPSemantics(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.equalWithFPSemantics(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula greaterThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.greaterThan(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula greaterOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.greaterOrEquals(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula lessThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.lessThan(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula lessOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.lessOrEquals(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula isNaN(FloatingPointFormula pNumber) {
    return manager.isNaN(pNumber);
  }
  @Override
  public BooleanFormula isInfinity(FloatingPointFormula pNumber) {
    return manager.isInfinity(pNumber);
  }
  @Override
  public BooleanFormula isZero(FloatingPointFormula pNumber) {
    return manager.isZero(pNumber);
  }
  @Override
  public BooleanFormula isSubnormal(FloatingPointFormula pNumber) {
    return manager.isSubnormal(pNumber);
  }

  @Override
  public FloatingPointFormula makeNumber(double pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(Rational pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType pType) {
    return manager.makeVariable(pVar, pType);
  }

  public FloatingPointFormula makeVariable(String pVar, int idx, FormulaType.FloatingPointType pType) {
    return manager.makeVariable(FormulaManagerView.makeName(pVar, idx), pType);
  }

  @Override
  public FloatingPointFormula makePlusInfinity(FloatingPointType pType) {
    return manager.makePlusInfinity(pType);
  }

  @Override
  public FloatingPointFormula makeMinusInfinity(FloatingPointType pType) {
    return manager.makeMinusInfinity(pType);
  }

  @Override
  public FloatingPointFormula makeNaN(FloatingPointType pType) {
    return manager.makeNaN(pType);
  }
}