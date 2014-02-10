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
package org.sosy_lab.cpachecker.util.invariants.templates.manager;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.invariants.templates.NonTemplate;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNumber;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.BitvectorType;


public class TemplateBitvectorFormulaManager implements BitvectorFormulaManager {

  private TemplateNumericBaseFormulaManager baseManager;
  private TemplateFormulaManager manager;

  public TemplateBitvectorFormulaManager(TemplateFormulaManager manager) {
    baseManager = new TemplateNumericBaseFormulaManager();
    this.manager = manager;
  }


  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return (BitvectorFormula) baseManager.negate(pNumber);
  }

  @Override
  public boolean isNegate(BitvectorFormula pNumber) {
    return baseManager.isNegate(pNumber);
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return (BitvectorFormula) baseManager.add(pNumber1, pNumber1);
  }

  @Override
  public boolean isAdd(BitvectorFormula pNumber) {
    return baseManager.isAdd(pNumber);
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return (BitvectorFormula) baseManager.subtract(pNumber1, pNumber1);
  }

  @Override
  public boolean isSubtract(BitvectorFormula pNumber) {
    return baseManager.isSubtract(pNumber);
  }

  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return (BitvectorFormula) baseManager.divide(pNumber1, pNumber2);
  }

  @Override
  public boolean isDivide(BitvectorFormula pNumber, boolean signed) {
    return baseManager.isDivide(pNumber);
  }

  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return (BitvectorFormula) baseManager.modulo(pNumber1, pNumber1);
  }

  @Override
  public boolean isModulo(BitvectorFormula pNumber, boolean signed) {
    return baseManager.isModulo(pNumber);
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return (BitvectorFormula) baseManager.multiply(pNumber1, pNumber2);
  }

  @Override
  public boolean isMultiply(BitvectorFormula pNumber) {
    return baseManager.isMultiply(pNumber);
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return baseManager.equal(pNumber1, pNumber2);
  }

  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    return baseManager.isEqual(pNumber);
  }

  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return baseManager.greaterThan(pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterThan(BooleanFormula pNumber, boolean signed) {
    return baseManager.isGreaterThan(pNumber);
  }

  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return baseManager.greaterOrEquals(pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber, boolean signed) {
    return baseManager.isGreaterOrEquals(pNumber);
  }

  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return baseManager.lessThan(pNumber1, pNumber2);
  }

  @Override
  public boolean isLessThan(BooleanFormula pNumber, boolean signed) {
    return baseManager.isLessThan(pNumber);
  }

  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return baseManager.lessOrEquals(pNumber1, pNumber2);
  }

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber, boolean signed) {
    return baseManager.isLessOrEquals(pNumber);
  }


  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return new NonTemplate(getType(pBits));
  }


  private BitvectorType getType(BitvectorFormula pBits) {
    return (BitvectorType)((TemplateFormula)pBits).getFormulaType();
  }


  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return new NonTemplate(getType(pBits1));
  }


  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return new NonTemplate(getType(pBits1));
  }


  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return new NonTemplate(getType(pBits1));
  }


  @Override
  public boolean isNot(BitvectorFormula pBits) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean isAnd(BitvectorFormula pBits) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean isOr(BitvectorFormula pBits) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean isXor(BitvectorFormula pBits) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    FormulaType<?> type = BitvectorType.getBitvectorType(pLength);
    TemplateNumber N = new TemplateNumber(type, (int) pI);
    TemplateTerm T = new TemplateTerm(type);
    T.setCoefficient(N);
    return T;
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    return makeBitvector(pLength, pI.longValue());
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, String pI) {
    return makeBitvector(pLength, Long.parseLong(pI));
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    FormulaType<?> type = BitvectorType.getBitvectorType(pLength);
    return manager.makeVariable(type, pVar, null);
  }


  @Override
  public FormulaType<BitvectorFormula> getFormulaType(int pLength) {
    return BitvectorType.getBitvectorType(pLength);
  }


  @Override
  public int getLength(BitvectorFormula pNumber) {
    return getType(pNumber).getSize();
  }


  @Override
  public BitvectorFormula shiftRight(BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    return new NonTemplate(getType(pNumber));
  }


  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    return new NonTemplate(getType(pNumber));
  }


  @Override
  public BitvectorFormula concat(BitvectorFormula pNumber, BitvectorFormula pAppend) {
    BitvectorType t1 = getType(pNumber);
    BitvectorType t2 = getType(pAppend);
    return new NonTemplate(t1.withSize(t1.getSize() + t2.getSize()));
  }


  @Override
  public BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb) {
    return new NonTemplate(getType(pNumber).withSize(pMsb - pLsb));
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    BitvectorType t = getType(pNumber);
    return new NonTemplate(t.withSize(t.getSize() + pExtensionBits));
  }
}
