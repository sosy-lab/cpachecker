// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code;

import com.google.common.collect.ImmutableList;

public class LineOfCodeUtil {

  /** Create and return the {@link String} for {@code pLinesOfCode}. */
  public static String buildString(ImmutableList<LineOfCode> pLinesOfCode) {
    StringBuilder rString = new StringBuilder();
    for (LineOfCode lineOfCode : pLinesOfCode) {
      rString.append(lineOfCode.toString());
    }
    return rString.toString();
  }
}
