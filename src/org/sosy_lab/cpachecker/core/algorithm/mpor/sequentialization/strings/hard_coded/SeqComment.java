// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded;

import org.sosy_lab.cpachecker.util.cwriter.export.CCommentStatement;

/** The comments are sorted as they appear in the output program. */
public class SeqComment {

  public static final String COMMENT_BLOCK_BEGIN = "/*";

  public static final String COMMENT_BLOCK_END = "*/";

  public static final CCommentStatement UNCHANGED_DECLARATIONS =
      new CCommentStatement("input program declarations, optionally with function declarations");

  public static final CCommentStatement GLOBAL_VAR_DECLARATIONS =
      new CCommentStatement("global variable substitutes");

  public static final CCommentStatement LOCAL_VAR_DECLARATIONS =
      new CCommentStatement("thread local variable substitutes");

  public static final CCommentStatement PARAMETER_VAR_SUBSTITUTES =
      new CCommentStatement("thread local parameter variables storing function arguments");

  public static final CCommentStatement MAIN_FUNCTION_ARG_SUBSTITUTES =
      new CCommentStatement("non-deterministic main function argument substitutes");

  public static final CCommentStatement START_ROUTINE_ARG_SUBSTITUTES =
      new CCommentStatement("start_routine argument substitutes passed via pthread_create");

  public static final CCommentStatement START_ROUTINE_EXIT_VARIABLES =
      new CCommentStatement("return values of start_routines passed via pthread_exit");

  public static final CCommentStatement THREAD_SIMULATION_VARIABLES =
      new CCommentStatement("thread and pthread method simulation variables");

  public static final CCommentStatement CUSTOM_FUNCTION_DECLARATIONS =
      new CCommentStatement("custom function declarations");

  public static final CCommentStatement CUSTOM_FUNCTION_DEFINITIONS =
      new CCommentStatement("custom function definitions");

  public static final CCommentStatement PC_DECLARATION =
      new CCommentStatement("track current program locations (pc) for all thread simulations");

  public static final CCommentStatement NEXT_THREAD_NONDET =
      new CCommentStatement("choose and assign next_thread non-deterministically");

  public static final CCommentStatement NEXT_THREAD_ACTIVE =
      new CCommentStatement("ensure next_thread is yet / still active");

  public static final CCommentStatement ACTIVE_THREAD_COUNT =
      new CCommentStatement(
          "counts active threads. incremented for every thread creation, decremented for every"
              + " thread termination");

  public static final CCommentStatement THREAD_SIMULATION_CONTROL_FLOW =
      new CCommentStatement(
          "represent reachable statements of thread simulations in separate control flow"
              + " statements");
}
