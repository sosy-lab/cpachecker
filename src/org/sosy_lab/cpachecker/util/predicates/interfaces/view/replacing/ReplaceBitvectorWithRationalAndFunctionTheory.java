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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.replacing;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.BitvectorType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

import com.google.common.base.Function;


public class ReplaceBitvectorWithRationalAndFunctionTheory implements BitvectorFormulaManager {

  private final RationalFormulaManager rationalFormulaManager;
  private final FunctionFormulaManager functionManager;
  private final ReplacingFormulaManager replaceManager;
  private final FunctionFormulaType<RationalFormula> bitwiseAndUfDecl;
  private final FunctionFormulaType<RationalFormula> bitwiseOrUfDecl;
  private final FunctionFormulaType<RationalFormula> bitwiseXorUfDecl;
  private final FunctionFormulaType<RationalFormula> bitwiseNotUfDecl;
  private final FunctionFormulaType<RationalFormula> leftShiftUfDecl;
  private final FunctionFormulaType<RationalFormula> rightShiftUfDecl;
  private final FormulaType<RationalFormula> formulaType;
  private final boolean ignoreExtractConcat;

  public ReplaceBitvectorWithRationalAndFunctionTheory(
      ReplacingFormulaManager pReplacingFormulaManager,
      RationalFormulaManager pRationalFormulaManager,
      FunctionFormulaManager rawFunctionManager,
      final boolean ignoreExtractConcat) {
    replaceManager = pReplacingFormulaManager;
    rationalFormulaManager = pRationalFormulaManager;
    this.ignoreExtractConcat = ignoreExtractConcat;
    this.functionManager = rawFunctionManager;

    formulaType = FormulaType.RationalType;
    bitwiseAndUfDecl = functionManager.createFunction(BitwiseAndUfName, formulaType, formulaType, formulaType);
    bitwiseOrUfDecl = functionManager.createFunction(BitwiseOrUfName, formulaType, formulaType, formulaType);
    bitwiseXorUfDecl = functionManager.createFunction(BitwiseXorUfName, formulaType, formulaType, formulaType);
    bitwiseNotUfDecl = functionManager.createFunction(BitwiseNotUfName, formulaType, formulaType);


    leftShiftUfDecl = functionManager.createFunction("_<<_", formulaType, formulaType, formulaType);
    rightShiftUfDecl = functionManager.createFunction("_>>_", formulaType, formulaType, formulaType);
  }

  private BitvectorFormula makeUf(FormulaType<BitvectorFormula> realreturn, FunctionFormulaType<RationalFormula> decl, BitvectorFormula... t1) {
    List<BitvectorFormula> wrapped = Arrays.<BitvectorFormula>asList(t1);

    List<Formula> unwrapped = from(wrapped)
      .transform(new Function<BitvectorFormula, Formula>() {
        @Override
        public Formula apply(BitvectorFormula pInput) {
          return unwrap(pInput);
        }
      }).toList();

    return wrap(realreturn, functionManager.createUninterpretedFunctionCall(decl, unwrapped));
  }

  private boolean isUf(FunctionFormulaType<RationalFormula> funcDecl, BitvectorFormula pBits) {

    return functionManager.isUninterpretedFunctionCall(funcDecl, unwrap(pBits));
  }

  private final Map<Integer[], FunctionFormulaType<RationalFormula>> extractMethods = new HashMap<>();

  private FunctionFormulaType<RationalFormula> getExtractDecl(int pMsb, int pLsb) {
    Integer[] hasKey = new Integer[]{pMsb, pLsb};
    FunctionFormulaType<RationalFormula> value = extractMethods.get(hasKey);
    if (value == null) {
      value = functionManager.createFunction("_extract("+ pMsb + "," + pLsb + ")_", formulaType, formulaType);
      extractMethods.put(hasKey, value);
    }
    return value;
  }

  private Map<Integer[], FunctionFormulaType<RationalFormula>> concatMethods = new HashMap<>();

  private FunctionFormulaType<RationalFormula> getConcatDecl(int firstSize, int secoundSize) {
    Integer[] hasKey = new Integer[]{firstSize, secoundSize};
    FunctionFormulaType<RationalFormula> value = concatMethods.get(hasKey);
    if (value == null) {
      value = functionManager.createFunction("_concat("+ firstSize + "," + secoundSize + ")_", formulaType, formulaType);
      concatMethods.put(hasKey, value);
    }
    return value;
  }

  private Map<Integer, FunctionFormulaType<RationalFormula>> extendSignedMethods = new HashMap<>();
  private Map<Integer, FunctionFormulaType<RationalFormula>> extendUnsignedMethods = new HashMap<>();

