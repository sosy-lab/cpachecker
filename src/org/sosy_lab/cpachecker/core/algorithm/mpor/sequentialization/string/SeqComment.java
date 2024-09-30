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

  public static String createNonVarDeclarationComment() {
    return COMMENT_SINGLE
        + SeqToken.ORIGINAL_PROGRAM
        + SeqSyntax.SPACE
        + SeqToken.DECLARATIONS
        + SeqSyntax.SPACE
        + SeqToken.NON_VARIABLE
        + SeqSyntax.NEWLINE;
  }

  public static String createFuncDeclarationComment() {
    return COMMENT_SINGLE
        + SeqToken.CUSTOM
        + SeqSyntax.SPACE
        + SeqToken.FUNCTION
        + SeqSyntax.SPACE
        + SeqToken.DECLARATIONS
        + SeqSyntax.NEWLINE;
  }

  public static String createGlobalVarsComment() {
    return COMMENT_SINGLE
        + SeqToken.GLOBAL
        + SeqSyntax.SPACE
        + SeqToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }

  public static String createLocalVarsComment(int threadId) {
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

  public static String createParamVarsComment(int threadId) {
    return COMMENT_SINGLE
        + SeqToken.THREAD
        + SeqSyntax.SPACE
        + threadId
        + SeqSyntax.SPACE
        + SeqToken.PARAMETER
        + SeqSyntax.SPACE
        + SeqToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }

  public static String createReturnPcVarsComment() {
    return COMMENT_SINGLE
        + SeqToken.THREAD
        + SeqSyntax.SPACE
        + SeqToken.LOCAL
        + SeqSyntax.SPACE
        + SeqToken.FUNCTION
        + SeqSyntax.SPACE
        + SeqToken.RETURN
        + SeqSyntax.SPACE
        + SeqToken.PC
        + SeqSyntax.NEWLINE;
  }
}
