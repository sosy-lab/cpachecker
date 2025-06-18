// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class LineOfCode {

  public final String code;

  private final boolean newlinePrefix;

  private final boolean newlineSuffix;

  private LineOfCode(String pCode, boolean pNewlinePrefix, boolean pNewlineSuffix) {
    code = pCode;
    newlinePrefix = pNewlinePrefix;
    newlineSuffix = pNewlineSuffix;
  }

  public LineOfCode cloneWithCode(String pCode) {
    return LineOfCode.of(pCode);
  }

  public LineOfCode cloneWithoutNewline() {
    return LineOfCode.withoutNewlineSuffix(code);
  }

  public static LineOfCode of(String pCode) {
    return new LineOfCode(pCode, false, true);
  }

  public static LineOfCode withoutNewlineSuffix(String pCode) {
    return new LineOfCode(pCode, false, false);
  }

  public static LineOfCode withNewlinePrefix(String pCode) {
    return new LineOfCode(pCode, true, true);
  }

  /** Use this constructor to create an empty {@link LineOfCode}, i.e. {@code ""} with newline. */
  public static LineOfCode empty() {
    return new LineOfCode(SeqSyntax.EMPTY_STRING, false, true);
  }

  @Override
  public String toString() {
    return buildNewline(newlinePrefix) + code + buildNewline(newlineSuffix);
  }

  private String buildNewline(boolean pNewline) {
    return pNewline ? SeqSyntax.NEWLINE : SeqSyntax.EMPTY_STRING;
  }
}
