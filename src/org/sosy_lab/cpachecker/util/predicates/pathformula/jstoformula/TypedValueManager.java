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

import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Manager to create {@link TypedValue} instances. */
class TypedValueManager {
  private final FormulaManagerView fmgr;
  private final TypedValues typedValues; // TODO remove
  private final TypeTags typeTags;
  private final TypedValue undefinedValue;
  private final TypedValue nullValue;

  TypedValueManager(
      final FormulaManagerView pFmgr,
      final TypedValues pTypedValues,
      final TypeTags pTypeTags,
      final IntegerFormula pNullValueObjectId) {
    fmgr = pFmgr;
    typedValues = pTypedValues;
    typeTags = pTypeTags;
    nullValue = new TypedValue(typeTags.OBJECT, pNullValueObjectId);
    // The value of undefined is always the same.
    // It does not matter as which value it is represented.
    // Thus, the same value as the type tag can be used.
    undefinedValue = new TypedValue(typeTags.UNDEFINED, typeTags.UNDEFINED);
  }

  /**
   * @return Typed value formula encoding of JavaScript's <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.9">undefined value</a>.
   */
  TypedValue getUndefinedValue() {
    return undefinedValue;
  }

  /**
   * @return Typed value formula encoding of JavaScript's <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.11">null value</a>.
   */
  TypedValue getNullValue() {
    return nullValue;
  }

  /**
   * @param pBooleanFormula Formula encoding of a <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.13">Boolean value</a>.
   * @return Typed formula encoding of a <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.13">Boolean value</a>.
   */
  TypedValue createBooleanValue(final BooleanFormula pBooleanFormula) {
    return new TypedValue(typeTags.BOOLEAN, pBooleanFormula);
  }

  /**
   * @param pFloatingPointFormula Formula encoding of a <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.19">Number value</a>.
   * @return Typed formula encoding of a <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.19">Number value</a>.
   */
  TypedValue createNumberValue(final FloatingPointFormula pFloatingPointFormula) {
    return new TypedValue(typeTags.NUMBER, pFloatingPointFormula);
  }

  /**
   * @param pObjectId Formula encoding of object ID (see {@link
   *     ObjectIdFormulaManager#createObjectId()}).
   * @return Typed formula encoding of the passed object ID.
   */
  TypedValue createObjectValue(final IntegerFormula pObjectId) {
    return new TypedValue(typeTags.OBJECT, pObjectId);
  }

  /**
   * @param pStringId Formula encoding of string ID (see {@link
   *     StringFormulaManager#getStringFormula(String)}).
   * @return Typed formula encoding of the passed string ID.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.16">String value</a>
   */
  TypedValue createStringValue(final IntegerFormula pStringId) {
    return new TypedValue(typeTags.STRING, pStringId);
  }

  /**
   * @param pFunctionDeclarationId Formula encoding of function declaration ID (see {@link
   *     GlobalManagerContext#functionDeclarationIds}).
   * @return Typed formula encoding of the passed function declaration ID.
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.24">function</a>
   */
  TypedValue createFunctionValue(final IntegerFormula pFunctionDeclarationId) {
    return new TypedValue(typeTags.FUNCTION, pFunctionDeclarationId);
  }

  TypedValue ifThenElse(
      final BooleanFormula pBooleanFormula, final TypedValue pThen, final TypedValue pElse) {
    final BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    return new TypedValue(
        bfmgr.ifThenElse(pBooleanFormula, pThen.getType(), pElse.getType()),
        bfmgr.ifThenElse(pBooleanFormula, pThen.getValue(), pElse.getValue()));
  }
}
