// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

public enum SeqSyntax {
  BRACKET_LEFT("("),
  BRACKET_RIGHT(")"),
  COLON(":"),
  CURLY_BRACKET_LEFT("{"),
  CURLY_BRACKET_RIGHT("}"),
  EXCLAMATION_MARK("!"),
  NEWLINE("\n"),
  SEMICOLON(";"),
  SPACE(" "),
  SQUARE_BRACKET_LEFT("["),
  SQUARE_BRACKET_RIGHT("]");

  /** The String representation for the syntax element. */
  private final String string;

  SeqSyntax(String pString) {
    string = pString;
  }

  public String getString() {
    return string;
  }
}
