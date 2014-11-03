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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.FloatingPointType;


public class FloatingPointFormulaManagerView
        extends BaseManagerView<FloatingPointFormula, FloatingPointFormula>
        implements FloatingPointFormulaManager {

  private final FloatingPointFormulaManager manager;

  public FloatingPointFormulaManagerView(FloatingPointFormulaManager pManager) {
    this.manager = pManager;
  }

  private BooleanFormula wrapInView(BooleanFormula pFormula) {
    return getViewManager().getBooleanFormulaManager().wrapInView(pFormula);
  }

  @Override
  public <T extends Formula> T castTo(FloatingPointFormula pNumber, FormulaType<T> pTargetType) {
    return getViewManager().wrapInView(manager.castTo(extractFromView(pNumber), pTargetType));
  }

  @Override
  public FloatingPointFormula castFrom(Formula pNumber, boolean pSigned, FloatingPointType pTargetType) {
    return wrapInView(manager.castFrom(
        getViewManager().extractFromView(pNumber), pSigned, pTargetType));
  }

  @Override
  public FloatingPointFormula negate(FloatingPointFormula pNumber) {
    return wrapInView(manager.negate(extractFromView(pNumber)));
  }

  @Override
  public FloatingPointFormula add(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.add(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }

  @Override
  public FloatingPointFormula subtract(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.subtract(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public FloatingPointFormula divide(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.divide(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public FloatingPointFormula multiply(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.multiply(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula assignment(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return wrapInView(manager.assignment(extractFromView(pNumber1), extractFromView(pNumber2)));
  }
  @Override
  public BooleanFormula equalWithFPSemantics(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.equalWithFPSemantics(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.greaterThan(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.greaterOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula lessThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.lessThan(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula lessOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return wrapInView(manager.lessOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }

  @Override
  public BooleanFormula isNaN(FloatingPointFormula pNumber) {
    return wrapInView(manager.isNaN(pNumber));
  }
  @Override
  public BooleanFormula isInfinity(FloatingPointFormula pNumber) {
    return wrapInView(manager.isInfinity(pNumber));
  }
  @Override
  public BooleanFormula isZero(FloatingPointFormula pNumber) {
    return wrapInView(manager.isZero(pNumber));
  }
  @Override
  public BooleanFormula isSubnormal(FloatingPointFormula pNumber) {
    return wrapInView(manager.isSubnormal(pNumber));
  }

  @Override
  public FloatingPointFormula makeNumber(double pN, FormulaType.FloatingPointType type) {
    return wrapInView(manager.makeNumber(pN, type));
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FormulaType.FloatingPointType type) {
    return wrapInView(manager.makeNumber(pN, type));
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FormulaType.FloatingPointType type) {
    return wrapInView(manager.makeNumber(pN, type));
  }

  @Override
  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType pType) {
    return wrapInView(manager.makeVariable(pVar, pType));
  }

  public FloatingPointFormula makeVariable(String pVar, int idx, FormulaType.FloatingPointType pType) {
    return wrapInView(manager.makeVariable(FormulaManagerView.makeName(pVar, idx), pType));
  }

  @Override
  public FloatingPointFormula makePlusInfinity(FloatingPointType pType) {
    return wrapInView(manager.makePlusInfinity(pType));
  }

  @Override
  public FloatingPointFormula makeMinusInfinity(FloatingPointType pType) {
    return wrapInView(manager.makeMinusInfinity(pType));
  }

  @Override
  public FloatingPointFormula makeNaN(FloatingPointType pType) {
    return wrapInView(manager.makeNaN(pType));
  }
}