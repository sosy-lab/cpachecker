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

  private LineOfCode(String pCode) {
    code = pCode;
  }

  public static LineOfCode of(String pCode) {
    return new LineOfCode(pCode);
  }

  public LineOfCode cloneWithCode(String pCode) {
    return LineOfCode.of(pCode);
  }

  @Override
  public String toString() {
    return code + SeqSyntax.NEWLINE;
  }
}
