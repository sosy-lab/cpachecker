/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import javax.annotation.Nonnull;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class TypedValueManager {
  private final TypedValues typedValues;
  private final TypeTags typeTags;
  private final TypedValue undefinedValue;
  private final TypedValue nullValue;

  TypedValueManager(
      final TypedValues pTypedValues,
      final TypeTags pTypeTags,
      final IntegerFormula pNullValueObjectId) {
    typedValues = pTypedValues;
    typeTags = pTypeTags;
    nullValue = new TypedValue(typeTags.OBJECT, pNullValueObjectId);
    undefinedValue = new UndefinedValue();
  }

  TypedValue getUndefinedValue() {
    return undefinedValue;
  }

  TypedValue getNullValue() {
    return nullValue;
  }

  TypedValue createBooleanValue(final BooleanFormula pBooleanFormula) {
    return new TypedValue(typeTags.BOOLEAN, pBooleanFormula);
  }

  TypedValue createNumberValue(final FloatingPointFormula pFloatingPointFormula) {
    return new TypedValue(typeTags.NUMBER, pFloatingPointFormula);
  }

  TypedValue createObjectValue(final IntegerFormula pObjectId) {
    return new TypedValue(typeTags.OBJECT, pObjectId);
  }

  TypedValue createStringValue(final IntegerFormula pStringId) {
    return new TypedValue(typeTags.STRING, pStringId);
  }

  TypedValue createFunctionValue(final IntegerFormula pFunctionDeclarationId) {
    return new TypedValue(typeTags.FUNCTION, pFunctionDeclarationId);
  }

  private class UndefinedValue extends TypedValue {

    UndefinedValue() {
      super(typeTags.UNDEFINED, typeTags.UNDEFINED);
    }

    @Nonnull
    @Override
    public Formula getValue() {
      throw new RuntimeException(
          "Can not get value of 'undefined'. Check for type 'undefined' instead.");
    }
  }

}
