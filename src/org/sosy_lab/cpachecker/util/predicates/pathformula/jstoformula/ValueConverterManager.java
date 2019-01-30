/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.OBJECT_TYPE;

import com.google.common.collect.Lists;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;

/**
 * Converter of a {@link TypedValue} to the formula encoding of its value to a specific type (see
 * {@link TypeTags}).
 *
 * <p>Since a {@link TypedValue} might contain a value of any type, formula encodings for each
 * possible case (type) of the value have to be combined to a formula. Therefore, every value has to
 * be convertible to a value of each type. The conversion is usually only relevant in the case where
 * the type of the {@link TypedValue} equals the target type of the conversion. An exception is the
 * coercion to another type.
 *
 * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9">Type Conversion and
 *     Testing</a>
 */
class ValueConverterManager {

  private final TypedVariableValues typedVarValues;
  private final TypeTags typeTags;
  private final TypedValueManager tvmgr;
  private final StringFormulaManager strMgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FloatingPointFormulaManagerView fpfmgr;
  private final BitvectorFormulaManagerView bitVecMgr;

  ValueConverterManager(
      final TypedVariableValues pTypedVariableValues,
      final TypeTags pTypeTags,
      final TypedValueManager pTvmgr,
      final StringFormulaManager pStrMgr,
      final FormulaManagerView pFmgr) {
    typedVarValues = pTypedVariableValues;
    typeTags = pTypeTags;
    tvmgr = pTvmgr;
    strMgr = pStrMgr;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    fpfmgr = fmgr.getFloatingPointFormulaManager();
    bitVecMgr = fmgr.getBitvectorFormulaManager();
  }

  /**
   * Converts the type value to its function formula. Everything except functions is converted to an
   * unknown function ID.
   *
   * @param pValue The typed value whose value should be converted to a function based on its type.
   * @return The value converted to its function formula.
   */
  IntegerFormula toFunction(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    final IntegerFormula notAFunction = fmgr.makeNumber(Types.FUNCTION_TYPE, 0);
    if (Lists.newArrayList(
            typeTags.BOOLEAN, typeTags.NUMBER, typeTags.OBJECT, typeTags.STRING, typeTags.UNDEFINED)
        .contains(type)) {
      return notAFunction;
    } else if (type.equals(typeTags.FUNCTION)) {
      return (IntegerFormula) pValue.getValue();
    }
    final IntegerFormula variable = (IntegerFormula) pValue.getValue();
    return bfmgr.ifThenElse(
        fmgr.makeEqual(type, typeTags.FUNCTION),
        typedVarValues.functionValue(variable),
        notAFunction);
  }

  /**
   * Converts the type value to its object formula. Everything except objects is converted to an
   * unknown object ID.
   *
   * @param pValue The typed value whose value should be converted to a object based on its type.
   * @return The value converted to its object formula.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.9">ToObject</a>
   */
  IntegerFormula toObject(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    final IntegerFormula unknownObjectValue = fmgr.makeNumber(OBJECT_TYPE, -1);
    // TODO convert boolean, number and string to Boolean/Number/String object
    if (Lists.newArrayList(
            typeTags.BOOLEAN,
            typeTags.NUMBER,
            typeTags.STRING,
            typeTags.UNDEFINED)
        .contains(type)) {
      return unknownObjectValue;
    } else if (type.equals(typeTags.OBJECT) || type.equals(typeTags.FUNCTION)) {
      return (IntegerFormula) pValue.getValue();
    }
    final IntegerFormula variable = (IntegerFormula) pValue.getValue();
    return bfmgr.ifThenElse(
        bfmgr.or(fmgr.makeEqual(type, typeTags.OBJECT), fmgr.makeEqual(type, typeTags.FUNCTION)),
        typedVarValues.objectValue(variable),
        unknownObjectValue);
  }

  /**
   * Converts the type value to its string formula. Everything except strings is converted to an
   * unknown string ID.
   *
   * @param pValue The typed value whose value should be converted to a string based on its type.
   * @return The value converted to its string formula.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.8">ToString</a>
   */
  RationalFormula toStringFormula(final TypedValue pValue) {
    // TODO convert boolean, number, object, function and undefined to string (update comment)
    final IntegerFormula type = pValue.getType();
    final RationalFormula unknownStringValue = fmgr.makeNumber(Types.STRING_TYPE, 0);
    if (Lists.newArrayList(
            typeTags.BOOLEAN,
            typeTags.NUMBER,
            typeTags.OBJECT,
            typeTags.FUNCTION,
            typeTags.UNDEFINED)
        .contains(type)) {
      return unknownStringValue;
    } else if (type.equals(typeTags.STRING)) {
      return (RationalFormula) pValue.getValue();
    }
    final IntegerFormula variable = (IntegerFormula) pValue.getValue();
    return bfmgr.ifThenElse(
        fmgr.makeEqual(type, typeTags.STRING),
        typedVarValues.stringValue(variable),
        unknownStringValue);
  }

