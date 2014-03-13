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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.wrapping;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BitvectorFormulaManagerView;


public class BitvectorFormulaView extends FormulaView<BitvectorFormula> implements BitvectorFormula {

  private BitvectorFormulaManagerView manager;

  public BitvectorFormulaView(BitvectorFormula pWrapped, BitvectorFormulaManagerView manager) {
    super(pWrapped);
    this.manager = manager;
  }

  private BitvectorFormulaView cast(BitvectorFormula formula) {
    return (BitvectorFormulaView) formula;
  }

  private BooleanFormulaView cast(BooleanFormula formula) {
    return (BooleanFormulaView) formula;
  }

  public BitvectorFormulaView negate() {
    return cast(manager.negate(this));
  }


  public BitvectorFormulaView add(BitvectorFormulaView pNumbe2) {
    return cast(manager.add(this, pNumbe2));
  }

  public BitvectorFormulaView subtract(BitvectorFormulaView pNumbe2) {
    return cast(manager.subtract(this, pNumbe2));
  }

  public BitvectorFormulaView divide(BitvectorFormulaView pNumbe2, boolean signed) {
    return cast(manager.divide(this, pNumbe2, signed));
  }

  public BitvectorFormulaView modulo(BitvectorFormulaView pNumbe2, boolean signed) {
    return cast(manager.modulo(this, pNumbe2, signed));
  }

  public BitvectorFormulaView multiply(BitvectorFormulaView pNumbe2) {
    return cast(manager.multiply(this, pNumbe2));
  }

  public BooleanFormulaView equal(BitvectorFormulaView pNumbe2) {
    return cast(manager.equal(this, pNumbe2));
  }

  public BooleanFormulaView isGreaterThan(BitvectorFormulaView pNumbe2, boolean signed) {
    return cast(manager.greaterThan(this, pNumbe2, signed));
  }

  public BooleanFormulaView isGreaterOrEquals(BitvectorFormulaView pNumbe2, boolean signed) {
    return cast(manager.greaterOrEquals(this, pNumbe2, signed));
  }

  public BooleanFormulaView isLessThan(BitvectorFormulaView pNumbe2, boolean signed) {
    return cast(manager.lessThan(this, pNumbe2, signed));
  }

  public BooleanFormulaView isLessOrEquals(BitvectorFormulaView pNumbe2, boolean signed) {
    return cast(manager.lessOrEquals(this, pNumbe2, signed));
  }

  public BitvectorFormulaView not() {
    return cast(manager.not(this));
  }

  public BitvectorFormulaView and(BitvectorFormulaView pBits2) {
    return cast(manager.and(this, pBits2));
  }

  public BitvectorFormulaView or(BitvectorFormulaView pBits2) {
    return cast(manager.or(this, pBits2));
  }

  public BitvectorFormulaView xor(BitvectorFormulaView pBits2) {
    return cast(manager.xor(this, pBits2));
  }


  public int getLength() {
    return manager.getLength(this);
  }

  public BitvectorFormula shiftRight(BitvectorFormula pToShift, boolean signed) {
    return manager.shiftRight(this, pToShift, signed);
  }

  public BitvectorFormula shiftLeft(BitvectorFormula pToShift) {
    return manager.shiftLeft(this, pToShift);
  }

  public BitvectorFormula concat(BitvectorFormula pAppend) {
    return manager.concat(this, pAppend);
  }

  public BitvectorFormula extract(int pMsb, int pLsb) {
    return manager.extract(this, pMsb, pLsb);
  }
}
