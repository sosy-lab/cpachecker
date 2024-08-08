// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

public enum SeqElement {
  ASSUME("assume"),
  NEXT_THREAD("nextThread"),
  NON_DET("nondet()"),
  NUM_THREADS("numThreads"),
  WHILE("while");

  /** The String representation for the sequentialization element type. */
  public final String string;

  SeqElement(String pString) {
    string = pString;
  }
}
