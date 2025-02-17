// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Splitter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;

public class SeqStringUtil {

  /** The amount of spaces in a tab, adjust as desired. */
  public static final int TAB_SIZE = 2;

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  /** Returns ""pString"" */
  public static String wrapInQuotationMarks(String pString) {
    return SeqSyntax.QUOTATION_MARK + pString + SeqSyntax.QUOTATION_MARK;
  }

  /** Returns "{ pString }" */
  public static String wrapInCurlyInwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_LEFT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns "} pString {" */
  public static String wrapInCurlyOutwards(String pString) {
    return SeqSyntax.CURLY_BRACKET_RIGHT
        + SeqSyntax.SPACE
        + pString
        + SeqSyntax.SPACE
        + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString {" */
  public static String appendOpeningCurly(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_LEFT;
  }

  /** Returns "pString }" */
  public static String appendClosingCurly(String pString) {
    return pString + SeqSyntax.SPACE + SeqSyntax.CURLY_BRACKET_RIGHT;
  }

  /** Returns pString with the specified amount of tabs as prefix and a new line \n as suffix. */
  public static String prependTabsWithNewline(int pTabs, String pString) {
    return prependTabsWithoutNewline(pTabs, pString) + SeqSyntax.NEWLINE;
  }

  /** Returns pString with the specified amount of tabs as prefix. */
  public static String prependTabsWithoutNewline(int pTabs, String pString) {
    return buildTab(pTabs) + pString;
  }

  public static String buildTab(int pTabs) {
    return repeat(SeqSyntax.SPACE, pTabs * TAB_SIZE);
  }

  public static String repeat(String pString, int pAmount) {
    return pString.repeat(Math.max(0, pAmount));
  }

  public static Iterable<String> splitOnNewline(String pString) {
    return newlineSplitter.split(pString);
  }

  /** Returns the number of lines, i.e. the amount of \n + 1 in pString. */
  public static int countLines(String pString) {
    if (isNullOrEmpty(pString)) {
      return 0;
    }
    return newlineSplitter.splitToList(pString).size();
  }
}
