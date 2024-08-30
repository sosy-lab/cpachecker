// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;

public class SeqSyntax {

  public static final String BRACKET_LEFT = "(";

  public static final String BRACKET_RIGHT = ")";

  public static final String COLON = ":";

  public static final String CURLY_BRACKET_LEFT = "{";

  public static final String CURLY_BRACKET_RIGHT = "}";

  public static final String EMPTY_STRING = "";

  public static final String EXCLAMATION_MARK = "!";

  public static final String NEWLINE = "\n";

  public static final String SEMICOLON = ";";

  public static final String SPACE = " ";

  public static final String SQUARE_BRACKET_LEFT = "[";

  public static final String SQUARE_BRACKET_RIGHT = "]";

  public static final String TAB = initTab();

  private static String initTab() {
    return SeqSyntax.EMPTY_STRING + SeqSyntax.SPACE.repeat(Sequentialization.TAB_SIZE);
  }
}
