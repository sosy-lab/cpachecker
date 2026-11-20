// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.exchange;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.util.CParserUtils;

/**
 * Utilities for parsing the previous values \at(e, AnyPrev) in the transition invariants of
 * witnesses. No parser of CPAchecker supports \at with a label, so they are found in the text of
 * the invariant, and only their expressions e are parsed with the C parser.
 */
final class PreviousValuesParserUtils {

  /** The label of \at(e, AnyPrev), i.e., the value of e in any previous state. */
  private static final String ANY_PREV_LABEL = "AnyPrev";

  /** An occurrence of \at(e, AnyPrev) in an invariant, from start until end (exclusive). */
  private record PreviousValue(int start, int end, String expression) {}

  private PreviousValuesParserUtils() {}

  /**
   * Returns the expressions e of the previous values \at(e, AnyPrev) in the given invariant, in the
   * order of their occurrences.
   */
  static ImmutableList<String> getExpressionsWithPreviousValues(String pInvariant) {
    return transformedImmutableListCopy(findPreviousValues(pInvariant), PreviousValue::expression);
  }

  /**
   * Returns the given invariant, where each previous value \at(e, AnyPrev) whose expression e is a
   * key of the given map is replaced by the value, e.g., by the name of a variable. The other
   * previous values stay in the invariant, which then cannot be parsed.
   */
  static String replacePreviousValues(String pInvariant, Map<String, String> pReplacements) {
    StringBuilder result = new StringBuilder();
    int replacedUntil = 0;
    for (PreviousValue previousValue : findPreviousValues(pInvariant)) {
      String replacement = pReplacements.get(previousValue.expression());
      if (replacement != null) {
        result.append(pInvariant, replacedUntil, previousValue.start()).append(replacement);
        replacedUntil = previousValue.end();
      }
    }
    return result.append(pInvariant.substring(replacedUntil)).toString();
  }

  /**
   * Parses the expression of a previous value in the given scope. It is empty if the expression
   * cannot be parsed in the scope, e.g., because a variable is not in scope.
   */
  static Optional<CExpression> parseExpression(String pExpression, CParser pParser, Scope pScope)
      throws InterruptedException {
    try {
      return CParserUtils.parseSingleStatement(pExpression, pParser, pScope)
              instanceof CExpressionStatement statement
          ? Optional.of(statement.getExpression())
          : Optional.empty();
    } catch (InvalidAutomatonException e) {
      return Optional.empty();
    }
  }

  private static ImmutableList<PreviousValue> findPreviousValues(String pInvariant) {
    ImmutableList.Builder<PreviousValue> previousValues = ImmutableList.builder();
    int start = pInvariant.indexOf(TransitionInvariantUtils.AT_PREFIX);
    while (start >= 0) {
      // The expression may contain parentheses, e.g., \at((*p), AnyPrev)
      int opening = start + TransitionInvariantUtils.AT_PREFIX.length() - 1;
      int closing = indexOfClosingParenthesis(pInvariant, opening);
      if (closing < 0) {
        break;
      }
      int labelStart = pInvariant.lastIndexOf(',', closing) + 1;
      if (labelStart > opening + 1
          && pInvariant.substring(labelStart, closing).strip().equals(ANY_PREV_LABEL)) {
        previousValues.add(
            new PreviousValue(
                start, closing + 1, pInvariant.substring(opening + 1, labelStart - 1)));
      }
      start = pInvariant.indexOf(TransitionInvariantUtils.AT_PREFIX, closing + 1);
    }
    return previousValues.build();
  }

  /** Returns the index of the parenthesis closing the one at the given index, or -1. */
  private static int indexOfClosingParenthesis(String pString, int pOpening) {
    int depth = 0;
    for (int i = pOpening; i < pString.length(); i++) {
      if (pString.charAt(i) == '(') {
        depth++;
      } else if (pString.charAt(i) == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }
}
