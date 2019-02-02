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
import static org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula.Types.VARIABLE_TYPE;

import java.math.BigDecimal;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

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
  /**
   * Offset that is used to calculate string IDs of strings that are not related to a string
   * representation of a ECMAScript number.
   *
   * <p>All string representations of ECMAScript numbers (except NaN and Infinity) are mapped to the
   * string ID that is the same floating point number.
   * All other string IDs are mapped to a greater floating point numbers than the maximum ECMAScript
   * number, which is 2e1024.
   */
  private static final BigDecimal nonNumberStringIdOffset = BigDecimal.TEN.pow(309); // TODO update comment

  private final Ids<String> stringIds;
  private final FormulaManagerView fmgr;
  private final FloatingPointFormulaManagerView fpfmgr;
  private final int maxFieldNameCount;
  private final FunctionFormulaManagerView ffmgr;
  private final FunctionDeclaration<FloatingPointFormula> concatStringsUF;
  private final FunctionDeclaration<FloatingPointFormula> unknownStringUF;

  /**
   * @param pFmgr Used to make string ID formulas.
   * @param pMaxFieldNameCount The limit of strings that may be used in the program (see {@link
   *     JSFormulaEncodingOptions#maxFieldNameCount}).
   */
  StringFormulaManager(final FormulaManagerView pFmgr, final int pMaxFieldNameCount) {
    fmgr = pFmgr;
    fpfmgr = fmgr.getFloatingPointFormulaManager();
    ffmgr = fmgr.getFunctionFormulaManager();
    concatStringsUF = ffmgr.declareUF("concatStrings", STRING_TYPE, STRING_TYPE, STRING_TYPE);
    unknownStringUF = ffmgr.declareUF("unknownString", STRING_TYPE, VARIABLE_TYPE);
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
  FloatingPointFormula getStringFormula(final String pValue) {
    // TODO check if pValue is a string representation of a ECMAScript number
    final int id = stringIds.get(pValue);
    if (id > maxFieldNameCount) {
      throw new RuntimeException(
          "Reached cpa.predicate.js.maxFieldNameCount of " + maxFieldNameCount);
    }
    return getNonNumberStringIdFormula(id);
  }

  @Nonnull
  private FloatingPointFormula getNonNumberStringIdFormula(final int pId) {
    // TODO describe index calculation
    final BigDecimal id = nonNumberStringIdOffset.multiply(BigDecimal.valueOf(1 + 0.0001 * pId));
    return fpfmgr.makeNumber(id, Types.STRING_TYPE);
  }

  /**
   * Get all string-IDs that might be knowingly used in the analyzed program. String literals and
   * property names result in known string-IDs, whereas calculated string values and hence there IDs
   * are unknown.
   *
   * @return Iterable of all string-IDs in ascending order.
   */
  Iterable<FloatingPointFormula> getIdRange() {
    return IntStream.rangeClosed(1, maxFieldNameCount).mapToObj(this::getNonNumberStringIdFormula)
        ::iterator;
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
  FloatingPointFormula concat(
      final FloatingPointFormula pLeftStringId, final FloatingPointFormula pRightStringId) {
    return ffmgr.callUF(concatStringsUF, pLeftStringId, pRightStringId);
  }

  /**
   * String IDs of unknown strings (for example the string representation of an object) is supported
   * as uninterpreted function.
   *
   * @param pUnknownValueId Identifier that is associated with the (unknown) string (for example a
   *     variable or object ID.
   * @return String ID
   */
  FloatingPointFormula unknownString(final IntegerFormula pUnknownValueId) {
    return ffmgr.callUF(unknownStringUF, pUnknownValueId);
  }
}
