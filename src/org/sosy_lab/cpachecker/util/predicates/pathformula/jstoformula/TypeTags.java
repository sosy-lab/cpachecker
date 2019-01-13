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

import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Formula encoding of JavaScript types as returned by <a
 * href="https://www.ecma-international.org/ecma-262/5.1/#sec-11.4.3">typeof</a>.
 *
 * <p>JavaScript has no static type system. Values may be of mixed type. For example a value could
 * be a number or a boolean depending on conditions. Hence, the type of a value has to be formula
 * encoded, too.
 *
 * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-11.4.3">typeof</a>
 * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8">Types</a>
 */
class TypeTags {
  /**
   * Formula encoding of <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.3">The
   * Boolean Type</a>.
   */
  final IntegerFormula BOOLEAN;
  /**
   * Formula encoding of <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.24">The
   * Function Type</a>.
   */
  final IntegerFormula FUNCTION;
  /**
   * Formula encoding of <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.5">The
   * Number Type</a>.
   */
  final IntegerFormula NUMBER;
  /**
   * Formula encoding of <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.6">The
   * Object Type</a> and <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.2">The Null
   * Type</a>.
   *
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-11.4.3">typeof</a>
   */
  final IntegerFormula OBJECT;
  /**
   * Formula encoding of <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.4">The
   * String Type</a>.
   */
  final IntegerFormula STRING;
  /**
   * Formula encoding of <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-8.1">The
   * Undefined Type</a>.
   */
  final IntegerFormula UNDEFINED;

  TypeTags(final IntegerFormulaManagerView pFmgr) {
    BOOLEAN = pFmgr.makeNumber(0);
    FUNCTION = pFmgr.makeNumber(1);
    NUMBER = pFmgr.makeNumber(2);
    OBJECT = pFmgr.makeNumber(3);
    STRING = pFmgr.makeNumber(4);
    UNDEFINED = pFmgr.makeNumber(5);
  }
}
