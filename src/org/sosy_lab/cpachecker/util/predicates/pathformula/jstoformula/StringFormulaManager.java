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

import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class StringFormulaManager {

  private final Ids<String> stringIds;
  private final FormulaManagerView fmgr;
  private final int maxFieldNameCount;

  StringFormulaManager(final FormulaManagerView pFmgr, final int pMaxFieldNameCount) {
    fmgr = pFmgr;
    maxFieldNameCount = pMaxFieldNameCount;
    stringIds = new Ids<>();
  }

  IntegerFormula getStringFormula(final String pValue) {
    final int id = stringIds.get(pValue);
    if (id > maxFieldNameCount) {
      throw new RuntimeException(
          "Reached cpa.predicate.js.maxFieldNameCount of " + maxFieldNameCount);
    }
    return fmgr.makeNumber(Types.STRING_TYPE, id);
  }

  /**
   * Get valid string-IDs.
   *
   * @return Iterable of all string-IDs in ascending order.
   */
  Iterable<Integer> getIdRange() {
    return IntStream.rangeClosed(1, maxFieldNameCount)::iterator;
  }
}
