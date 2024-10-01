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
        + SeqCommentToken.ORIGINAL_PROGRAM
        + SeqSyntax.SPACE
        + SeqCommentToken.DECLARATIONS
        + SeqSyntax.SPACE
        + SeqCommentToken.NON_VARIABLE
        + SeqSyntax.NEWLINE;
  }

  public static String createFuncDeclarationComment() {
    return COMMENT_SINGLE
        + SeqCommentToken.CUSTOM
        + SeqSyntax.SPACE
        + SeqToken.FUNCTION
        + SeqSyntax.SPACE
        + SeqCommentToken.DECLARATIONS
        + SeqSyntax.NEWLINE;
  }

  public static String createGlobalVarsComment() {
    return COMMENT_SINGLE
        + SeqCommentToken.GLOBAL
        + SeqSyntax.SPACE
        + SeqCommentToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }

  public static String createLocalVarsComment(int threadId) {
    return COMMENT_SINGLE
        + SeqCommentToken.THREAD
        + SeqSyntax.SPACE
        + threadId
        + SeqSyntax.SPACE
        + SeqCommentToken.LOCAL
        + SeqSyntax.SPACE
        + SeqCommentToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }

  public static String createParamVarsComment(int threadId) {
    return COMMENT_SINGLE
        + SeqCommentToken.THREAD
        + SeqSyntax.SPACE
        + threadId
        + SeqSyntax.SPACE
        + SeqCommentToken.PARAMETER
        + SeqSyntax.SPACE
        + SeqCommentToken.VARIABLES
        + SeqSyntax.NEWLINE;
  }

  public static String createReturnPcVarsComment() {
    return COMMENT_SINGLE
        + SeqCommentToken.THREAD
        + SeqSyntax.SPACE
        + SeqCommentToken.LOCAL
        + SeqSyntax.SPACE
        + SeqToken.FUNCTION
        + SeqSyntax.SPACE
        + SeqToken.RETURN
        + SeqSyntax.SPACE
        + SeqToken.PC
        + SeqSyntax.NEWLINE;
  }
}