  private FunctionFormulaType<RationalFormula> getExtendDecl(int extensionBits, boolean pSigned) {
    Integer hasKey = Integer.valueOf(extensionBits);
    FunctionFormulaType<RationalFormula> value;
    if (pSigned) {
      value = extendSignedMethods.get(hasKey);
      if (value == null) {
        value = functionManager.createFunction("_extendSigned("+ extensionBits + ")_", formulaType, formulaType);
        extendSignedMethods.put(hasKey, value);
      }
    } else {
      value = extendUnsignedMethods.get(hasKey);
      if (value == null) {
        value = functionManager.createFunction("_extendUnsigned("+ extensionBits + ")_", formulaType, formulaType);
        extendUnsignedMethods.put(hasKey, value);
      }
    }
    return value;
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    RationalFormula number = rationalFormulaManager.makeNumber(pI);
    return wrap(getFormulaType(pLength), number);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    RationalFormula number = rationalFormulaManager.makeNumber(pI);
    return wrap(getFormulaType(pLength), number);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, String pI) {
    RationalFormula number = rationalFormulaManager.makeNumber(pI);
    return wrap(getFormulaType(pLength), number);
  }

  private BitvectorFormula wrap(FormulaType<BitvectorFormula> pFormulaType, RationalFormula number) {
    return replaceManager.wrap(pFormulaType, number);
  }

  private RationalFormula unwrap(BitvectorFormula pNumber) {
    return replaceManager.unwrap(pNumber);
  }
  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return wrap(getFormulaType(pLength), rationalFormulaManager.makeVariable(pVar));
  }

  @Override
  public FormulaType<BitvectorFormula> getFormulaType(int pLength) {
    return FormulaType.BitvectorType.getBitvectorType(pLength);
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    BitvectorType fmgr = (BitvectorType)replaceManager.getFormulaType(pNumber);
    return fmgr.getSize();
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return wrap(getFormulaType(pNumber), rationalFormulaManager.negate(unwrap(pNumber)));
  }

  private FormulaType<BitvectorFormula> getFormulaType(BitvectorFormula pNumber) {
    return getFormulaType(getLength(pNumber));
  }


  @Override
  public boolean isNegate(BitvectorFormula pNumber) {
    return rationalFormulaManager.isNegate(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), rationalFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isAdd(BitvectorFormula pNumber) {
    return rationalFormulaManager.isAdd(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), rationalFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isSubtract(BitvectorFormula pNumber) {
    return rationalFormulaManager.isSubtract(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), rationalFormulaManager.divide(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isDivide(BitvectorFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isDivide(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), rationalFormulaManager.modulo(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isModulo(BitvectorFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isModulo(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(getFormulaType(pNumber1), rationalFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isMultiply(BitvectorFormula pNumber) {
    return rationalFormulaManager.isMultiply(unwrap(pNumber));
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return rationalFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    return rationalFormulaManager.isEqual(pNumber);
  }

  @Override
  public BooleanFormula greaterThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    return rationalFormulaManager.greaterThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public boolean isGreaterThan(BooleanFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isGreaterThan(pNumber);
  }

  @Override
  public BooleanFormula greaterOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    return rationalFormulaManager.greaterOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isGreaterOrEquals(pNumber);
  }

  @Override
  public BooleanFormula lessThan(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    return rationalFormulaManager.lessThan(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public boolean isLessThan(BooleanFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isLessThan(pNumber);
  }

  @Override
  public BooleanFormula lessOrEquals(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    return rationalFormulaManager.lessOrEquals(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isLessOrEquals(pNumber);
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
    FormulaType<BitvectorFormula> returnType = getFormulaType(firstLength + secoundLength);
    if (ignoreExtractConcat) {
      return wrap(returnType, unwrap(pSecound));
    }
    FunctionFormulaType<RationalFormula> concatUfDecl = getConcatDecl(firstLength, secoundLength);
    return makeUf(returnType, concatUfDecl, pFirst, pSecound);
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pFirst, int pMsb, int pLsb) {
    FormulaType<BitvectorFormula> returnType = getFormulaType(pMsb + 1 - pLsb);
    if (ignoreExtractConcat) {
      return wrap(returnType, unwrap(pFirst));
    }
    FunctionFormulaType<RationalFormula> extractUfDecl = getExtractDecl(pMsb, pLsb);
    return makeUf(returnType, extractUfDecl, pFirst);
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    FormulaType<BitvectorFormula> returnType = getFormulaType(getLength(pNumber) + pExtensionBits);
    if (ignoreExtractConcat) {
      return wrap(returnType, unwrap(pNumber));
    }
    FunctionFormulaType<RationalFormula> extendUfDecl = getExtendDecl(pExtensionBits, pSigned);
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

  @Override
  public boolean isNot(BitvectorFormula pBits) {
    return isUf(bitwiseNotUfDecl, pBits);
  }
  @Override
  public boolean isAnd(BitvectorFormula pBits) {
    return isUf(bitwiseAndUfDecl, pBits);
  }

  @Override
  public boolean isOr(BitvectorFormula pBits) {
    return isUf(bitwiseOrUfDecl, pBits);
  }

  @Override
  public boolean isXor(BitvectorFormula pBits) {
    return isUf(bitwiseXorUfDecl, pBits);
  }

}
