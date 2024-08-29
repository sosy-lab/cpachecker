// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

// TODO split into C syntax and custom names?
public enum SeqToken {
  ASSUME("assume"),
  BREAK("break"),
  CASE("case"),
  EXECUTED("executed"),
  NEXT_THREAD("nextThread"),
  NON_DET("nondet()"),
  NUM_THREADS("numThreads"),
  SWITCH("switch"),
  WHILE("while");

  /** The String representation for the sequentialization token. */
  private final String string;

  SeqToken(String pString) {
    string = pString;
  }

  public String getString() {
    return string;
  }
}
