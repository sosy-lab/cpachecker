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
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;

/**
 * This AbstractNumericFormulaManager allows you to implement the Rational-Theory by
 * providing a NumericBaseFormulaManager<TFormulaInfo,TFormulaInfo> and implementing 3 methods.
 * @param <TFormulaInfo> the Solver specific type.
 */
public abstract class AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv,
        ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements NumeralFormulaManager<ParamFormulaType, ResultFormulaType> {

  // it is not possible to get a Class from Generics,so we need this field.
  private final Class<ResultFormulaType> formulaType;

  protected AbstractNumeralFormulaManager(
      AbstractFormulaCreator<TFormulaInfo, TType, TEnv> pCreator, Class<ResultFormulaType> pType) {
    super(pCreator);
    formulaType = pType;
  }


  protected TFormulaInfo extractInfo(Formula pNumber) {
    return getFormulaCreator().extractInfo(pNumber);
  }

  protected ResultFormulaType wrap(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(formulaType, pTerm);
  }

  protected BooleanFormula wrapBool(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(BooleanFormula.class, pTerm);
  }

  @Override
  public ResultFormulaType makeNumber(long i) {
    return wrap(makeNumberImpl(i));
  }
  protected abstract TFormulaInfo makeNumberImpl(long i);

  @Override
  public ResultFormulaType makeNumber(BigInteger i) {
    return wrap(makeNumberImpl(i));
  }
  protected abstract TFormulaInfo makeNumberImpl(BigInteger i);

  @Override
  public ResultFormulaType makeNumber(String i) {
    return wrap(makeNumberImpl(i));
  }
  protected abstract TFormulaInfo makeNumberImpl(String i);

  @Override
  public ResultFormulaType makeVariable(String pVar) {
    return wrap(makeVariableImpl(pVar));
  }
  protected abstract TFormulaInfo makeVariableImpl(String i);

  @Override
  public ResultFormulaType negate(ParamFormulaType pNumber) {
    TFormulaInfo param1 = extractInfo(pNumber);
    return wrap(negate(param1));
  }


  protected abstract TFormulaInfo negate(TFormulaInfo pParam1);



  @Override
  public ResultFormulaType add(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(add(param1, param2));
  }

  protected abstract TFormulaInfo add(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public ResultFormulaType subtract(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(subtract(param1, param2));
  }

  protected abstract TFormulaInfo subtract(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public ResultFormulaType divide(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(divide(param1, param2));
  }

  protected abstract TFormulaInfo divide(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public ResultFormulaType modulo(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(modulo(param1, param2));
  }

  protected abstract TFormulaInfo modulo(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(multiply(param1, param2));
  }

  protected abstract TFormulaInfo multiply(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BooleanFormula equal(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(equal(param1, param2));
  }

  protected abstract TFormulaInfo equal(TFormulaInfo pParam1, TFormulaInfo pParam2);


  @Override
  public BooleanFormula greaterThan(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(greaterThan(param1, param2));
  }

  protected abstract TFormulaInfo greaterThan(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public BooleanFormula greaterOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(greaterOrEquals(param1, param2));
  }

  protected abstract TFormulaInfo greaterOrEquals(TFormulaInfo pParam1, TFormulaInfo pParam2) ;

  @Override
  public BooleanFormula lessThan(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(lessThan(param1, param2));
  }

  protected abstract TFormulaInfo lessThan(TFormulaInfo pParam1, TFormulaInfo pParam2) ;


  @Override
  public BooleanFormula lessOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(lessOrEquals(param1, param2));
  }

  protected abstract TFormulaInfo lessOrEquals(TFormulaInfo pParam1, TFormulaInfo pParam2);

  @Override
  public boolean isNegate(ParamFormulaType pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isNegate(param);
  }
  protected abstract boolean isNegate(TFormulaInfo pParam) ;

  @Override
  public boolean isAdd(ParamFormulaType pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isAdd(param);
  }
  protected abstract boolean isAdd(TFormulaInfo pParam);


  @Override
  public boolean isSubtract(ParamFormulaType pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isSubtract(param);
  }

  protected abstract boolean isSubtract(TFormulaInfo pParam);


  @Override
  public boolean isDivide(ParamFormulaType pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isDivide(param);
  }
  protected  abstract boolean isDivide(TFormulaInfo pParam) ;


  @Override
  public boolean isModulo(ParamFormulaType pNumber) {
    TFormulaInfo param = extractInfo(pNumber);
    return isModulo(param);
  }

  protected  abstract boolean isModulo(TFormulaInfo pParam) ;


  @Override
  public boolean isMultiply(ParamFormulaType pNumber) {
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
