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

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumericFormula;


public class NumeralFormulaManagerView
        extends BaseManagerView<NumericFormula>
        implements NumeralFormulaManager {

  private NumeralFormulaManager manager;

  public NumeralFormulaManagerView(NumeralFormulaManager pManager) {
    this.manager = pManager;
  }

  private BooleanFormula wrapInView(BooleanFormula pFormula) {
    return getViewManager().getBooleanFormulaManager().wrapInView(pFormula);
  }

  private BooleanFormula extractFromView(BooleanFormula pCast) {
    return getViewManager().getBooleanFormulaManager().extractFromView(pCast);
  }

  @Override
  public NumericFormula negate(NumericFormula pNumber) {
    return wrapInView(manager.negate(extractFromView(pNumber)));
  }

  @Override
  public NumericFormula add(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.add(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumericFormula subtract(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.subtract(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumericFormula divide(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.divide(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumericFormula modulo(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.modulo(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumericFormula multiply(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.multiply(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula equal(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.equal(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterThan(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.greaterThan(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterOrEquals(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.greaterOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula lessThan(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.lessThan(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula lessOrEquals(NumericFormula pNumber1, NumericFormula pNumbe2) {
    return wrapInView(manager.lessOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }


  @Override
  public boolean isNegate(NumericFormula pNumber) {
    return manager.isNegate(extractFromView(pNumber));
  }

  @Override
  public boolean isAdd(NumericFormula pNumber) {
    return manager.isAdd(extractFromView(pNumber));
  }

  @Override
  public boolean isSubtract(NumericFormula pNumber) {
    return manager.isSubtract(extractFromView(pNumber));
  }

  @Override
  public boolean isDivide(NumericFormula pNumber) {
    return manager.isDivide(extractFromView(pNumber));
  }

  @Override
  public boolean isModulo(NumericFormula pNumber) {
    return manager.isModulo(extractFromView(pNumber));
  }

  @Override
  public boolean isMultiply(NumericFormula pNumber) {
    return manager.isMultiply(extractFromView(pNumber));
  }

  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    return manager.isEqual(extractFromView(pNumber));
  }



  @Override
  public boolean isGreaterThan(BooleanFormula pNumber) {
    return manager.isGreaterThan(extractFromView(pNumber));
  }

  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber) {
    return manager.isGreaterOrEquals(extractFromView(pNumber));
  }

  @Override
  public boolean isLessThan(BooleanFormula pNumber) {
    return manager.isLessThan(extractFromView(pNumber));
  }

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber) {
    return manager.isLessOrEquals(extractFromView(pNumber));
  }

  @Override
  public NumericFormula makeNumber(long pI) {
    return wrapInView(manager.makeNumber(pI));
  }

  @Override
  public NumericFormula makeNumber(BigInteger pI) {
    return wrapInView(manager.makeNumber(pI));
  }

  @Override
  public NumericFormula makeNumber(String pI) {
    return wrapInView(manager.makeNumber(pI));
  }

  @Override
  public NumericFormula makeVariable(String pVar) {
    return wrapInView(manager.makeVariable(pVar));
  }

  @Override
  public FormulaType<NumericFormula> getFormulaType() {
    return manager.getFormulaType();
  }

}
