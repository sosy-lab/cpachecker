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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.BOOLEAN_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.FUNCTION_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.JS_TYPE_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.NUMBER_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.OBJECT_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.STRING_TYPE;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.VARIABLE_TYPE;

import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Provider of uninterpreted function formulas that associate a JavaScript variable with its value,
 * type and scope object (formula encoding).
 */
class TypedVariableValues {
  private final FunctionDeclaration<IntegerFormula> typeofDeclaration;
  private final FunctionFormulaManagerView ffmgr;
  private final FunctionDeclaration<BooleanFormula> booleanValueDeclaration;
  private final FunctionDeclaration<FloatingPointFormula> numberValueDeclaration;
  private final FunctionDeclaration<IntegerFormula> functionValueDeclaration;
  private final FunctionDeclaration<IntegerFormula> stringValueDeclaration;
  private final FunctionDeclaration<IntegerFormula> objectValueDeclaration;

  TypedVariableValues(final FunctionFormulaManagerView pFfmgr) {
    ffmgr = pFfmgr;
    typeofDeclaration = pFfmgr.declareUF("typeof", JS_TYPE_TYPE, VARIABLE_TYPE);
    booleanValueDeclaration = pFfmgr.declareUF("booleanValue", BOOLEAN_TYPE, VARIABLE_TYPE);
    numberValueDeclaration = pFfmgr.declareUF("numberValue", NUMBER_TYPE, VARIABLE_TYPE);
    functionValueDeclaration = pFfmgr.declareUF("functionValue", FUNCTION_TYPE, VARIABLE_TYPE);
    objectValueDeclaration = pFfmgr.declareUF("objectValue", OBJECT_TYPE, VARIABLE_TYPE);
    stringValueDeclaration = pFfmgr.declareUF("stringValue", STRING_TYPE, VARIABLE_TYPE);
  }

  /**
   * @param pVariable Formula of (scoped) variable.
   * @return Type tag formula associated with the passed variable.
   * @see TypeTags
   */
  IntegerFormula typeof(final IntegerFormula pVariable) {
    return ffmgr.callUF(typeofDeclaration, pVariable);
  }

  /**
   * @param pVariable Formula of (scoped) variable.
   * @return Boolean value associated with the passed variable.
   * @see TypeTags#BOOLEAN
   * @see TypedValueManager#createBooleanValue(BooleanFormula)
   */
  BooleanFormula booleanValue(final IntegerFormula pVariable) {
    return ffmgr.callUF(booleanValueDeclaration, pVariable);
  }

  /**
   * @param pVariable Formula of (scoped) variable.
   * @return Number value associated with the passed variable.
   * @see TypeTags#NUMBER
   * @see TypedValueManager#createNumberValue(FloatingPointFormula)
   */
  FloatingPointFormula numberValue(final IntegerFormula pVariable) {
    return ffmgr.callUF(numberValueDeclaration, pVariable);
  }

  /**
   * @param pVariable Formula of (scoped) variable.
   * @return Function value associated with the passed variable.
   * @see TypeTags#FUNCTION
   * @see TypedValueManager#createFunctionValue(IntegerFormula)
   */
  IntegerFormula functionValue(final IntegerFormula pVariable) {
    return ffmgr.callUF(functionValueDeclaration, pVariable);
  }

  /**
   * @param pVariable Formula of (scoped) variable.
   * @return Object value associated with the passed variable.
   * @see TypeTags#OBJECT
   * @see TypedValueManager#createObjectValue(IntegerFormula)
   */
  IntegerFormula objectValue(final IntegerFormula pVariable) {
    return ffmgr.callUF(objectValueDeclaration, pVariable);
  }

  /**
   * @param pVariable Formula of (scoped) variable.
   * @return String value associated with the passed variable.
   * @see TypeTags#STRING
   * @see TypedValueManager#createStringValue(IntegerFormula)
   */
  IntegerFormula stringValue(final IntegerFormula pVariable) {
    return ffmgr.callUF(stringValueDeclaration, pVariable);
  }

}
