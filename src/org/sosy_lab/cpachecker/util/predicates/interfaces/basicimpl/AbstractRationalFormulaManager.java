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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

/**
 * This AbstractNumericFormulaManager allows you to implement the Rational-Theory by
 * providing a NumericBaseFormulaManager<TFormulaInfo,TFormulaInfo> and implementing 3 methods.
 * @param <TFormulaInfo> the Solver specific type.
 */
public abstract class AbstractRationalFormulaManager<TFormulaInfo>
  extends AbstractBaseFormulaManager<TFormulaInfo>
  implements RationalFormulaManager {

    /**
   *
   * @param signedNumericManager
   */
  protected AbstractRationalFormulaManager(
      FormulaCreator<TFormulaInfo> pCreator) {
    super(pCreator);
  }


  protected TFormulaInfo extractInfo(Formula pNumber) {
    return getFormulaCreator().extractInfo(pNumber);
  }

  protected RationalFormula wrap(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(RationalFormula.class, pTerm);
  }

  protected BooleanFormula wrapBool(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(BooleanFormula.class, pTerm);
  }

  @Override
  public RationalFormula makeNumber(long i) {
    return wrap(makeNumberImpl(i));
  }
  protected abstract TFormulaInfo makeNumberImpl(long i);

  @Override
  public RationalFormula makeNumber(BigInteger i) {
    return wrap(makeNumberImpl(i));
  }
  protected abstract TFormulaInfo makeNumberImpl(BigInteger i);

  @Override
  public RationalFormula makeNumber(String i) {
    return wrap(makeNumberImpl(i));
  }
  protected abstract TFormulaInfo makeNumberImpl(String i);

  @Override
  public RationalFormula makeVariable(String pVar) {
    return wrap(makeVariableImpl(pVar));
  }
  protected abstract TFormulaInfo makeVariableImpl(String i);

  @Override
  public FormulaType<RationalFormula> getFormulaType() {
    return FormulaType.RationalType;
  }

  @Override
  public RationalFormula negate(RationalFormula pNumber) {
    TFormulaInfo param1 = extractInfo(pNumber);
    return wrap(negate(param1));
  }


  protected abstract TFormulaInfo negate(TFormulaInfo pParam1);



  @Override
  public RationalFormula add(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(add(param1, param2));
  }

  protected abstract TFormulaInfo add(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public RationalFormula subtract(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(subtract(param1, param2));
  }

  protected abstract TFormulaInfo subtract(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public RationalFormula divide(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(divide(param1, param2));
  }

  protected abstract TFormulaInfo divide(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public RationalFormula modulo(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(modulo(param1, param2));
  }

  protected abstract TFormulaInfo modulo(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public RationalFormula multiply(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(multiply(param1, param2));
  }

  protected abstract TFormulaInfo multiply(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BooleanFormula equal(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(equal(param1, param2));
  }

  protected abstract TFormulaInfo equal(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BooleanFormula greaterThan(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(greaterThan(param1, param2));
  }

  protected abstract TFormulaInfo greaterThan(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public BooleanFormula greaterOrEquals(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(greaterOrEquals(param1, param2));
  }

  protected abstract TFormulaInfo greaterOrEquals(TFormulaInfo pParam1, TFormulaInfo pParam2) ;

  @Override
  public BooleanFormula lessThan(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(lessThan(param1, param2));
  }

  protected abstract TFormulaInfo lessThan(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public BooleanFormula lessOrEquals(RationalFormula pNumber1, RationalFormula pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(lessOrEquals(param1, param2));
  }

  protected abstract TFormulaInfo lessOrEquals(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public boolean isNegate(RationalFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isNegate(param);
  }
  protected abstract boolean isNegate(TFormulaInfo pParam) ;

  @Override
  public boolean isAdd(RationalFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isAdd(param);
  }
  protected abstract boolean isAdd(TFormulaInfo pParam);


  @Override
  public boolean isSubtract(RationalFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isSubtract(param);
  }

  protected abstract boolean isSubtract(TFormulaInfo pParam);


  @Override
  public boolean isDivide(RationalFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isDivide(param);
  }
  protected  abstract boolean isDivide(TFormulaInfo pParam) ;


  @Override
  public boolean isModulo(RationalFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isModulo(param);
  }

  protected  abstract boolean isModulo(TFormulaInfo pParam) ;


  @Override
  public boolean isMultiply(RationalFormula pNumber) {
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
  public boolean isGreaterThan(BooleanFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isGreaterThan(param);
  }
  protected abstract boolean isGreaterThan(TFormulaInfo pParam) ;

  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isGreaterOrEquals(param);
  }
  protected abstract boolean isGreaterOrEquals(TFormulaInfo pParam) ;

  @Override
  public boolean isLessThan(BooleanFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isLessThan(param);
  }
  protected abstract boolean isLessThan(TFormulaInfo pParam) ;

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isLessOrEquals(param);
  }
  protected abstract boolean isLessOrEquals(TFormulaInfo pParam) ;

}
