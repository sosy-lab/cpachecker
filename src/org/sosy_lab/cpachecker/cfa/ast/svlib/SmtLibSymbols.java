// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableSet;
import java.util.regex.Pattern;

/**
 * How a name is written as a symbol of SMT-LIB and SV-LIB. The parser drops the vertical bars of a
 * quoted symbol, so the AST holds names, and printing a name adds them again where they are needed.
 */
public final class SmtLibSymbols {

  private SmtLibSymbols() {}

  private static final Pattern SIMPLE_SYMBOL =
      Pattern.compile("[A-Za-z~!@$%^&*_+=<>.?/-][A-Za-z0-9~!@$%^&*_+=<>.?/-]*");

  /** The words that the grammar of SV-LIB reads as keywords and not as symbols. */
  static final ImmutableSet<String> RESERVED_WORDS =
      ImmutableSet.of(
          "!",
          "BINARY",
          "DECIMAL",
          "HEXADECIMAL",
          "NUMERAL",
          "_",
          "annotate-tag",
          "as",
          "assert",
          "assign",
          "assume",
          "break",
          "call",
          "check-sat",
          "check-sat-assuming",
          "choice",
          "continue",
          "correct-annotation",
          "declare-const",
          "declare-datatype",
          "declare-datatypes",
          "declare-fun",
          "declare-sort",
          "declare-var",
          "define-fun",
          "define-fun-rec",
          "define-funs-rec",
          "define-proc",
          "define-procs-rec",
          "define-sort",
          "echo",
          "entry-proc",
          "exists",
          "exit",
          "forall",
          "get-assertions",
          "get-assignment",
          "get-info",
          "get-model",
          "get-option",
          "get-proof",
          "get-unsat-assumptions",
          "get-unsat-core",
          "get-value",
          "get-witness",
          "goto",
          "havoc",
          "if",
          "incorrect-annotation",
          "init-global-vars",
          "init-proc-vars",
          "invalid-step",
          "label",
          "leap",
          "let",
          "match",
          "model",
          "old",
          "par",
          "pop",
          "push",
          "reset",
          "reset-assertions",
          "return",
          "select-trace",
          "sequence",
          "set-info",
          "set-logic",
          "set-option",
          "steps",
          "string",
          "using-annotation",
          "verify-call",
          "while");

  /**
   * The given name as a symbol: between vertical bars, unless it is a simple symbol that is not a
   * reserved word.
   */
  public static String quoteIfNeeded(String pName) {
    if (isSimpleSymbol(pName) && !RESERVED_WORDS.contains(pName)) {
      return pName;
    }
    return "|" + pName + "|";
  }

  /** Can the given name be written without vertical bars, if it is not a reserved word? */
  static boolean isSimpleSymbol(String pName) {
    return SIMPLE_SYMBOL.matcher(pName).matches();
  }
}
