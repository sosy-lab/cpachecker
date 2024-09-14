// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string;

public class SeqComment {

  private static final String COMMENT_SINGLE = "// ";

  public static String createGlobalVarsComment() {
    return COMMENT_SINGLE
        + SeqToken.GLOBAL
        + SeqSyntax.SPACE
        + SeqToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }

  public static String createThreadVarsComment(int threadId) {
    return COMMENT_SINGLE
        + SeqToken.THREAD
        + SeqSyntax.SPACE
        + threadId
        + SeqSyntax.SPACE
        + SeqToken.LOCAL
        + SeqSyntax.SPACE
        + SeqToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }
}
