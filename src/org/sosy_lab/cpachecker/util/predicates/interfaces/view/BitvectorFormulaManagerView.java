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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.BitvectorType;


public class BitvectorFormulaManagerView extends BaseManagerView implements BitvectorFormulaManager {

  private final BitvectorFormulaManager manager;
  private final BooleanFormulaManager bmgr;

  public BitvectorFormulaManagerView(FormulaWrappingHandler pWrappingHandler,
      BitvectorFormulaManager pManager, BooleanFormulaManager pBmgr) {
    super(pWrappingHandler);
    this.manager = pManager;
    bmgr = pBmgr;
  }

  public BooleanFormula notEqual(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return bmgr.not(equal(pNumber1, pNumber2));
  }
  public BitvectorFormula makeBitvector(FormulaType<BitvectorFormula> pType, long pI) {
    BitvectorType t = (BitvectorType)pType;
    return makeBitvector(t.getSize(), pI);
  }

  public BitvectorFormula makeBitvector(FormulaType<BitvectorFormula> pType, BigInteger pI) {
    BitvectorType t = (BitvectorType)pType;
    return makeBitvector(t.getSize(), pI);
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.add(pNumber1, pNumbe2);
  }
  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.subtract(pNumber1, pNumbe2);
  }
  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.divide(pNumber1, pNumbe2, signed);
  }
  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.modulo(pNumber1, pNumbe2, signed);
  }
  @Override
  public BooleanFormula modularCongruence(BitvectorFormula pNumber1, BitvectorFormula pNumber2, long pModulo) {
    return manager.modularCongruence(pNumber1, pNumber2, pModulo);
  }
  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.multiply(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.equal(pNumber1, pNumbe2);
  }
  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.greaterThan(pNumber1, pNumbe2, signed);
  }
  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.greaterOrEquals(pNumber1, pNumbe2, signed);
  }
  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.lessThan(pNumber1, pNumbe2, signed);
  }
  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.lessOrEquals(pNumber1, pNumbe2, signed);
  }


  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return manager.not(pBits);
  }
  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return manager.and(pBits1, pBits2);
  }
  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return manager.or(pBits1, pBits2);
  }
  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return manager.xor(pBits1, pBits2);
  }


  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    return manager.makeBitvector(pLength, pI);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    return manager.makeBitvector(pLength, pI);
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return manager.makeVariable(pLength, pVar);
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return manager.getLength(pNumber);
  }

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by toShift.
   */
  @Override
  public BitvectorFormula shiftRight(BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    return manager.shiftRight(pNumber, pToShift, signed);
  }
  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    return manager.shiftLeft(pNumber, pToShift);
  }
  @Override
  public BitvectorFormula concat(BitvectorFormula pNumber, BitvectorFormula pAppend) {
    return manager.concat(pNumber, pAppend);
  }
  @Override
  public BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb) {
    return manager.extract(pNumber, pMsb, pLsb);
  }
  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    return manager.extend(pNumber, pExtensionBits, pSigned);
  }
}
