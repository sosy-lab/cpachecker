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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public abstract class AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements BitvectorFormulaManager {

  protected AbstractBitvectorFormulaManager(
      AbstractFormulaCreator<TFormulaInfo, TType, TEnv> pCreator) {
    super(pCreator);
  }


  @Override
  public FormulaType<BitvectorFormula> getFormulaType(int pLength) {
      return FormulaType.BitvectorType.getBitvectorType(pLength);
  }

  protected TFormulaInfo extractInfo(Formula pNumber) {
    return getFormulaCreator().extractInfo(pNumber);
  }

  protected BitvectorFormula wrap(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(BitvectorFormula.class, pTerm);
  }

  protected BooleanFormula wrapBool(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(BooleanFormula.class, pTerm);
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    TFormulaInfo param1 = extractInfo(pNumber);
    return wrap(negate(param1));
  }

  protected abstract TFormulaInfo negate(TFormulaInfo pParam1);

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't add bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);



    return wrap(add(param1, param2));
  }

  protected abstract TFormulaInfo add(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't subtract bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(subtract(param1, param2));
  }

  protected abstract TFormulaInfo subtract(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't divide bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(divide(param1, param2, signed));
  }

  protected abstract TFormulaInfo divide(TFormulaInfo pParam1, TFormulaInfo pParam2, boolean signed);


  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't modulo bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(modulo(param1, param2, signed));
  }

  protected abstract TFormulaInfo modulo(TFormulaInfo pParam1, TFormulaInfo pParam2, boolean signed);


  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't multiply bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(multiply(param1, param2));
  }

  protected abstract TFormulaInfo multiply(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't compare bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(equal(param1, param2));
  }

  protected abstract TFormulaInfo equal(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't compare bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(greaterThan(param1, param2, signed));
  }

  protected abstract TFormulaInfo greaterThan(TFormulaInfo pParam1, TFormulaInfo pParam2, boolean signed) ;


  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't compare bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(greaterOrEquals(param1, param2, signed));
  }

  protected abstract TFormulaInfo greaterOrEquals(TFormulaInfo pParam1, TFormulaInfo pParam2, boolean signed) ;

  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't compare bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(lessThan(param1, param2, signed));
  }

  protected abstract TFormulaInfo lessThan(TFormulaInfo pParam1, TFormulaInfo pParam2, boolean signed) ;


  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't compare bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(lessOrEquals(param1, param2, signed));
  }

  protected abstract TFormulaInfo lessOrEquals(TFormulaInfo pParam1, TFormulaInfo pParam2, boolean signed);

  @Override
  public boolean isNegate(BitvectorFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isNegate(param);
  }
  protected abstract boolean isNegate(TFormulaInfo pParam) ;

  @Override
  public boolean isAdd(BitvectorFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isAdd(param);
  }
  protected abstract boolean isAdd(TFormulaInfo pParam);


  @Override
  public boolean isSubtract(BitvectorFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isSubtract(param);
  }

  protected abstract boolean isSubtract(TFormulaInfo pParam);


  @Override
  public boolean isDivide(BitvectorFormula pNumber, boolean signed) {
    TFormulaInfo param = extractInfo(pNumber);
    return isDivide(param, signed);
  }
  protected abstract boolean isDivide(TFormulaInfo pParam, boolean signed) ;


  @Override
  public boolean isModulo(BitvectorFormula pNumber, boolean signed) {
    TFormulaInfo param = extractInfo(pNumber);
    return isModulo(param, signed);
  }

  protected abstract boolean isModulo(TFormulaInfo pParam, boolean signed) ;


  @Override
  public boolean isMultiply(BitvectorFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isMultiply(param);
  }
  protected abstract boolean isMultiply(TFormulaInfo pParam) ;

  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isEqual(param);
  }
  protected abstract boolean isEqual(TFormulaInfo pParam) ;

  @Override
  public boolean isGreaterThan(BooleanFormula pNumber, boolean signed) {
    TFormulaInfo param = extractInfo(pNumber);
    return isGreaterThan(param, signed);
  }
  protected abstract boolean isGreaterThan(TFormulaInfo pParam, boolean signed) ;

  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber, boolean signed) {
    TFormulaInfo param = extractInfo(pNumber);
    return isGreaterOrEquals(param, signed);
  }
  protected abstract boolean isGreaterOrEquals(TFormulaInfo pParam, boolean signed) ;

  @Override
  public boolean isLessThan(BooleanFormula pNumber, boolean signed) {
    TFormulaInfo param = extractInfo(pNumber);
    return isLessThan(param, signed);
  }
  protected abstract boolean isLessThan(TFormulaInfo pParam, boolean signed) ;

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber, boolean signed) {
    TFormulaInfo param = extractInfo(pNumber);
    return isLessOrEquals(param, signed);
  }
  protected abstract boolean isLessOrEquals(TFormulaInfo pParam, boolean signed) ;


  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    TFormulaInfo param1 = extractInfo(pBits);
    return wrap(not(param1));
  }

  protected abstract TFormulaInfo not(TFormulaInfo pParam1) ;


  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2);
    TFormulaInfo param1 = extractInfo(pBits1);
    TFormulaInfo param2 = extractInfo(pBits2);

    return wrap(and(param1, param2));
  }

  protected abstract TFormulaInfo and(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2);
    TFormulaInfo param1 = extractInfo(pBits1);
    TFormulaInfo param2 = extractInfo(pBits2);

    return wrap(or(param1, param2));
  }

  protected abstract TFormulaInfo or(TFormulaInfo pParam1, TFormulaInfo pParam2);
  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2);
    TFormulaInfo param1 = extractInfo(pBits1);
    TFormulaInfo param2 = extractInfo(pBits2);

    return wrap(xor(param1, param2));
  }

  protected abstract TFormulaInfo xor(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public boolean isNot(BitvectorFormula pBits) {
    TFormulaInfo param = extractInfo(pBits);
    return isNot(param);
  }

  protected abstract boolean isNot(TFormulaInfo pParam) ;


  @Override
  public boolean isAnd(BitvectorFormula pBits) {
    TFormulaInfo param = extractInfo(pBits);
    return isAnd(param);
  }
  protected abstract boolean isAnd(TFormulaInfo pParam) ;
  @Override
  public boolean isOr(BitvectorFormula pBits) {
    TFormulaInfo param = extractInfo(pBits);
    return isOr(param);
  }
  protected abstract boolean isOr(TFormulaInfo pParam) ;
  @Override
  public boolean isXor(BitvectorFormula pBits) {
    TFormulaInfo param = extractInfo(pBits);
    return isXor(param);
  }
  protected abstract boolean isXor(TFormulaInfo pParam) ;



  @Override
  public BitvectorFormula makeBitvector(int pLength, long i) {
    return wrap(makeBitvectorImpl(pLength, i));
  }
  protected abstract TFormulaInfo makeBitvectorImpl(int pLength, long pI) ;

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger i) {
    return wrap(makeBitvectorImpl(pLength, i));
  }
  protected abstract TFormulaInfo makeBitvectorImpl(int pLength, BigInteger pI) ;

  @Override
  public BitvectorFormula makeBitvector(int pLength, String i) {
    return wrap(makeBitvectorImpl(pLength, i));
  }
  protected abstract TFormulaInfo makeBitvectorImpl(int pLength, String pI) ;


  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return wrap(makeVariableImpl(pLength, pVar));
  }
  protected abstract TFormulaInfo makeVariableImpl(int pLength, String pVar);

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by toShift.
   */
  @Override
  public BitvectorFormula shiftRight(BitvectorFormula pNumber, BitvectorFormula toShift, boolean signed) {
    TFormulaInfo param1 = extractInfo(pNumber);
    TFormulaInfo param2 = extractInfo(toShift);

    return wrap(shiftRight(param1, param2, signed));
  }

  protected abstract TFormulaInfo shiftRight(TFormulaInfo pNumber, TFormulaInfo toShift, boolean signed);

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula toShift) {
    TFormulaInfo param1 = extractInfo(pNumber);
    TFormulaInfo param2 = extractInfo(toShift);

    return wrap(shiftLeft(param1, param2));
  }

  protected abstract TFormulaInfo shiftLeft(TFormulaInfo pExtract, TFormulaInfo pExtract2);


  @Override
  public final BitvectorFormula concat(BitvectorFormula pNumber, BitvectorFormula pAppend) {
    TFormulaInfo param1 = extractInfo(pNumber);
    TFormulaInfo param2 = extractInfo(pAppend);

    return wrap(concat(param1, param2));
  }

  protected abstract TFormulaInfo concat(TFormulaInfo number, TFormulaInfo pAppend);


  @Override
  public final BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb) {
    TFormulaInfo param = extractInfo(pNumber);

    return wrap(extract(param, pMsb, pLsb));
  }
  protected abstract TFormulaInfo extract(TFormulaInfo pNumber, int pMsb, int pLsb) ;


  @Override
  public final BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    TFormulaInfo param = extractInfo(pNumber);

    return wrap(extend(param, pExtensionBits, pSigned));
  }
  protected abstract TFormulaInfo extend(TFormulaInfo pNumber, int pExtensionBits, boolean pSigned) ;


  @Override
  public int getLength(BitvectorFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return getLength(param);
  }

  protected abstract int getLength(TFormulaInfo pParam) ;


}
