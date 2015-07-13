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
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public abstract class AbstractBitvectorFormulaManager<TFormulaInfo, TType, TEnv>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements BitvectorFormulaManager {

  protected AbstractBitvectorFormulaManager(
      FormulaCreator<TFormulaInfo, TType, TEnv> pCreator) {
    super(pCreator);
  }

  private BitvectorFormula wrap(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulateBitvector(pTerm);
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber, boolean signed) {
    TFormulaInfo param1 = extractInfo(pNumber);
    return wrap(negate(param1));
  }

  protected abstract TFormulaInfo negate(TFormulaInfo pParam1);

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2)
        : "Can't add bitvectors with different sizes (" + getLength(pNumber1) + " and " + getLength(pNumber2) + ")";

    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);



    return wrap(add(param1, param2));
  }

  protected abstract TFormulaInfo add(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
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
  public BooleanFormula modularCongruence(BitvectorFormula pNumber1, BitvectorFormula pNumber2, long pModulo) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(modularCongruence(param1, param2, pModulo));
  }

  protected abstract TFormulaInfo modularCongruence(TFormulaInfo pNumber1, TFormulaInfo pNumber2, long pModulo);


  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
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
  public final BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb, boolean pSigned) {
    TFormulaInfo param = extractInfo(pNumber);

    return wrap(extract(param, pMsb, pLsb, pSigned));
  }
  protected abstract TFormulaInfo extract(TFormulaInfo pNumber, int pMsb, int pLsb, boolean pSigned) ;


  @Override
  public final BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    TFormulaInfo param = extractInfo(pNumber);

    return wrap(extend(param, pExtensionBits, pSigned));
  }
  protected abstract TFormulaInfo extend(TFormulaInfo pNumber, int pExtensionBits, boolean pSigned) ;

  @Override
  public int getLength(BitvectorFormula pNumber) {
    FormulaType<BitvectorFormula> type = getFormulaCreator().getFormulaType(pNumber);
    return ((FormulaType.BitvectorType)type).getSize();
  }
}
