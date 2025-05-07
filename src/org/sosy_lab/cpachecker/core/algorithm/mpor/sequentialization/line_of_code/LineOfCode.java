// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class LineOfCode {

  public final int tabs;

  public final String code;

  private final boolean newlinePrefix;

  private final boolean newlineSuffix;

  private LineOfCode(int pTabs, String pCode, boolean pNewlinePrefix, boolean pNewlineSuffix) {
    // pCode.isEmpty() -> pTabs == 0
    /*checkArgument(!pCode.isEmpty() || pTabs == 0, "if pCode is empty, pTabs must be 0");
    checkArgument(
        pCode.length() == pCode.trim().length(),
        "pCode cannot have leading or trailing whitespaces");*/
    tabs = pTabs;
    code = pCode;
    newlinePrefix = pNewlinePrefix;
    newlineSuffix = pNewlineSuffix;
  }

  public LineOfCode cloneWithCode(String pCode) {
    return LineOfCode.of(tabs, pCode);
  }

  public LineOfCode cloneWithoutNewline() {
    return LineOfCode.withoutNewlineSuffix(tabs, code);
  }

  public static LineOfCode of(int pTabs, String pCode) {
    return new LineOfCode(pTabs, pCode, false, true);
  }

  public static LineOfCode withoutNewlineSuffix(int pTabs, String pCode) {
    return new LineOfCode(pTabs, pCode, false, false);
  }

  public static LineOfCode withNewlinePrefix(int pTabs, String pCode) {
    return new LineOfCode(pTabs, pCode, true, true);
  }

  /** Use this constructor to create an empty {@link LineOfCode}, i.e. {@code ""} with newline. */
  public static LineOfCode empty() {
    return new LineOfCode(0, SeqSyntax.EMPTY_STRING, false, true);
  }

  @Override
  public String toString() {
    return buildNewline(newlinePrefix)
        + SeqStringUtil.buildTab(tabs)
        + code
        + buildNewline(newlineSuffix);
  }

  private String buildNewline(boolean pNewline) {
    return pNewline ? SeqSyntax.NEWLINE : SeqSyntax.EMPTY_STRING;
  }
}
