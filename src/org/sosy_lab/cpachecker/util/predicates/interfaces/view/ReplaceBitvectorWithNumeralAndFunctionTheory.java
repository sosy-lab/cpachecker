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

import static org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.getBitvectorTypeWithSize;
import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.BitvectorType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;

import com.google.common.collect.Lists;

class ReplaceBitvectorWithNumeralAndFunctionTheory<T extends NumeralFormula>
  extends BaseManagerView implements BitvectorFormulaManager {

  private final BooleanFormulaManager booleanFormulaManager;
  private final NumeralFormulaManager<? super T, T> numericFormulaManager;
  private final FunctionFormulaManager functionManager;
  private final UninterpretedFunctionDeclaration<T> bitwiseAndUfDecl;
  private final UninterpretedFunctionDeclaration<T> bitwiseOrUfDecl;
  private final UninterpretedFunctionDeclaration<T> bitwiseXorUfDecl;
  private final UninterpretedFunctionDeclaration<T> bitwiseNotUfDecl;
  private final UninterpretedFunctionDeclaration<T> leftShiftUfDecl;
  private final UninterpretedFunctionDeclaration<T> rightShiftUfDecl;
  private final FormulaType<T> formulaType;
  private final boolean ignoreExtractConcat;
  private final boolean handleOverflowsWithUFs;
  private final boolean handleSignConversionWithUFs;


  public ReplaceBitvectorWithNumeralAndFunctionTheory(
      FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pBooleanFormulaManager,
      NumeralFormulaManager<? super T, T> rawNumericFormulaManager,
      FunctionFormulaManager rawFunctionManager,
      final boolean ignoreExtractConcat,
      final boolean handleOverflowsWithUFs,
      final boolean handleSignConversionWithUFs) {
    super(pWrappingHandler);
    booleanFormulaManager = pBooleanFormulaManager;
    numericFormulaManager = rawNumericFormulaManager;
    this.ignoreExtractConcat = ignoreExtractConcat;
    this.handleOverflowsWithUFs = handleOverflowsWithUFs;
    this.handleSignConversionWithUFs = handleSignConversionWithUFs;
    this.functionManager = rawFunctionManager;

    formulaType = numericFormulaManager.getFormulaType();
    bitwiseAndUfDecl = createBinaryFunction(BitwiseAndUfName);
    bitwiseOrUfDecl = createBinaryFunction(BitwiseOrUfName);
    bitwiseXorUfDecl = createBinaryFunction(BitwiseXorUfName);
    bitwiseNotUfDecl = createUnaryFunction(BitwiseNotUfName);

    leftShiftUfDecl = createBinaryFunction("_<<_");
    rightShiftUfDecl = createBinaryFunction("_>>_");
  }

  @SuppressWarnings("unchecked")
  private T unwrap(BitvectorFormula pNumber) {
    return (T)super.unwrap(pNumber);
  }

  private UninterpretedFunctionDeclaration<T> createUnaryFunction(String name) {
    return functionManager.declareUninterpretedFunction(name, formulaType, formulaType);
  }

  private UninterpretedFunctionDeclaration<T> createBinaryFunction(String name) {
    return functionManager.declareUninterpretedFunction(name, formulaType, formulaType, formulaType);
  }

  private BitvectorFormula makeUf(FormulaType<BitvectorFormula> realreturn, UninterpretedFunctionDeclaration<T> decl, BitvectorFormula... t1) {
    List<Formula> args = unwrap(Arrays.<Formula>asList(t1));

    return wrap(realreturn, functionManager.callUninterpretedFunction(decl, args));
  }

  private final Map<Integer[], UninterpretedFunctionDeclaration<T>> extractMethods = new HashMap<>();

  private UninterpretedFunctionDeclaration<T> getExtractDecl(int pMsb, int pLsb) {
    Integer[] hasKey = new Integer[]{pMsb, pLsb};
    UninterpretedFunctionDeclaration<T> value = extractMethods.get(hasKey);
    if (value == null) {
      value = createUnaryFunction("_extract("+ pMsb + "," + pLsb + ")_");
      extractMethods.put(hasKey, value);
    }
    return value;
  }

  private Map<Integer[], UninterpretedFunctionDeclaration<T>> concatMethods = new HashMap<>();

  private UninterpretedFunctionDeclaration<T> getConcatDecl(int firstSize, int secoundSize) {
    Integer[] hasKey = new Integer[]{firstSize, secoundSize};
    UninterpretedFunctionDeclaration<T> value = concatMethods.get(hasKey);
    if (value == null) {
      value = createUnaryFunction("_concat("+ firstSize + "," + secoundSize + ")_");
      concatMethods.put(hasKey, value);
    }
    return value;
  }

  private final Map<Triple<String, Boolean, Integer>, UninterpretedFunctionDeclaration<T>> UFDeclarations = new HashMap<>();

  private UninterpretedFunctionDeclaration<T> getUFDecl(String name, int id, boolean signed) {
    Triple<String, Boolean, Integer> key = Triple.of(name, signed, id);
    UninterpretedFunctionDeclaration<T> value = UFDeclarations.get(key);
    if (value == null) {
      String UFname = String.format("_%s%s(%d)_", name, (signed ? "Signed" : "Unsigned"), id);
      value = createUnaryFunction(UFname);
      UFDeclarations.put(key, value);
    }
    return value;
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    assert BigInteger.valueOf(pI).bitLength() <= pLength:
      String.format("numeral value %s is too big for bitvector of length %d.", pI, pLength);
    T number = numericFormulaManager.makeNumber(pI);
    return wrap(getBitvectorTypeWithSize(pLength), number);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    assert pI.bitLength() <= pLength:
      String.format("numeral value %s is too big for bitvector of length %d.", pI, pLength);
    T number = numericFormulaManager.makeNumber(pI);
    return wrap(getBitvectorTypeWithSize(pLength), number);
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return wrap(getBitvectorTypeWithSize(pLength), numericFormulaManager.makeVariable(pVar));
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return ((BitvectorType)getFormulaType(pNumber)).getSize();
  }

  private static Pair<BigInteger, BigInteger> getMinAndMax(int pLength, boolean signed) {
    assert pLength > 0;
    BigInteger maxInt;
    BigInteger minInt;
    if (signed) {
      // signed integer : [2^31, 2^31-1]
      minInt = BigInteger.ONE.shiftLeft(pLength - 1).negate();
      maxInt = BigInteger.ONE.shiftLeft(pLength - 1).subtract(BigInteger.ONE);
    } else {
      // unsigned integer : [0, 2^32-1]
      minInt = BigInteger.ZERO;
      maxInt = BigInteger.ONE.shiftLeft(pLength).subtract(BigInteger.ONE);
    }
    return Pair.of(minInt, maxInt);
  }


  /**
   * Replace the formula with a matching UF, if the formula causes an overflow, else keep it.
   *
   * @param pValue that should be replaced
   * @param pLength size of bitvector to determine the current range of values
   * @param pSigned to determine the current range of values
   */
  private T replaceOverflowWithUF(T pValue, int pLength, boolean pSigned) {

    Pair<BigInteger, BigInteger> minMax = getMinAndMax(pLength, pSigned);
    BooleanFormula lower  = numericFormulaManager.lessOrEquals(numericFormulaManager.makeNumber(minMax.getFirst()), pValue);
    BooleanFormula higher = numericFormulaManager.lessOrEquals(pValue, numericFormulaManager.makeNumber(minMax.getSecond()));

    UninterpretedFunctionDeclaration<T> decl = getUFDecl("overflow", pLength, pSigned);
    T overflowUF = functionManager.callUninterpretedFunction(decl, Lists.<Formula>newArrayList(pValue));

    // TODO improvement:
    // Add special handling for a constant number of overflows (N=1 or N=2).
    // This would allow to catch overflows from ADD and SUBTRACT.

    // if (value in [MIN,MAX])   then return (value)  else return UF(value)
    return booleanFormulaManager.ifThenElse(booleanFormulaManager.and(lower, higher), pValue, overflowUF);
  }

  private BitvectorFormula handleOverflow(FormulaType<BitvectorFormula> pFormulaType, T pValue, boolean pSigned) {
    if (handleOverflowsWithUFs) {
      pValue = replaceOverflowWithUF(pValue, ((BitvectorType)pFormulaType).getSize(), pSigned);
    }
    return wrap(pFormulaType, pValue);
  }

  private T handleOverflow(BitvectorFormula pValue, boolean pSigned) {
    T result = unwrap(pValue);
    if (handleSignConversionWithUFs) {
      result = replaceOverflowWithUF(result, getLength(pValue), pSigned);
    }
    return result;
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber, boolean signed) {
    return handleOverflow(getFormulaType(pNumber), numericFormulaManager.negate(unwrap(pNumber)), signed);
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return handleOverflow(getFormulaType(pNumber1), numericFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)), signed);
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return handleOverflow(getFormulaType(pNumber1), numericFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)), signed);
  }

  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), getC99ReplacementForSMTlib2Division(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), getC99ReplacementForSMTlib2Modulo(unwrap(pNumber1), unwrap(pNumber2)));
  }


  /**
   * @see BitvectorFormulaManagerView#divide(BitvectorFormula, BitvectorFormula, boolean)
   */
  private Formula getC99ReplacementForSMTlib2Division(final T f1, final T f2) {

    final T zero = numericFormulaManager.makeNumber(0);
    final T additionalUnit = booleanFormulaManager.ifThenElse(
        numericFormulaManager.greaterOrEquals(f2, zero),
        numericFormulaManager.makeNumber(1),
        numericFormulaManager.makeNumber(-1));
    final T div = numericFormulaManager.divide(f1, f2);

    // IF   first operand is positive or is divisible by second operand
    // THEN return plain division --> here C99 is equal to SMTlib2
    // ELSE divide and add an additional unit towards the nearest infinity.

    return booleanFormulaManager.ifThenElse(
        booleanFormulaManager.or(
            numericFormulaManager.greaterOrEquals(f1, zero),
            numericFormulaManager.equal(numericFormulaManager.multiply(div, f2), f1)),
        div,
        numericFormulaManager.add(div, additionalUnit));
  }


  /**
   * @see BitvectorFormulaManagerView#modulo(BitvectorFormula, BitvectorFormula, boolean)
   */
  private Formula getC99ReplacementForSMTlib2Modulo(final T f1, final T f2) {

    final T zero = numericFormulaManager.makeNumber(0);
    final T additionalUnit = booleanFormulaManager.ifThenElse(
        numericFormulaManager.greaterOrEquals(f2, zero),
        numericFormulaManager.negate(f2),
        f2);
    final T mod = numericFormulaManager.modulo(f1, f2);

    // IF   first operand is positive or mod-result is zero
    // THEN return plain modulo --> here C99 is equal to SMTlib2
    // ELSE modulo and add an additional unit towards the nearest infinity.

    return booleanFormulaManager.ifThenElse(
        booleanFormulaManager.or(
            numericFormulaManager.greaterOrEquals(f1, zero),
            numericFormulaManager.equal(mod, zero)),
        mod,
        numericFormulaManager.add(mod, additionalUnit));
  }

  @Override
  public BooleanFormula modularCongruence(BitvectorFormula pNumber1, BitvectorFormula pNumber2, long pModulo) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.modularCongruence(unwrap(pNumber1), unwrap(pNumber2), pModulo);
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return handleOverflow(getFormulaType(pNumber1), numericFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)), signed);
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    boolean signed = true; // signedness is irrelevant for equality
    return numericFormulaManager.equal(handleOverflow(pNumber1, signed), handleOverflow(pNumber2, signed));
    // return numericFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.greaterThan(handleOverflow(pNumber1, pSigned), handleOverflow(pNumber2, pSigned));
  }

  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.greaterOrEquals(handleOverflow(pNumber1, pSigned), handleOverflow(pNumber2, pSigned));
  }

  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.lessThan(handleOverflow(pNumber1, pSigned), handleOverflow(pNumber2, pSigned));
  }

  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.lessOrEquals(handleOverflow(pNumber1, pSigned), handleOverflow(pNumber2, pSigned));
  }


  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by toShift.
   */
  @Override
  public BitvectorFormula shiftRight(BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    assert getLength(pNumber) == getLength(pToShift) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pNumber), rightShiftUfDecl, pNumber, pToShift);
  }

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    assert getLength(pNumber) == getLength(pToShift) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pNumber), leftShiftUfDecl, pNumber, pToShift);
  }

  @Override
  public BitvectorFormula concat(BitvectorFormula pFirst, BitvectorFormula pSecound) {
    int firstLength = getLength(pFirst);
    int secoundLength = getLength(pSecound);
    FormulaType<BitvectorFormula> returnType = getBitvectorTypeWithSize(firstLength + secoundLength);
    if (ignoreExtractConcat) {
      return wrap(returnType, unwrap(pSecound));
    }
    UninterpretedFunctionDeclaration<T> concatUfDecl = getConcatDecl(firstLength, secoundLength);
    return makeUf(returnType, concatUfDecl, pFirst, pSecound);
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pFirst, int pMsb, int pLsb, boolean signed) {
    FormulaType<BitvectorFormula> returnType = getBitvectorTypeWithSize(pMsb + 1 - pLsb);
    if (ignoreExtractConcat) {
      return wrap(returnType, unwrap(pFirst));
    }
    UninterpretedFunctionDeclaration<T> extractUfDecl = getExtractDecl(pMsb, pLsb);
    return makeUf(returnType, extractUfDecl, pFirst);
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    FormulaType<BitvectorFormula> returnType = getBitvectorTypeWithSize(getLength(pNumber) + pExtensionBits);
    if (ignoreExtractConcat) {
      return wrap(returnType, unwrap(pNumber));
    }
    UninterpretedFunctionDeclaration<T> extendUfDecl = getUFDecl("extend", pExtensionBits, pSigned);
    return makeUf(returnType, extendUfDecl, pNumber);
  }

  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return makeUf(getFormulaType(pBits), bitwiseNotUfDecl, pBits);
  }

  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pBits1), bitwiseAndUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pBits1), bitwiseOrUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    assert getLength(pBits1) == getLength(pBits2) : "Expect operators to have the same size";
    return makeUf(getFormulaType(pBits1), bitwiseXorUfDecl, pBits1, pBits2);
  }
}
