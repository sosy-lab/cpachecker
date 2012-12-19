/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView.*;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;


public class ReplaceBitvectorWithRationalAndFunctionTheory implements BitvectorFormulaManager {

  private RationalFormulaManager rationalFormulaManager;
  private FunctionFormulaManager functionManager;
  private ReplacingFormulaManager replaceManager;
  private FunctionFormulaType<BitvectorFormula> bitwiseAndUfDecl;
  private FunctionFormulaType<BitvectorFormula> bitwiseOrUfDecl;
  private FunctionFormulaType<BitvectorFormula> bitwiseXorUfDecl;
  private FunctionFormulaType<BitvectorFormula> bitwiseNotUfDecl;
  private FunctionFormulaType<BitvectorFormula> leftShiftUfDecl;
  private FunctionFormulaType<BitvectorFormula> rightShiftUfDecl;
  private FunctionFormulaType<BitvectorFormula> concatUfDecl;
  private FormulaType<BitvectorFormula> formulaType;

  public ReplaceBitvectorWithRationalAndFunctionTheory(
      ReplacingFormulaManager pReplacingFormulaManager,
      RationalFormulaManager pRationalFormulaManager,
      FunctionFormulaManager functionManager) {
    replaceManager = pReplacingFormulaManager;
    rationalFormulaManager = pRationalFormulaManager;
    this.functionManager = functionManager;

    // Delegate the conversion to the formulaManager
    formulaType = FormulaType.BitvectorType.getBitvectorType(1);
    bitwiseAndUfDecl = functionManager.createFunction(BitwiseAndUfName, formulaType, Arrays.<FormulaType<?>>asList(formulaType, formulaType));
    bitwiseOrUfDecl = functionManager.createFunction(BitwiseOrUfName, formulaType, Arrays.<FormulaType<?>>asList(formulaType, formulaType));
    bitwiseXorUfDecl = functionManager.createFunction(BitwiseXorUfName, formulaType, Arrays.<FormulaType<?>>asList((FormulaType<?>)formulaType, formulaType));
    bitwiseNotUfDecl = functionManager.createFunction(BitwiseNotUfName, formulaType, Arrays.<FormulaType<?>>asList(formulaType));


    leftShiftUfDecl = functionManager.createFunction("_<<_", formulaType, Arrays.<FormulaType<?>>asList(formulaType, formulaType));
    rightShiftUfDecl = functionManager.createFunction("_>>_", formulaType, Arrays.<FormulaType<?>>asList(formulaType, formulaType));
    concatUfDecl = functionManager.createFunction("_concat_", formulaType, Arrays.<FormulaType<?>>asList(formulaType, formulaType));
  }

  private BitvectorFormula makeUf(FunctionFormulaType<BitvectorFormula> decl, BitvectorFormula... t1) {
    return functionManager.createUninterpretedFunctionCall(decl, Arrays.<Formula>asList(t1));
  }

  private boolean isUf(FunctionFormulaType<BitvectorFormula> funcDecl, BitvectorFormula pBits) {
    return functionManager.isUninterpretedFunctionCall(funcDecl, pBits);
  }

  private Map<Integer[], FunctionFormulaType<BitvectorFormula>> extractMethods =
      new Hashtable<Integer[], FunctionFormulaType<BitvectorFormula>>();
  @SuppressWarnings("unchecked")
  private FunctionFormulaType<BitvectorFormula> getExtractDecl (int pMsb, int pLsb){
    Integer[] hasKey = new Integer[]{pMsb, pLsb};
    FunctionFormulaType<BitvectorFormula> value = extractMethods.get(hasKey);
    if (value == null){
      value = functionManager.createFunction("_extract("+ pMsb + "," + pLsb + ")_", formulaType, Arrays.<FormulaType<?>>asList(formulaType));
      extractMethods.put(hasKey, value);
    }
    return value;
  }


  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    RationalFormula number = rationalFormulaManager.makeNumber(pI);
    return wrap(number);
  }

  private BitvectorFormula wrap(RationalFormula number) {
    return replaceManager.wrap(BitvectorFormula.class, number);
  }

  private RationalFormula unwrap(BitvectorFormula pNumber) {
    return replaceManager.unwrap(pNumber);
  }
  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return wrap(rationalFormulaManager.makeVariable(pVar));
  }

  @Override
  public FormulaType<BitvectorFormula> getFormulaType(int pLength) {
    return FormulaType.BitvectorType.getBitvectorType(1);
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return 1;
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return wrap(rationalFormulaManager.negate(unwrap(pNumber)));
  }


  @Override
  public boolean isNegate(BitvectorFormula pNumber) {
    return rationalFormulaManager.isNegate(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return wrap(rationalFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isAdd(BitvectorFormula pNumber) {
    return rationalFormulaManager.isAdd(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return wrap(rationalFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isSubtract(BitvectorFormula pNumber) {
    return rationalFormulaManager.isSubtract(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula divide(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    return wrap(rationalFormulaManager.divide(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isDivide(BitvectorFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isDivide(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula modulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    return wrap(rationalFormulaManager.modulo(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public boolean isModulo(BitvectorFormula pNumber, boolean pSigned) {
    return rationalFormulaManager.isModulo(unwrap(pNumber));
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return wrap(rationalFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)));
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

  @Override
  public BitvectorFormula shiftRight(BitvectorFormula pToShift, BitvectorFormula pNumber, boolean signed) {
    return makeUf(rightShiftUfDecl, pToShift, pNumber);
  }

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pToShift, BitvectorFormula pNumber) {
    return makeUf(leftShiftUfDecl, pToShift, pNumber);
  }

  @Override
  public BitvectorFormula concat(BitvectorFormula pFirst, BitvectorFormula pSecound) {
    return makeUf(concatUfDecl, pFirst, pSecound);
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pFirst, int pMsb, int pLsb) {
    FunctionFormulaType<BitvectorFormula> extractUfDecl = getExtractDecl(pMsb, pLsb);
    return makeUf(extractUfDecl, pFirst);
  }

  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return makeUf(bitwiseNotUfDecl, pBits);
  }

  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return makeUf(bitwiseAndUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return makeUf(bitwiseOrUfDecl, pBits1, pBits2);
  }

  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return makeUf(bitwiseXorUfDecl, pBits1, pBits2);
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
