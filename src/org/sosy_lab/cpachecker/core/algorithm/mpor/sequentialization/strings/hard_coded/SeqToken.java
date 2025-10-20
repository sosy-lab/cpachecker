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
 * variables alphabetically.
 */
public class SeqToken {

  /** a for access */
  public static final String a = "a";

  public static final String abort = "abort";

  public static final String ACCESS = "ACCESS";

  public static final String ASSERT_FAIL_KEYWORD = "__assert_fail";

  public static final String ASSERTION_KEYWORD_ASSERT_FAIL = "__assertion";

  public static final String assume = "assume";

  /** b for bit vector */
  public static final String b = "b";

  public static final String BINARY_LITERAL = "0b";

  public static final String BIT_VECTOR = "BIT_VECTOR";

  public static final String BREAK_KEYWORD = "break";

  /** C for Call */
  public static final String C = "C";

  public static final String CALL = "CALL";

  public static final String CASE_KEYWORD = "case";

  /** cnt for count */
  public static final String cnt = "cnt";

  public static final String cond = "cond";

  public static final String CONTINUE_KEYWORD = "continue";

  public static final String CPACHECKER_TMP_KEYWORD = "__CPAchecker_TMP_";

  /** d for direct */
  public static final String d = "d";

  public static final String DIRECT = "DIRECT";

  public static final String dummy = "dummy";

  public static final String E = "E";

  public static final String EXIT = "EXIT";

  public static final String FILE_KEYWORD_ASSERT_FAIL = "__file";

  public static final String FILE_NAME_PLACEHOLDER = "__FILE_NAME_PLACEHOLDER__";

  public static final String FUNCTION_KEYWORD_ASSERT_FAIL = "__function";

  public static final String G = "G";

  public static final String GLOBAL = "GLOBAL";

  public static final String GOTO_KEYWORD = "goto";

  public static final String HEXADECIMAL_LITERAL = "0x";

  public static final String iteration = "i";

  public static final String LINE_KEYWORD_ASSERT_FAIL = "__line";

  public static final String L = "L";

  public static final String last = "last";

  public static final String LAST = "LAST";

  public static final String last_thread = "last_thread";

  public static final String LOCAL = "LOCAL";

  public static final String LOCKED = "LOCKED";

  public static final String M = "M";

  public static final String main = "main";

  public static final String MAIN_FUNCTION_ARG = "MAIN_FUNCTION_ARG";

  public static final String malloc = "malloc";

  public static final String MPOR_PREFIX = "__MPOR__";

  public static final String next_thread = "next_thread";

  public static final String NUM = "NUM";

  /** Constant, hence uppercase. */
  public static final String NUM_THREADS = "NUM_THREADS";

  public static final String ONE_BIT = "1";

  public static final String P = "P";

  public static final String PARAMETER = "PARAMETER";

  /** pc for program counter */
  public static final String pc = "pc";

  public static final String PRETTY_FUNCTION_KEYWORD = "__PRETTY_FUNCTION__";

  /** r for reachable, or read. */
  public static final String r = "r";

  public static final String REACHABLE = "REACHABLE";

  public static final String reach_error = "reach_error";

  public static final String READ = "READ";

  public static final String READERS = "READERS";

  public static final String RETURN_KEYWORD = "return";

  public static final String return_value = "return_value";

  public static final String round = "round";

  public static final String round_max = "round_max";

  public static final String S = "S";

  public static final String SIGNALED = "SIGNALED";

  public static final String size = "size";

  public static final String START_ROUTINE_ARG = "START_ROUTINE_ARG";

  public static final String SYNC = "SYNC";

  public static final String t = "_t";

  public static final String T = "T";

  public static final String thread = "thread";

  public static final String THREAD = "THREAD";

  public static final String uint = "uint";

  /** w for write */
  public static final String w = "w";

  public static final String WRITE = "WRITE";

  public static final String WRITERS = "WRITERS";

  public static final String ZERO_BIT = "0";

  public static final String ZERO_STRING = "\"0\"";
}
