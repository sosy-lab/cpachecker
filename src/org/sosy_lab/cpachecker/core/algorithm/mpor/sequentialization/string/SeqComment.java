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

  public static final String PARAM_DEC =
      COMMENT_SINGLE
          + "parameter ("
          + SeqToken.PREFIX_PARAMETER
          + ") variable declarations"
          + SeqSyntax.NEWLINE;

  public static final String VAR_DEC =
      COMMENT_SINGLE
          + "global ("
          + SeqToken.PREFIX_GLOBAL
          + ") and thread local ("
          + SeqToken.PREFIX_THREAD
          + ") variable declarations"
          + SeqSyntax.NEWLINE;
}
