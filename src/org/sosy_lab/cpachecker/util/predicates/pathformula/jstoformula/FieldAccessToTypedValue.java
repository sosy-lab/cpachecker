/*
 *  CPAchecker is a tool for configurable software verification.
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

import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class FieldAccessToTypedValue {
  private final JSToFormulaConverter conv;
  private final SSAMapBuilder ssa;

  /**
   * The <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.6.2">internal property
   * [[Prototype]]</a> is represented as regular field called <a
   * href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto">__proto__</a>
   * which is still supported by all common JavaScript engines (<a
   * href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/proto#Browser_compatibility">as
   * of 4. September 2018</a>).
   */
  private final IntegerFormula prototypeField;

  FieldAccessToTypedValue(final JSToFormulaConverter pConv, final SSAMapBuilder pSsa) {
    conv = pConv;
    ssa = pSsa;
    prototypeField = conv.getStringFormula("__proto__");
  }

  /**
   * Look up field on the prototype chain (recursively) of an object.
   *
   * @param prototypeChainDepth The depth of the current prototype in the prototype chain.
   * @param pPrototypeField The prototype field (internal property) of the object.
   * @param pFieldName The field name (formula of string constant) to look up.
   * @return The typed value of the field on the prototype chain.
   */
  private TypedValue lookUpOnPrototypeChain(
      final int prototypeChainDepth,
      final IntegerFormula pPrototypeField,
      final IntegerFormula pFieldName) {
    if (prototypeChainDepth > conv.maxPrototypeChainLength) {
      return conv.tvmgr.getUndefinedValue();
    }
    final IntegerFormula prototypeObjectId = conv.typedValues.objectValue(pPrototypeField);
    final ArrayFormula<IntegerFormula, IntegerFormula> prototypeFields =
        conv.getObjectFields(prototypeObjectId, ssa);
    final IntegerFormula fieldOnPrototype = conv.afmgr.select(prototypeFields, pFieldName);
    final BooleanFormula isFieldOnPrototypeNotSet =
        conv.bfmgr.or(
            conv.fmgr.makeEqual(pPrototypeField, conv.objectFieldNotSet),
            conv.fmgr.makeEqual(fieldOnPrototype, conv.objectFieldNotSet));
    final TypedValue parentPrototype =
        lookUpOnPrototypeChain(
            prototypeChainDepth + 1,
            conv.afmgr.select(prototypeFields, prototypeField),
            pFieldName);
    return new TypedValue(
        conv.bfmgr.ifThenElse(
            isFieldOnPrototypeNotSet,
            parentPrototype.getType(),
            conv.typedValues.typeof(fieldOnPrototype)),
        conv.bfmgr.ifThenElse(
            isFieldOnPrototypeNotSet, parentPrototype.getValue(), fieldOnPrototype));
  }

  /**
   * Get typed value of a field on an object.
   *
   * @param pObjectId The object (value) to look up the field.
   * @param pFieldName The field name (formula of string constant) to look up.
   * @return Typed value of the field on the object or the field inherited by the prototype chain of
   *     the object.
   */
  TypedValue accessField(final IntegerFormula pObjectId, final IntegerFormula pFieldName) {
    final ArrayFormula<IntegerFormula, IntegerFormula> fields =
        conv.getObjectFields(pObjectId, ssa);
    final IntegerFormula field = conv.afmgr.select(fields, pFieldName);
    final BooleanFormula isObjectFieldNotSet = conv.fmgr.makeEqual(field, conv.objectFieldNotSet);
    final TypedValue typedValueOnPrototypeChain =
        lookUpOnPrototypeChain(1, conv.afmgr.select(fields, prototypeField), pFieldName);
    return new TypedValue(
        conv.bfmgr.ifThenElse(
            isObjectFieldNotSet,
            typedValueOnPrototypeChain.getType(),
            conv.typedValues.typeof(field)),
        conv.bfmgr.ifThenElse(isObjectFieldNotSet, typedValueOnPrototypeChain.getValue(), field));
  }
}