  /**
   * Converts the type value to its boolean formula.
   *
   * @param pValue The typed value whose value should be converted to a boolean based on its type.
   * @return The value converted to its boolean formula.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.2">ToBoolean</a>
   */
  BooleanFormula toBoolean(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    if (type.equals(typeTags.BOOLEAN)) {
      return (BooleanFormula) pValue.getValue();
    } else if (type.equals(typeTags.FUNCTION)) {
      return bfmgr.makeTrue();
    } else if (type.equals(typeTags.NUMBER)) {
      return numberToBoolean((FloatingPointFormula) pValue.getValue());
    } else if (type.equals(typeTags.STRING)) {
      return stringToBoolean((RationalFormula) pValue.getValue());
    } else if (type.equals(typeTags.UNDEFINED)) {
      return bfmgr.makeFalse();
    } else if (type.equals(typeTags.OBJECT)) {
      return bfmgr.not(fmgr.makeEqual(tvmgr.getNullValue().getValue(), pValue.getValue()));
    } else {
      // variable
      final IntegerFormula variable = (IntegerFormula) pValue.getValue();
      return bfmgr.ifThenElse(
          fmgr.makeEqual(type, typeTags.BOOLEAN),
          typedVarValues.booleanValue(variable),
          bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.NUMBER),
              numberToBoolean(typedVarValues.numberValue(variable)),
              bfmgr.ifThenElse(
                  fmgr.makeEqual(type, typeTags.OBJECT),
                  bfmgr.not(
                      fmgr.makeEqual(
                          tvmgr.getNullValue().getValue(), typedVarValues.objectValue(variable))),
                  bfmgr.ifThenElse(
                      fmgr.makeEqual(type, typeTags.STRING),
                      stringToBoolean(typedVarValues.stringValue(variable)),
                      fmgr.makeEqual(type, typeTags.FUNCTION)))));
    }
  }

  /**
   * Converts the type value to its boolean formula. Strings and objects are not fully supported as
   * input values. All strings are converted to <code>NaN</code>. All objects (including the <a
   * href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.11">null value</a>) are
   * converted to <code>0</code>.
   *
   * @param pValue The typed value whose value should be converted to a number based on its type.
   * @return The value converted to its number formula.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.3">ToNumber</a>
   */
  FloatingPointFormula toNumber(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    if (type.equals(typeTags.BOOLEAN)) {
      return booleanToNumber((BooleanFormula) pValue.getValue());
    } else if (type.equals(typeTags.FUNCTION)) {
      return fpfmgr.makeNaN(Types.NUMBER_TYPE);
    } else if (type.equals(typeTags.NUMBER)) {
      return (FloatingPointFormula) pValue.getValue();
    } else if (type.equals(typeTags.STRING)) {
      // TODO string to number conversion of string constants should be possible
      // For now, assume that every string is not a StringNumericLiteral, see
      // https://www.ecma-international.org/ecma-262/5.1/#sec-9.3
      return fpfmgr.makeNaN(Types.NUMBER_TYPE);
    } else if (type.equals(typeTags.UNDEFINED)) {
      return fpfmgr.makeNaN(Types.NUMBER_TYPE);
    } else if (type.equals(typeTags.OBJECT)) {
      return fmgr.makeNumber(Types.NUMBER_TYPE, 0); // TODO handle non null objects
    } else {
      // variable
      final IntegerFormula variable = (IntegerFormula) pValue.getValue();
      return bfmgr.ifThenElse(
          fmgr.makeEqual(type, typeTags.BOOLEAN),
          booleanToNumber(typedVarValues.booleanValue(variable)),
          bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.NUMBER),
              typedVarValues.numberValue(variable),
              bfmgr.ifThenElse(
                  fmgr.makeEqual(type, typeTags.OBJECT),
                  fmgr.makeNumber(Types.NUMBER_TYPE, 0), // TODO handle non null objects
                  fpfmgr.makeNaN(Types.NUMBER_TYPE))));
    }
  }

  /**
   * Convert string to boolean. <cite>The result is false if the argument is the empty String (its
   * length is zero); otherwise the result is true.</cite>
   *
   * @param pValue The string ID to convert to boolean.
   * @return Boolean formula of converted string ID.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.2">ToBoolean</a>
   */
  @Nonnull
  private BooleanFormula stringToBoolean(final RationalFormula pValue) {
    return bfmgr.not(fmgr.makeEqual(pValue, strMgr.getStringFormula("")));
  }

  /**
   * Convert number formula to boolean value formula. <cite>The result is false if the argument is
   * +0, −0, or NaN; otherwise the result is true.</cite>
   *
   * @param pValue The number formula to convert to boolean.
   * @return Boolean formula of converted number formula.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.2">ToBoolean</a>
   */
  private BooleanFormula numberToBoolean(final FloatingPointFormula pValue) {
    return bfmgr.ifThenElse(
        bfmgr.or(fpfmgr.isZero(pValue), fpfmgr.isNaN(pValue)), bfmgr.makeFalse(), bfmgr.makeTrue());
  }

  /**
   * Convert boolean formula to number value formula. <cite>The result is 1 if the argument is true.
   * The result is +0 if the argument is false.</cite>
   *
   * @param pValue The boolean formula to convert to number.
   * @return Boolean formula of converted boolean formula.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.3">ToNumber</a>
   */
  private FloatingPointFormula booleanToNumber(final BooleanFormula pValue) {
    return bfmgr.ifThenElse(
        pValue, fmgr.makeNumber(Types.NUMBER_TYPE, 1), fmgr.makeNumber(Types.NUMBER_TYPE, 0));
  }

  /**
   * <cite>The abstract operation ToInt32 converts its argument to one of 2<sup>32</sup> integer
   * values in the range −2<sup>31</sup> through 2<sup>31</sup>−1, inclusive.</cite>
   *
   * @param pValue Value to convert to Int32.
   * @return Converted value is Int32 encoded as 32 bitvector.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.5">9.5 ToInt32: (Signed 32
   *     Bit Integer)</a>
   */
  BitvectorFormula toInt32(final TypedValue pValue) {
    // 1. Let number be the result of calling ToNumber on the input argument.
    return toInt32(toNumber(pValue));
  }

  /**
   * <cite>The abstract operation ToInt32 converts its argument to one of 2<sup>32</sup> integer
   * values in the range −2<sup>31</sup> through 2<sup>31</sup>−1, inclusive.</cite>
   *
   * @param pNumber Value to convert to Int32.
   * @return Converted value is Int32 encoded as 32 bitvector.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-9.5">9.5 ToInt32: (Signed 32
   *     Bit Integer)</a>
   */
  @Nonnull
  BitvectorFormula toInt32(final FloatingPointFormula pNumber) {
    final BitvectorType bv32 = FormulaType.getBitvectorTypeWithSize(32);
    final BitvectorType bv1026 = FormulaType.getBitvectorTypeWithSize(1026);
    final BitvectorFormula i2Pow31 = bitVecMgr.makeBitvector(bv1026, 2147483648L); // 2^31
    final BitvectorFormula i2Pow32 = bitVecMgr.makeBitvector(bv1026, 4294967296L); // 2^32

    // 3. Let posInt be sign(number) * floor(abs(number)).
    final BitvectorFormula posInt =
        fpfmgr.castTo(pNumber, bv1026, FloatingPointRoundingMode.TOWARD_ZERO);

    // 4. Let int32bit be posInt modulo 2^32; that is, a finite integer value k of Number type
    // with positive sign and less than 2^32 in magnitude such that the mathematical difference of
    // posInt and k is mathematically an integer multiple of 2^32.
    final BitvectorFormula int32bit = bitVecMgr.modulo(posInt, i2Pow32, true);

    // 5. If int32bit is greater than or equal to 2^31, return int32bit - 2^32, otherwise return
    // int32bit.
    final BitvectorFormula intResult =
        bfmgr.ifThenElse(
            bitVecMgr.greaterOrEquals(int32bit, i2Pow31, true),
            bitVecMgr.subtract(int32bit, i2Pow32),
            int32bit);
    final BitvectorFormula bv32Result = bitVecMgr.extract(intResult, 31, 0, true);

    // 2. If number is NaN, +0, −0, +∞, or −∞, return +0.
    return bfmgr.ifThenElse(
        bfmgr.or(fpfmgr.isNaN(pNumber), fpfmgr.isInfinity(pNumber)),
        bitVecMgr.makeBitvector(bv32, 0),
        bv32Result);
  }
}
