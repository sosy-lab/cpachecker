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
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;


public class NumeralFormulaManagerView
        extends BaseManagerView<NumeralFormula>
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
  public NumeralFormula negate(NumeralFormula pNumber) {
    return wrapInView(manager.negate(extractFromView(pNumber)));
  }

  @Override
  public NumeralFormula add(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.add(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumeralFormula subtract(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.subtract(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumeralFormula divide(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.divide(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumeralFormula modulo(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.modulo(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public NumeralFormula multiply(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.multiply(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula equal(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.equal(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterThan(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.greaterThan(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterOrEquals(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.greaterOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula lessThan(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.lessThan(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula lessOrEquals(NumeralFormula pNumber1, NumeralFormula pNumbe2) {
    return wrapInView(manager.lessOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }


  @Override
  public boolean isNegate(NumeralFormula pNumber) {
    return manager.isNegate(extractFromView(pNumber));
  }

  @Override
  public boolean isAdd(NumeralFormula pNumber) {
    return manager.isAdd(extractFromView(pNumber));
  }

  @Override
  public boolean isSubtract(NumeralFormula pNumber) {
    return manager.isSubtract(extractFromView(pNumber));
  }

  @Override
  public boolean isDivide(NumeralFormula pNumber) {
    return manager.isDivide(extractFromView(pNumber));
  }

  @Override
  public boolean isModulo(NumeralFormula pNumber) {
    return manager.isModulo(extractFromView(pNumber));
  }

  @Override
  public boolean isMultiply(NumeralFormula pNumber) {
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
  public NumeralFormula makeNumber(long pI) {
    return wrapInView(manager.makeNumber(pI));
  }

  @Override
  public NumeralFormula makeNumber(BigInteger pI) {
    return wrapInView(manager.makeNumber(pI));
  }

  @Override
  public NumeralFormula makeNumber(String pI) {
    return wrapInView(manager.makeNumber(pI));
  }

  @Override
  public NumeralFormula makeVariable(String pVar) {
    return wrapInView(manager.makeVariable(pVar));
  }

  @Override
  public FormulaType<NumeralFormula> getFormulaType() {
    return manager.getFormulaType();
  }

}
