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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.OBJECT_ID_TYPE;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Management of object IDs, whereas the <a
 * href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.11">null value</a> is represented
 * by a particular object ID (see {@link ObjectIdFormulaManager#getNullObjectId()})..
 */
class ObjectIdFormulaManager {

  private final FormulaManagerView fmgr;

  private long nextObjectId;

  private final IntegerFormula nullObjectId;

  ObjectIdFormulaManager(final FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    nullObjectId = pFmgr.getIntegerFormulaManager().makeNumber(0);
    nextObjectId = 1; // Regular object IDs start at ID 1
  }

  /**
   * Create a new unique object ID.
   *
   * @return The created object ID formula.
   */
  IntegerFormula createObjectId() {
    return fmgr.makeNumber(OBJECT_ID_TYPE, nextObjectId++);
  }

  /**
   * @return The object ID formula that represents the <a
   *     href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.11">null value</a>
   */
  IntegerFormula getNullObjectId() {
    return nullObjectId;
  }
}
