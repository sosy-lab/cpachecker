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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Similar to the other Abstract*FormulaManager classes in this package,
 * this class serves as a helper for implementing {@link NumeralFormulaManager}.
 * It handles all the unwrapping and wrapping from and to the {@link Formula}
 * instances, such that the concrete class needs to handle only its own internal types.
 *
 * For {@link #multiply(NumeralFormula, NumeralFormula)},
 * {@link #divide(NumeralFormula, NumeralFormula)}, and
 * {@link #modulo(NumeralFormula, NumeralFormula)},
 * this class even offers an implementation based on UFs.
 * Sub-classes are supposed to override them
 * if they can implement these operations more precisely
 * (for example multiplication with constants should be supported by all solvers
 * and implemented by all sub-classes).
 */
public abstract class AbstractNumeralFormulaManager<TFormulaInfo, TType, TEnv,
        ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements NumeralFormulaManager<ParamFormulaType, ResultFormulaType> {

  private static final String UF_MULTIPLY_NAME = "_*_";
  private static final String UF_DIVIDE_NAME = "_/_";
  private static final String UF_MODULO_NAME = "_%_";

  private final AbstractFunctionFormulaManager<TFormulaInfo, ?, TType, TEnv> functionManager;

  private final UninterpretedFunctionDeclaration<ResultFormulaType> multUfDecl;
  private final UninterpretedFunctionDeclaration<ResultFormulaType> divUfDecl;
  private final UninterpretedFunctionDeclaration<ResultFormulaType> modUfDecl;

  protected AbstractNumeralFormulaManager(
      FormulaCreator<TFormulaInfo, TType, TEnv> pCreator,
      AbstractFunctionFormulaManager<TFormulaInfo, ?, TType, TEnv> pFunctionManager) {
    super(pCreator);
    functionManager = pFunctionManager;

    FormulaType<ResultFormulaType> resultType = getFormulaType();
    multUfDecl = functionManager.declareUninterpretedFunction(resultType + "_" + UF_MULTIPLY_NAME, resultType, resultType, resultType);
    divUfDecl = functionManager.declareUninterpretedFunction(resultType + "_" + UF_DIVIDE_NAME, resultType, resultType, resultType);
    modUfDecl = functionManager.declareUninterpretedFunction(resultType + "_" + UF_MODULO_NAME, resultType, resultType, resultType);
  }

  private TFormulaInfo makeUf(UninterpretedFunctionDeclaration<?> decl, TFormulaInfo t1, TFormulaInfo t2) {
    return functionManager.createUninterpretedFunctionCallImpl(decl, ImmutableList.of(t1, t2));
  }

  protected ResultFormulaType wrap(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulate(getFormulaType(), pTerm);
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

  public ResultFormulaType makeNumber(Rational pRational) {
    return wrap(makeNumberImpl(pRational));
  }
  protected TFormulaInfo makeNumberImpl(Rational pRational) {
    return makeNumberImpl(pRational.toString());
  }

  @Override
  public ResultFormulaType makeNumber(double pNumber) {
    return wrap(makeNumberImpl(pNumber));
  }
  protected abstract TFormulaInfo makeNumberImpl(double pNumber);

  @Override
  public ResultFormulaType makeNumber(BigDecimal pNumber) {
    return wrap(makeNumberImpl(pNumber));
  }
  protected abstract TFormulaInfo makeNumberImpl(BigDecimal pNumber);

  /**
   * This method tries to represent a BigDecimal using only BigInteger.
   * It can be used for implementing {@link #makeNumber(BigDecimal)}
   * when the current theory supports only integers.
   */
  protected final TFormulaInfo decimalAsInteger(BigDecimal val) {
    if (val.scale() <= 0) {
      // actually an integral number
      return makeNumberImpl(convertBigDecimalToBigInteger(val));

    } else {
      // represent x.y by xy / (10^z) where z is the number of digits in y
      // (the "scale" of a BigDecimal)

      BigDecimal n = val.movePointRight(val.scale()); // this is "xy"
      BigInteger numerator = convertBigDecimalToBigInteger(n);

      BigDecimal d = BigDecimal.ONE.scaleByPowerOfTen(val.scale()); // this is "10^z"
      BigInteger denominator = convertBigDecimalToBigInteger(d);
      assert denominator.signum() > 0;

      return divide(makeNumberImpl(numerator), makeNumberImpl(denominator));
    }
  }

  private static BigInteger convertBigDecimalToBigInteger(BigDecimal d)
      throws NumberFormatException {
    try {
      return d.toBigIntegerExact();
    } catch (ArithmeticException e) {
      NumberFormatException nfe = new NumberFormatException("Cannot represent BigDecimal " + d + " as BigInteger");
      nfe.initCause(e);
      throw nfe;
    }
  }

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
  public ResultFormulaType sum(List<ParamFormulaType> operands) {
    return wrap(sumImpl(Lists.transform(operands, extractor)));
  }

  protected TFormulaInfo sumImpl(List<TFormulaInfo> operands) {
    TFormulaInfo result = makeNumberImpl(0);
    for (TFormulaInfo operand : operands) {
      result = add(result, operand);
    }
    return result;
  }

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

  protected TFormulaInfo divide(TFormulaInfo pParam1, TFormulaInfo pParam2) {
    return makeUf(divUfDecl, pParam1, pParam2);
  }


  @Override
  public ResultFormulaType modulo(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(modulo(param1, param2));
  }

  protected TFormulaInfo modulo(TFormulaInfo pParam1, TFormulaInfo pParam2) {
    return makeUf(modUfDecl, pParam1, pParam2);
  }


  @Override
  public BooleanFormula modularCongruence(ParamFormulaType pNumber1, ParamFormulaType pNumber2, long pModulo) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrapBool(modularCongruence(param1, param2, pModulo));
  }

  protected abstract TFormulaInfo modularCongruence(TFormulaInfo pNumber1, TFormulaInfo pNumber2, long pModulo);

  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    TFormulaInfo param1 = extractInfo(pNumber1);
    TFormulaInfo param2 = extractInfo(pNumber2);

    return wrap(multiply(param1, param2));
  }

  protected TFormulaInfo multiply(TFormulaInfo pParam1, TFormulaInfo pParam2) {
    return makeUf(multUfDecl, pParam1, pParam2);
  }


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
}
