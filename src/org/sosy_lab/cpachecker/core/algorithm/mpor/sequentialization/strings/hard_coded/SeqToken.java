// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded;

/**
 * All tokens used for function and variable names in the sequentialization. Make sure to sort the
 * variables alphabetically based on their string value.
 */
public class SeqToken {

  public static final String ZERO_BIT = "0";

  public static final String ZERO_STRING = "\"0\"";

  public static final String BINARY_LITERAL = "0b";

  public static final String HEXADECIMAL_LITERAL = "0x";

  public static final String ONE_BIT = "1";

  /** a for access */
  public static final String ACCESS_BIT_VECTOR_PREFIX = "a";

  public static final String ABORT_FUNCTION_NAME = "abort";

  public static final String ACCESS = "ACCESS";

  public static final String ASSERT_FAIL_FUNCTION_NAME = "__assert_fail";

  public static final String ASSERTION_KEYWORD_ASSERT_FAIL = "__assertion";

  public static final String ASSUME_FUNCTION_NAME = "assume";

  /** b for bit vector */
  public static final String BIT_VECTOR_PREFIX = "b";

  public static final String BIT_VECTOR = "BIT_VECTOR";

  public static final String BREAK_KEYWORD = "break";

  /** C for Call */
  public static final String CALL_PREFIX = "C";

  public static final String CALL = "CALL";

  public static final String CASE_KEYWORD = "case";

  /** cnt for count */
  public static final String THREAD_COUNT_VARIABLE = "cnt";

  public static final String COND_KEYWORD_ASSUME = "cond";

  public static final String CONTINUE_KEYWORD = "continue";

  public static final String CPACHECKER_TMP_KEYWORD = "__CPAchecker_TMP_";

  /** d for direct */
  public static final String DIRECT_BIT_VECTOR_PREFIX = "d";

  public static final String DIRECT = "DIRECT";

  public static final String DUMMY = "dummy";

  public static final String EXIT_PREFIX = "E";

  public static final String EXIT = "EXIT";

  public static final String FILE_KEYWORD_ASSERT_FAIL = "__file";

  public static final String FILE_NAME_PLACEHOLDER = "__FILE_NAME_PLACEHOLDER__";

  public static final String FUNCTION_KEYWORD_ASSERT_FAIL = "__function";

  public static final String GLOBAL_VARIABLE_PREFIX = "G";

  public static final String GLOBAL = "GLOBAL";

  public static final String GOTO_KEYWORD = "goto";

  public static final String ITERATION_VARIABLE = "i";

  public static final String LINE_KEYWORD_ASSERT_FAIL = "__line";

  public static final String LAST_KEYWORD = "LAST";

  public static final String LOCAL_VARIABLE_PREFIX = "L";

  public static final String LOCAL = "LOCAL";

  public static final String MUTEX_LOCKED_SUFFIX = "LOCKED";

  public static final String MAIN_FUNCTION_ARG_PREFIX = "M";

  public static final String MAIN_FUNCTION_ARG = "MAIN_FUNCTION_ARG";

  public static final String MAIN_FUNCTION_KEYWORD = "main";

  public static final String MALLOC_FUNCTION_KEYWORD = "malloc";

  public static final String MPOR_PREFIX = "__MPOR__";

  public static final String NEXT_THREAD_VARIABLE = "next_thread";

  public static final String RW_LOCK_NUM_READERS_SUFFIX = "NUM_WRITERS";

  public static final String RW_LOCK_NUM_WRITERS_SUFFIX = "NUM_WRITERS";

  public static final String PARAMETER_PREFIX = "P";

  public static final String PARAMETER = "PARAMETER";

  public static final String PRETTY_FUNCTION_KEYWORD = "__PRETTY_FUNCTION__";

  /** pc for program counter */
  public static final String PROGRAM_COUNTER_VARIABLE = "pc";

  /** r for reachable. */
  public static final String REACHABLE_BIT_VECTOR_PREFIX = "r";

  public static final String REACHABLE = "REACHABLE";

  public static final String REACH_ERROR_FUNCTION_NAME = "reach_error";

  /** r for read. */
  public static final String READ_BIT_VECTOR_PREFIX = "r";

  public static final String READ = "READ";

  public static final String RETURN_KEYWORD = "return";

  public static final String RETURN_VALUE_SUFFIX = "return_value";

  public static final String ROUND_VARIABLE = "round";

  public static final String ROUND_MAX_VARIABLE = "round_max";

  public static final String COND_SIGNALED_SUFFIX = "SIGNALED";

  public static final String SIZE_KEYWORD_MALLOC = "size";

  public static final String START_ROUTINE_ARG = "START_ROUTINE_ARG";

  public static final String START_ROUTINE_ARG_PREFIX = "S";

  public static final String SYNC = "SYNC";

  public static final String THREAD_PREFIX = "T";

  public static final String THREAD = "THREAD";

  public static final String TYPE_NAME_SUFFIX = "_t";

  public static final String UINT_TYPE_KEYWORD = "uint";

  /** w for write */
  public static final String WRITE_BIT_VECTOR_PREFIX = "w";

  public static final String WRITE = "WRITE";
}
