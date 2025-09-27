// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded;

/**
 * All tokens used for function and variable names in the sequentialization. Make sure:
 *
 * <ul>
 *   <li>that the variable name is an exact representation for the string including lower/upper case
 *       and underscores but excluding quotation marks ", e.g. {@code var = "var"} or {@code TEST =
 *       "\"TEST\""}
 *   <li>to use a single preceding underscore for variable names that cannot have the strings value,
 *       e.g. {@code _break = "break"} or {@code _0 = "0"}
 *   <li>to sort the variables by alphabet after removing underscores
 * </ul>
 */
public class SeqToken {

  public static final String _0 = "0";

  public static final String _0b = "0b";

  public static final String _0x = "0x";

  public static final String _1 = "1";

  /** a for access */
  public static final String a = "a";

  public static final String abort = "abort";

  public static final String ACCESS = "ACCESS";

  public static final String act = "act";

  public static final String __assert_fail = "__assert_fail";

  public static final String __assertion = "__assertion";

  public static final String assume = "assume";

  public static final String assume_abort_if_not = "assume_abort_if_not";

  /** b for bit vector */
  public static final String b = "b";

  /** "bit vector write" */
  public static final String bw = "bw";

  public static final String BIT_VECTOR = "BIT_VECTOR";

  public static final String _break = "break";

  /** C for Call */
  public static final String C = "C";

  public static final String CALL = "CALL";

  public static final String _case = "case";

  public static final String cnt = "cnt";

  public static final String cond = "cond";

  public static final String _continue = "continue";

  public static final String __CPAchecker_TMP_ = "__CPAchecker_TMP_";

  /** d for direct */
  public static final String d = "d";

  public static final String _default = "default";

  public static final String DIRECT = "DIRECT";

  public static final String dummy = "dummy";

  public static final String E = "E";

  public static final String END = "END";

  public static final String EXIT = "EXIT";

  public static final String __file = "__file";

  public static final String __FILE_NAME_PLACEHOLDER__ = "__FILE_NAME_PLACEHOLDER__";

  public static final String __function = "__function";

  public static final String G = "G";

  public static final String GLOBAL = "GLOBAL";

  public static final String _goto = "goto";

  public static final String i = "i";

  public static final String K = "K";

  public static final String __line = "__line";

  public static final String L = "L";

  public static final String last = "last";

  public static final String LAST = "LAST";

  public static final String last_thread = "last_thread";

  public static final String ldv_assume = "ldv_assume";

  public static final String LOCAL = "LOCAL";

  public static final String LOCKED = "LOCKED";

  public static final String M = "M";

  public static final String main = "main";

  public static final String MAIN_FUNCTION_ARG = "MAIN_FUNCTION_ARG";

  public static final String __MPOR_SEQ__ = "__MPOR_SEQ__";

  public static final String next_thread = "next_thread";

  /** Constant, hence uppercase. */
  public static final String NUM_THREADS = "NUM_THREADS";

  public static final String P = "P";

  public static final String PARAMETER = "PARAMETER";

  /** pc for program counter */
  public static final String pc = "pc";

  public static final String __PRETTY_FUNCTION__ = "__PRETTY_FUNCTION__";

  /** r for reachable */
  public static final String r = "r";

  public static final String REACHABLE = "REACHABLE";

  public static final String reach_error = "reach_error";

  public static final String READ = "READ";

  public static final String return_value = "return_value";

  public static final String S = "S";

  public static final String __SEQUENTIALIZATION_ERROR__ = "__SEQUENTIALIZATION_ERROR__";

  public static final String START_ROUTINE_ARG = "START_ROUTINE_ARG";

  public static final String STRING_0 = "\"0\"";

  public static final String SYNC = "SYNC";

  public static final String T = "T";

  public static final String _t = "_t";

  public static final String THREAD = "THREAD";

  public static final String uint = "uint";

  public static final String __VERIFIER_assume = "__VERIFIER_assume";

  /** w for write */
  public static final String w = "w";

  public static final String WRITE = "WRITE";
}
