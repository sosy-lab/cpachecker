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

  public static final String _0 = "\"0\"";

  public static final String abort = "abort";

  public static final String __assert_fail = "__assert_fail";

  public static final String __assertion = "__assertion";

  public static final String assume = "assume";

  public static final String ASSUME = "ASSUME";

  public static final String ATOMIC = "ATOMIC";

  public static final String BEGINS = "BEGINS";

  public static final String _break = "break";

  public static final String C = "C";

  public static final String CALL = "CALL";

  public static final String _case = "case";

  public static final String cond = "cond";

  public static final String __CPAchecker_TMP_ = "__CPAchecker_TMP_";

  public static final String _default = "default";

  public static final String dummy = "dummy";

  public static final String __file = "__file";

  public static final String __FILE_NAME_PLACEHOLDER__ = "__FILE_NAME_PLACEHOLDER__";

  public static final String __function = "__function";

  public static final String G = "G";

  public static final String GLOBAL = "GLOBAL";

  public static final String _goto = "goto";

  public static final String JOINS = "JOINS";

  public static final String K = "K";

  public static final String __line = "__line";

  public static final String L = "L";

  public static final String LOCAL = "LOCAL";

  public static final String LOCKED = "LOCKED";

  public static final String LOCKS = "LOCKS";

  public static final String LOOP_HEAD = "LOOP_HEAD";

  public static final String main = "main";

  public static final String __MPOR_SEQ__ = "__MPOR_SEQ__";

  public static final String next_thread = "next_thread";

  /** Constant, hence uppercase. */
  public static final String NUM_THREADS = "NUM_THREADS";

  public static final String P = "P";

  public static final String PARAMETER = "PARAMETER";

  public static final String pc = "pc";

  public static final String __PRETTY_FUNCTION__ = "__PRETTY_FUNCTION__";

  public static final String r = "r";

  public static final String reach_error = "reach_error";

  public static final String __SEQUENTIALIZATION_ERROR__ = "__SEQUENTIALIZATION_ERROR__";

  public static final String SWITCH = "SWITCH";

  public static final String T = "T";

  public static final String THREAD = "THREAD";

  public static final String __VERIFIER_nondet_int = "__VERIFIER_nondet_int";

  public static final String __VERIFIER_nondet_uint = "__VERIFIER_nondet_uint";
}
