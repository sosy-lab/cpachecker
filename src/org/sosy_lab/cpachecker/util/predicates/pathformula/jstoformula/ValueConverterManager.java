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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class ValueConverterManager {

  private final TypedValues typedValues;
  private final TypeTags typeTags;
  private final TypedValueManager tvmgr;
  private final StringFormulaManager strMgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final FloatingPointFormulaManagerView fpfmgr;

  ValueConverterManager(
      final TypedValues pTypedValues,
      final TypeTags pTypeTags,
      final TypedValueManager pTvmgr,
      final StringFormulaManager pStrMgr,
      final FormulaManagerView pFmgr) {
    typedValues = pTypedValues;
    typeTags = pTypeTags;
    tvmgr = pTvmgr;
    strMgr = pStrMgr;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    fpfmgr = fmgr.getFloatingPointFormulaManager();
  }

  IntegerFormula toFunction(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    final IntegerFormula notAFunction = fmgr.makeNumber(Types.FUNCTION_TYPE, 0);
    if (Lists.newArrayList(
            typeTags.BOOLEAN, typeTags.NUMBER, typeTags.OBJECT, typeTags.STRING, typeTags.UNDEFINED)
        .contains(type)) {
      return notAFunction;
    } else if (type.equals(typeTags.FUNCTION)) {
      return typedValues.functionValue((IntegerFormula) pValue.getValue());
    }
    final IntegerFormula variable = (IntegerFormula) pValue.getValue();
    return bfmgr.ifThenElse(
        fmgr.makeEqual(type, typeTags.FUNCTION), typedValues.functionValue(variable), notAFunction);
  }

  IntegerFormula toObject(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    final IntegerFormula unknownObjectValue = fmgr.makeNumber(OBJECT_TYPE, -1);
    if (Lists.newArrayList(
            typeTags.BOOLEAN,
            typeTags.NUMBER,
            typeTags.STRING,
            typeTags.FUNCTION,
            typeTags.UNDEFINED)
        .contains(type)) {
      return unknownObjectValue;
    } else if (type.equals(typeTags.OBJECT)) {
      return (IntegerFormula) pValue.getValue();
    }
    final IntegerFormula variable = (IntegerFormula) pValue.getValue();
    return bfmgr.ifThenElse(
        fmgr.makeEqual(type, typeTags.OBJECT),
        typedValues.objectValue(variable),
        unknownObjectValue);
  }

  IntegerFormula toStringFormula(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    final IntegerFormula unknownStringValue = fmgr.makeNumber(Types.STRING_TYPE, 0);
    if (Lists.newArrayList(
            typeTags.BOOLEAN,
            typeTags.NUMBER,
            typeTags.OBJECT,
            typeTags.FUNCTION,
            typeTags.UNDEFINED)
        .contains(type)) {
      return unknownStringValue;
    } else if (type.equals(typeTags.STRING)) {
      return (IntegerFormula) pValue.getValue();
    }
    final IntegerFormula variable = (IntegerFormula) pValue.getValue();
    return bfmgr.ifThenElse(
        fmgr.makeEqual(type, typeTags.STRING),
        typedValues.stringValue(variable),
        unknownStringValue);
  }

  BooleanFormula toBoolean(final TypedValue pValue) {
    final IntegerFormula type = pValue.getType();
    if (type.equals(typeTags.BOOLEAN)) {
      return (BooleanFormula) pValue.getValue();
    } else if (type.equals(typeTags.FUNCTION)) {
      return bfmgr.makeTrue();
    } else if (type.equals(typeTags.NUMBER)) {
      return numberToBoolean((FloatingPointFormula) pValue.getValue());
    } else if (type.equals(typeTags.STRING)) {
      return stringToBoolean((IntegerFormula) pValue.getValue());
    } else if (type.equals(typeTags.UNDEFINED)) {
      return bfmgr.makeFalse();
    } else if (type.equals(typeTags.OBJECT)) {
      return bfmgr.not(fmgr.makeEqual(tvmgr.getNullValue().getValue(), pValue.getValue()));
    } else {
      // variable
      final IntegerFormula variable = (IntegerFormula) pValue.getValue();
      return bfmgr.ifThenElse(
          fmgr.makeEqual(type, typeTags.BOOLEAN),
          typedValues.booleanValue(variable),
          bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.NUMBER),
              numberToBoolean(typedValues.numberValue(variable)),
              bfmgr.ifThenElse(
                  fmgr.makeEqual(type, typeTags.OBJECT),
                  bfmgr.not(
                      fmgr.makeEqual(
                          tvmgr.getNullValue().getValue(), typedValues.objectValue(variable))),
                  bfmgr.ifThenElse(
                      fmgr.makeEqual(type, typeTags.STRING),
                      stringToBoolean(typedValues.stringValue(variable)),
                      fmgr.makeEqual(type, typeTags.FUNCTION)))));
    }
  }

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
          booleanToNumber(typedValues.booleanValue(variable)),
          bfmgr.ifThenElse(
              fmgr.makeEqual(type, typeTags.NUMBER),
              typedValues.numberValue(variable),
              bfmgr.ifThenElse(
                  fmgr.makeEqual(type, typeTags.OBJECT),
                  fmgr.makeNumber(Types.NUMBER_TYPE, 0), // TODO handle non null objects
                  fpfmgr.makeNaN(Types.NUMBER_TYPE))));
    }
  }

  @Nonnull
  private BooleanFormula stringToBoolean(final IntegerFormula pValue) {
    return bfmgr.not(fmgr.makeEqual(pValue, strMgr.getStringFormula("")));
  }

  private BooleanFormula numberToBoolean(final FloatingPointFormula pValue) {
    return bfmgr.ifThenElse(
        bfmgr.or(fpfmgr.isZero(pValue), fpfmgr.isNaN(pValue)), bfmgr.makeFalse(), bfmgr.makeTrue());
  }

  private FloatingPointFormula booleanToNumber(final BooleanFormula pValue) {
    return bfmgr.ifThenElse(
        pValue, fmgr.makeNumber(Types.NUMBER_TYPE, 1), fmgr.makeNumber(Types.NUMBER_TYPE, 0));
  }
}
