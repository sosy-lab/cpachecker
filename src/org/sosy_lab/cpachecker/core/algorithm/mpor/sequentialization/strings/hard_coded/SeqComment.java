// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded;

public class SeqComment {

  private static final String COMMENT_SINGLE = "//";

  public static final String UNCHANGED_DECLARATIONS =
      COMMENT_SINGLE + " unchanged input program declarations";

  public static final String GLOBAL_VAR_DECLARATIONS =
      COMMENT_SINGLE + " global variable substitutes";

  public static final String LOCAL_VAR_DECLARATIONS =
      COMMENT_SINGLE + " thread local variable substitutes";

  public static final String PARAMETER_VAR_SUBSTITUTES =
      COMMENT_SINGLE + " thread local parameter variables storing function arguments";

  public static final String RETURN_PCS =
      COMMENT_SINGLE + " thread local function return pc storing calling contexts";

  public static final String THREAD_SIMULATION = COMMENT_SINGLE + " (p)thread simulation variables";

  public static final String CUSTOM_FUNCTION_DECLARATIONS =
      COMMENT_SINGLE + " custom function declarations";
}
