// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings;

import org.sosy_lab.cpachecker.util.cwriter.export.statement.CComment;

/** The comments are sorted as they appear in the output program. */
public class SeqComment {

  public static final String COMMENT_BLOCK_BEGIN = "/*";

  public static final String COMMENT_BLOCK_END = "*/";

  public static final CComment UNCHANGED_DECLARATIONS =
      new CComment("input program declarations, optionally with function declarations");

  public static final CComment GLOBAL_VAR_DECLARATIONS =
      new CComment("global variable substitutes");

  public static final CComment LOCAL_VAR_DECLARATIONS =
      new CComment("thread local variable substitutes");

  public static final CComment PARAMETER_VAR_SUBSTITUTES =
      new CComment("thread local parameter variables storing function arguments");

  public static final CComment MAIN_FUNCTION_ARG_SUBSTITUTES =
      new CComment("non-deterministic main function argument substitutes");

  public static final CComment START_ROUTINE_ARG_SUBSTITUTES =
      new CComment("start_routine argument substitutes passed via pthread_create");

  public static final CComment START_ROUTINE_EXIT_VARIABLES =
      new CComment("return values of start_routines passed via pthread_exit");

  public static final CComment THREAD_SIMULATION_VARIABLES =
      new CComment("thread and pthread method simulation variables");

  public static final CComment CUSTOM_FUNCTION_DECLARATIONS =
      new CComment("custom function declarations");

  public static final CComment CUSTOM_FUNCTION_DEFINITIONS =
      new CComment("custom function definitions");

  public static final CComment PC_DECLARATION =
      new CComment("track current program locations (pc) for all thread simulations");

  public static final CComment NEXT_THREAD_NONDET =
      new CComment("choose and assign next_thread non-deterministically");

  public static final CComment NEXT_THREAD_ACTIVE =
      new CComment("ensure next_thread is yet / still active");

  public static final CComment ACTIVE_THREAD_COUNT =
      new CComment(
          "counts active threads. incremented for every thread creation, decremented for every"
              + " thread termination");

  public static final CComment THREAD_SIMULATION_CONTROL_FLOW =
      new CComment(
          "represent reachable statements of thread simulations in separate control flow"
              + " statements");
}
