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
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.BitvectorType;


public class BitvectorFormulaManagerView extends BaseManagerView<BitvectorFormula> implements BitvectorFormulaManager {

  private BitvectorFormulaManager manager;
  private BooleanFormulaManagerView bmgr;
  public BitvectorFormulaManagerView(BitvectorFormulaManager pManager) {
    this.manager = pManager;
  }

  public BooleanFormula notEqual(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    bmgr = getViewManager().getBooleanFormulaManager();
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

  public BitvectorFormula makeBitvector(FormulaType<BitvectorFormula> pType, String pI) {
    BitvectorType t = (BitvectorType)pType;
    return makeBitvector(t.getSize(), pI);
  }

  public BitvectorFormula makeVariable(int pLength, String pVar, int idx) {
    return makeVariable(pLength, FormulaManagerView.makeName(pVar, idx));
  }

  public BitvectorFormula makeVariable(FormulaType<BitvectorFormula> pType, String name) {
    BitvectorType t = (BitvectorType)pType;
    return makeVariable(t.getSize(), name);
  }
  public BitvectorFormula makeVariable(FormulaType<BitvectorFormula> pType, String name, int idx) {
    BitvectorType t = (BitvectorType)pType;
    return makeVariable(t.getSize(), name, idx);
  }

  private BooleanFormula extractFromView(BooleanFormula pCast) {
    return getViewManager().getBooleanFormulaManager().extractFromView(pCast);
  }


  private BooleanFormula wrapInView(BooleanFormula pFormula) {
    return getViewManager().getBooleanFormulaManager().wrapInView(pFormula);
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return wrapInView(manager.negate(extractFromView(pNumber)));
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return wrapInView(manager.add(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return wrapInView(manager.subtract(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return wrapInView(manager.divide(extractFromView(pNumber1), extractFromView(pNumbe2), signed));
  }
  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return wrapInView(manager.modulo(extractFromView(pNumber1), extractFromView(pNumbe2), signed));
  }
  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return wrapInView(manager.multiply(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return wrapInView(manager.equal(extractFromView(pNumber1), extractFromView(pNumbe2)));
  }
  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return wrapInView(manager.greaterThan(extractFromView(pNumber1), extractFromView(pNumbe2), signed));
  }
  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return wrapInView(manager.greaterOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2), signed));
  }
  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return wrapInView(manager.lessThan(extractFromView(pNumber1), extractFromView(pNumbe2), signed));
  }
  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return wrapInView(manager.lessOrEquals(extractFromView(pNumber1), extractFromView(pNumbe2), signed));
  }

  @Override
  public boolean isNegate(BitvectorFormula pNumber) {
    return manager.isNegate(extractFromView(pNumber));
  }

  @Override
  public boolean isAdd(BitvectorFormula pNumber) {
    return manager.isAdd(extractFromView(pNumber));
  }

  @Override
  public boolean isSubtract(BitvectorFormula pNumber) {
    return manager.isSubtract(extractFromView(pNumber));
  }

  @Override
  public boolean isDivide(BitvectorFormula pNumber, boolean signed) {
    return manager.isDivide(extractFromView(pNumber), signed);
  }

  @Override
  public boolean isModulo(BitvectorFormula pNumber, boolean signed) {
    return manager.isModulo(extractFromView(pNumber), signed);
  }

  @Override
  public boolean isMultiply(BitvectorFormula pNumber) {
    return manager.isMultiply(extractFromView(pNumber));
  }

  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    return manager.isEqual(extractFromView(pNumber));
  }

  @Override
  public boolean isGreaterThan(BooleanFormula pNumber, boolean signed) {
    return manager.isGreaterThan(extractFromView(pNumber), signed);
  }


  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber, boolean signed) {
    return manager.isGreaterOrEquals(extractFromView(pNumber), signed);
  }

  @Override
  public boolean isLessThan(BooleanFormula pNumber, boolean signed) {
    return manager.isLessThan(extractFromView(pNumber), signed);
  }

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber, boolean signed) {
    return manager.isLessOrEquals(extractFromView(pNumber), signed);
  }


  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return wrapInView(manager.not(extractFromView(pBits)));
  }
  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return wrapInView(manager.and(extractFromView(pBits1), extractFromView(pBits2)));
  }
  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return wrapInView(manager.or(extractFromView(pBits1), extractFromView(pBits2)));
  }
  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return wrapInView(manager.xor(extractFromView(pBits1), extractFromView(pBits2)));
  }

  @Override
  public boolean isNot(BitvectorFormula pBits) {
    return manager.isNot(extractFromView(pBits));
  }

  @Override
  public boolean isAnd(BitvectorFormula pBits) {
    return manager.isAnd(extractFromView(pBits));
  }

  @Override
  public boolean isOr(BitvectorFormula pBits) {
    return manager.isOr(extractFromView(pBits));
  }

  @Override
  public boolean isXor(BitvectorFormula pBits) {
    return manager.isXor(extractFromView(pBits));
  }


  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    return wrapInView(manager.makeBitvector(pLength, pI));
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    return wrapInView(manager.makeBitvector(pLength, pI));
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, String pI) {
    return wrapInView(manager.makeBitvector(pLength, pI));
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return wrapInView(manager.makeVariable(pLength, pVar));
  }

  @Override
  public FormulaType<BitvectorFormula> getFormulaType(int pLength) {
    return manager.getFormulaType(pLength);
  }
  @Override
  public int getLength(BitvectorFormula pNumber) {
    return manager.getLength(extractFromView(pNumber));
  }

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by toShift.
   */
  @Override
  public BitvectorFormula shiftRight(BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    return wrapInView(manager.shiftRight(extractFromView(pNumber), extractFromView(pToShift), signed));
  }
  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    return wrapInView(manager.shiftLeft(extractFromView(pNumber), extractFromView(pToShift)));
  }
  @Override
  public BitvectorFormula concat(BitvectorFormula pNumber, BitvectorFormula pAppend) {
    return wrapInView(manager.concat(extractFromView(pNumber), extractFromView(pAppend)));
  }
  @Override
  public BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb) {
    return wrapInView(manager.extract(extractFromView(pNumber), pMsb, pLsb));
  }
  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    return wrapInView(manager.extend(extractFromView(pNumber), pExtensionBits, pSigned));
  }
}
