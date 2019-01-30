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

import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.STRING_TYPE;

import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.RationalFormulaManagerView;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;

/**
 * Management of formula encoding of <a
 * href="https://www.ecma-international.org/ecma-262/5.1/#sec-4.3.16">String values</a>.
 *
 * <p>String values are immutable values in JavaScript. Each value is mapped to an Id.
 *
 * <p>Strings are used to encode properties of JavaScript objects (see {@link
 * ObjectFormulaManager}). That encoding requires the IDs of all strings that might be used as a
 * property name in the program when the first object has to be formula encoded. Since this can be
 * anytime, the IDs of all property names (i.e. strings) have to be known anytime. Therefore, the
 * amount of strings is limited. Thus, a specific range of integers can be used as IDs (see {@link
 * #getIdRange()}). The limit is provided as an option (see {@link
 * JSFormulaEncodingOptions#maxFieldNameCount}). If it is detected that the limit is exceeded then a
 * exception is thrown that cancels the analysis of the program. The limit option has to be
 * increased to analyze the program.
 */
class StringFormulaManager {

  private final Ids<String> stringIds;
  private final FormulaManagerView fmgr;
  private final RationalFormulaManagerView rfmgr;
  private final int maxFieldNameCount;
  private final FunctionFormulaManagerView ffmgr;
  private final FunctionDeclaration<RationalFormula> concatStringsUF;

  /**
   * @param pFmgr Used to make string ID formulas.
   * @param pMaxFieldNameCount The limit of strings that may be used in the program (see {@link
   *     JSFormulaEncodingOptions#maxFieldNameCount}).
   */
  StringFormulaManager(final FormulaManagerView pFmgr, final int pMaxFieldNameCount) {
    fmgr = pFmgr;
    rfmgr = fmgr.getRationalFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    concatStringsUF = ffmgr.declareUF("concatStrings", STRING_TYPE, STRING_TYPE, STRING_TYPE);
    maxFieldNameCount = pMaxFieldNameCount;
    stringIds = new Ids<>();
  }

  /**
   * Get the string ID formula of a specific string value. If the value has not been associated with
   * an ID yet then a new ID is created for it. This might lead to the limit (of strings being used
   * in the analyzed program) being exceeded, which results in termination of the analysis.
   *
   * @param pValue The string value whose string ID formula should be returned.
   * @return The string ID of the passed string value.
   */
  RationalFormula getStringFormula(final String pValue) {
    final int id = stringIds.get(pValue);
    if (id > maxFieldNameCount) {
      throw new RuntimeException(
          "Reached cpa.predicate.js.maxFieldNameCount of " + maxFieldNameCount);
    }
    return fmgr.makeNumber(STRING_TYPE, id);
  }

  /**
   * Get all string-IDs that might be knowingly used in the analyzed program. String literals and
   * property names result in known string-IDs, whereas calculated string values and hence there IDs
   * are unknown.
   *
   * @return Iterable of all string-IDs in ascending order.
   */
  Iterable<RationalFormula> getIdRange() {
    return IntStream.rangeClosed(1, maxFieldNameCount).mapToObj(rfmgr::makeNumber)::iterator;
  }

  /**
   * Concatenation of two string IDs is only supported as uninterpreted function.
   *
   * @param pLeftStringId Left operand
   * @param pRightStringId Right operand
   * @return String ID
   * @see <a href="https://www.ecma-international.org/ecma-262/5.1/#sec-11.6.1">The Addition
   *     operator ( + )</a>
   */
  RationalFormula concat(
      final RationalFormula pLeftStringId, final RationalFormula pRightStringId) {
    return ffmgr.callUF(concatStringsUF, pLeftStringId, pRightStringId);
  }
}
