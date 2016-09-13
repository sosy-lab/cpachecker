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
package org.sosy_lab.cpachecker.util.predicates.smt;

import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseAndUfName;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseNotUfName;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseOrUfName;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.BitwiseXorUfName;
import static org.sosy_lab.java_smt.api.FormulaType.getBitvectorTypeWithSize;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;
import org.sosy_lab.java_smt.api.UFManager;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ReplaceBitvectorWithNumeralAndFunctionTheory<T extends NumeralFormula>
  extends BaseManagerView implements BitvectorFormulaManager {

  private final BooleanFormulaManager booleanFormulaManager;
  private final NumeralFormulaManager<? super T, T> numericFormulaManager;
  private final UFManager functionManager;
  private final FunctionDeclaration<T> bitwiseAndUfDecl;
  private final FunctionDeclaration<T> bitwiseOrUfDecl;
  private final FunctionDeclaration<T> bitwiseXorUfDecl;
  private final FunctionDeclaration<T> bitwiseNotUfDecl;
  private final FunctionDeclaration<T> leftShiftUfDecl;
  private final FunctionDeclaration<T> rightShiftUfDecl;
  private final FormulaType<T> formulaType;
  private final ReplaceBitvectorEncodingOptions options;

  @Options(prefix="cpa.predicate")
  static class ReplaceBitvectorEncodingOptions {

    ReplaceBitvectorEncodingOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    @Option(secure=true, description="Allows to ignore Concat and Extract Calls when Bitvector theory was replaced with Integer or Rational.")
    private boolean ignoreExtractConcat = true;
  }

  ReplaceBitvectorWithNumeralAndFunctionTheory(
      FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pBooleanFormulaManager,
      NumeralFormulaManager<? super T, T> rawNumericFormulaManager,
      UFManager rawFunctionManager,
      final ReplaceBitvectorEncodingOptions pOptions) {
    super(pWrappingHandler);
    booleanFormulaManager = pBooleanFormulaManager;
    numericFormulaManager = rawNumericFormulaManager;
    this.options = pOptions;
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

  private FunctionDeclaration<T> createUnaryFunction(String name) {
    return functionManager.declareUF(name, formulaType, formulaType);
  }

  private FunctionDeclaration<T> createBinaryFunction(String name) {
    return functionManager.declareUF(name, formulaType, formulaType, formulaType);
  }

  private BitvectorFormula makeUf(FormulaType<BitvectorFormula> realreturn, FunctionDeclaration<T> decl,
                                  BitvectorFormula... t1) {
    List<Formula> args = unwrap(Arrays.<Formula>asList(t1));

    return wrap(realreturn, functionManager.callUF(decl, args));
  }

  private final Map<Integer[], FunctionDeclaration<T>> extractMethods = new HashMap<>();

  private FunctionDeclaration<T> getExtractDecl(int pMsb, int pLsb) {
    Integer[] hasKey = new Integer[]{pMsb, pLsb};
    FunctionDeclaration<T> value = extractMethods.get(hasKey);
    if (value == null) {
      value = createUnaryFunction("_extract("+ pMsb + "," + pLsb + ")_");
      extractMethods.put(hasKey, value);
    }
    return value;
  }

  private Map<Integer[], FunctionDeclaration<T>> concatMethods = new HashMap<>();

  private FunctionDeclaration<T> getConcatDecl(int firstSize, int secoundSize) {
    Integer[] hasKey = new Integer[]{firstSize, secoundSize};
    FunctionDeclaration<T> value = concatMethods.get(hasKey);
    if (value == null) {
      value = createUnaryFunction("_concat("+ firstSize + "," + secoundSize + ")_");
      concatMethods.put(hasKey, value);
    }
    return value;
  }

  private final Map<Triple<String, Boolean, Integer>, FunctionDeclaration<T>> UFDeclarations = new HashMap<>();

  private FunctionDeclaration<T> getUFDecl(String name, int id, boolean signed) {
    Triple<String, Boolean, Integer> key = Triple.of(name, signed, id);
    FunctionDeclaration<T> value = UFDeclarations.get(key);
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
  public BitvectorFormula makeVariable(BitvectorType type, String pVar) {
    return wrap(type, numericFormulaManager.makeVariable(pVar));
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return ((BitvectorType)getFormulaType(pNumber)).getSize();
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return wrap(getFormulaType(pNumber), numericFormulaManager.negate(unwrap(pNumber)));
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), numericFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), numericFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)));
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
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), numericFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.greaterThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.greaterOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.lessThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return numericFormulaManager.lessOrEquals(unwrap(pNumber1), unwrap(pNumber2));
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
    if (options.ignoreExtractConcat) {
      return wrap(returnType, unwrap(pSecound));
    }
    FunctionDeclaration<T> concatUfDecl = getConcatDecl(firstLength, secoundLength);
    return makeUf(returnType, concatUfDecl, pFirst, pSecound);
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pFirst, int pMsb, int pLsb, boolean signed) {
    FormulaType<BitvectorFormula> returnType = getBitvectorTypeWithSize(pMsb + 1 - pLsb);
    if (options.ignoreExtractConcat) {
      return wrap(returnType, unwrap(pFirst));
    }
    FunctionDeclaration<T> extractUfDecl = getExtractDecl(pMsb, pLsb);
    return makeUf(returnType, extractUfDecl, pFirst);
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    FormulaType<BitvectorFormula> returnType = getBitvectorTypeWithSize(getLength(pNumber) + pExtensionBits);
    if (options.ignoreExtractConcat) {
      return wrap(returnType, unwrap(pNumber));
    }
    FunctionDeclaration<T> extendUfDecl = getUFDecl("extend", pExtensionBits, pSigned);
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
