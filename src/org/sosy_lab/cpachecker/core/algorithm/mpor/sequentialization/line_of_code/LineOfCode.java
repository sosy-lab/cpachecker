// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class LineOfCode {

  public final int tabs;

  public final String code;

  private LineOfCode(int pTabs, String pCode) {
    // pCode == "" -> pTabs == 0
    checkArgument(
        !pCode.equals(SeqSyntax.EMPTY_STRING) || pTabs == 0, "if pCode is empty, pTabs must be 0");
    tabs = pTabs;
    code = pCode;
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
    return SeqUtil.buildTab(tabs)
        + code
        + SeqSyntax.NEWLINE;
  }

  public static String toString(ImmutableList<LineOfCode> pLinesOfCode) {
    StringBuilder rString = new StringBuilder();
    for (LineOfCode lineOfCode : pLinesOfCode) {
      rString.append(lineOfCode.toString());
    }
    return rString.toString();
  }
}
