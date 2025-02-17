// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqSyntax;

public class LineOfCode {

  public final int tabs;

  public final String code;

  private LineOfCode(int pTabs, String pCode) {
    // pCode.isEmpty() -> pTabs == 0
    checkArgument(!pCode.isEmpty() || pTabs == 0, "if pCode is empty, pTabs must be 0");
    checkArgument(
        pCode.length() == pCode.trim().length(),
        "pCode cannot have leading or trailing whitespaces");
    tabs = pTabs;
    code = pCode;
  }

  public LineOfCode copyWithCode(String pCode) {
    return LineOfCode.of(tabs, pCode);
  }

  public static LineOfCode of(int pTabs, String pCode) {
    return new LineOfCode(pTabs, pCode);
  }

  /** Use this constructor to create an empty {@link LineOfCode}, i.e. {@code ""}. */
  public static LineOfCode empty() {
    return new LineOfCode(0, SeqSyntax.EMPTY_STRING);
  }

  @Override
  public String toString() {
    return SeqStringUtil.buildTab(tabs) + code + SeqSyntax.NEWLINE;
  }
}
