// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded;

/** The comments are sorted as they appear in the output program. */
public class SeqComment {

  private static final String COMMENT_SINGLE = "//";

  public static final String UNCHANGED_DECLARATIONS =
      COMMENT_SINGLE + " input program declarations, optionally with function declarations";

  public static final String GLOBAL_VAR_DECLARATIONS =
      COMMENT_SINGLE + " global variable substitutes";

  public static final String LOCAL_VAR_DECLARATIONS =
      COMMENT_SINGLE + " thread local variable substitutes";

  public static final String PARAMETER_VAR_SUBSTITUTES =
      COMMENT_SINGLE + " thread local parameter variables storing function arguments";

  public static final String START_ROUTINE_ARG_SUBSTITUTES =
      COMMENT_SINGLE + " start_routine argument substitutes passed via pthread_create";

  public static final String THREAD_SIMULATION_VARIABLES =
      COMMENT_SINGLE + " thread and pthread method simulation variables";

  public static final String CUSTOM_FUNCTION_DECLARATIONS =
      COMMENT_SINGLE + " custom function declarations";

  public static final String CUSTOM_FUNCTION_DEFINITIONS =
      COMMENT_SINGLE + " custom function definitions";

  public static final String PC_DECLARATION =
      COMMENT_SINGLE + " track current program locations (pc) for all thread simulations";

  public static final String NEXT_THREAD_NONDET =
      COMMENT_SINGLE + " choose and assign next_thread non-deterministically";

  public static final String NEXT_THREAD_ACTIVE =
      COMMENT_SINGLE + " ensure next_thread is yet / still active";

  public static final String THREAD_SIMULATION_ASSUMPTIONS =
      COMMENT_SINGLE + " enforce strict orders over execution of program statements such as locks";

  public static final String THREAD_SIMULATION_SWITCHES =
      COMMENT_SINGLE
          + " represent reachable statements of thread simulations in separate switch cases";
}
