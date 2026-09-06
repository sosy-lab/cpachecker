// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableSet;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.Vocabulary;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibParser;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

final class CToSvLibTransformationConstants {
  static final String INPUT_VAR_DUMMY_PREFIX = "__originalInput_";

  /** The characters that a simple symbol of SMT-LIB may contain besides letters and digits. */
  private static final String SYMBOL_PUNCTUATION = "~!@$%^&*_+=<>.?/-";

  private static final Pattern SIMPLE_SYMBOL =
      Pattern.compile(
          "[A-Za-z"
              + Pattern.quote(SYMBOL_PUNCTUATION)
              + "]"
              + "[A-Za-z0-9"
              + Pattern.quote(SYMBOL_PUNCTUATION)
              + "]*");

  /** The words that the grammar of SV-LIB reserves and that therefore cannot be symbols. */
  private static final ImmutableSet<String> KEYWORDS = collectKeywords();

  private static ImmutableSet<String> collectKeywords() {
    ImmutableSet.Builder<String> keywords = ImmutableSet.builder();
    Vocabulary vocabulary = SvLibParser.VOCABULARY;
    for (int token = 0; token <= vocabulary.getMaxTokenType(); token++) {
      String literal = vocabulary.getLiteralName(token);
      if (literal != null && literal.length() > 2) {
        // The literal of a token is quoted, as in "'choice'".
        keywords.add(literal.substring(1, literal.length() - 1));
      }
    }
    return keywords.build();
  }

  /**
   * The given name as a symbol of SMT-LIB, quoted if it cannot be written as it is.
   *
   * <p>A simple symbol does not start with a digit and contains neither parentheses nor spaces, so
   * a name like the expression of a call through a function pointer has to be quoted. A word that
   * the grammar of SV-LIB reserves, such as {@code choice}, has to be quoted as well, and a program
   * may well contain a variable or a function of that name.
   */
  static String asSymbol(String pName) {
    return SIMPLE_SYMBOL.matcher(pName).matches() && !KEYWORDS.contains(pName)
        ? pName
        : "|" + pName + "|";
  }

  /**
   * The variable that holds the address above all memory that has been allocated so far, which is
   * where the next allocation starts.
   */
  static final String HIGHEST_ALLOCATED_ADDRESS = "__transformationHighestAllocatedAddress";

  /**
   * The variable that holds the address at which the memory that the program allocates begins. The
   * objects that exist without being allocated lie below it.
   */
  static final String FIRST_ALLOCATED_ADDRESS = "__transformationFirstAllocatedAddress";

  private static final String RETURN_VAR_DUMMY_PREFIX = "__transformationDummyReturn_";
  private static final String TMP_VAR_ASSIGNMENT = "__Transformation_TMP_VariableAssignment_";

  /**
   * The name of the variable that holds the value that a procedure of an external function returns.
   *
   * <p>The name contains the type of the value, because the analysis of the generated program does
   * not distinguish the variables of two procedures by the name of the procedure, and two variables
   * of the same name must not have different types.
   */
  static String returnValueName(SvLibType pReturnType) {
    return "_retval_" + nameOfType(pReturnType);
  }

  /**
   * The name of the global variable that holds the value that a procedure returns, for a call in a
   * procedure for which no dummy of that type was declared.
   *
   * <p>It differs from the name of the dummy that a procedure declares, because a global variable
   * and a variable of a procedure must not have the same name.
   */
  static String globalReturnDummyVariableName(SvLibType pReturnType) {
    return "__transformationGlobalDummyReturn_" + nameOfType(pReturnType);
  }

  /**
   * The name of the variable that holds the return value of a procedure call whose result is not
   * assigned to a variable of the generated program.
   */
  static String returnDummyVariableName(SvLibType pReturnType) {
    return RETURN_VAR_DUMMY_PREFIX + nameOfType(pReturnType);
  }

  /**
   * The name of the variable that holds the return value of a procedure call before it is stored in
   * the array that models the heap.
   */
  static String tmpVariableNameForAssignment(SvLibType pReturnType) {
    return TMP_VAR_ASSIGNMENT + nameOfType(pReturnType);
  }

  /**
   * A representation of the given type that is usable as part of a variable name.
   *
   * <p>The types do not implement {@link Object#toString()}, so their default representation
   * contains an identity hash code, which differs between two instances of the same type and can
   * therefore not be used to build a name that several parts of the transformation agree on.
   */
  private static String nameOfType(SvLibType pType) {
    if (pType instanceof SvLibSmtLibBitVectorType bitVectorType) {
      return "bv" + bitVectorType.getSize();
    }
    if (pType instanceof SvLibSmtLibFloatingPointType floatingPointType) {
      return "fp"
          + floatingPointType.getExponentSize()
          + "_"
          + floatingPointType.getSignificandSize();
    }
    if (pType instanceof SvLibSmtLibPredefinedType predefinedType) {
      return predefinedType.toString();
    }
    throw new UnsupportedOperationException(
        "Cannot create a variable name for the type " + pType.toASTString());
  }

  /**
   * Functions that allocate memory on the heap.
   *
   * <p>Their call is transformed through the formula of the whole edge, which contains the base
   * address of the allocated block and the constraints that separate it from the objects that
   * already exist. Those constraints are the same for every execution of the statement, so the
   * transformation additionally separates the block from the blocks that earlier executions
   * allocated, see {@link #HIGHEST_ALLOCATED_ADDRESS}.
   */
  static final ImmutableSet<String> NAMES_OF_MEMORY_ALLOCATION_FUNCTIONS =
      ImmutableSet.of(
          "malloc",
          "calloc",
          "realloc",
          "kzalloc",
          "kmalloc",
          "__kmalloc",
          "alloca",
          "__builtin_alloca");

  static final ImmutableSet<String> NAMES_OF_ASSERT_FUNCTIONS =
      ImmutableSet.of("__assert_fail", "__assert_perror_fail", "__assert");
  static final ImmutableSet<String> NAMES_OF_UNSUPPORTED_STDLIB_EXTERNAL_FUNCTIONS =
      ImmutableSet.of(
          "atof",
          "strtof",
          "strtold",
          "strtod",
          "strtol",
          "strtoul",
          "strtoq",
          "strtouq",
          "strtoll",
          "strtoull",
          "initstate",
          "setstate",
          "drand48",
          "erand48",
          "drand48_r",
          "erand48_r",
          "nrand48_r",
          "mrand48_r",
          "jrand48_r",
          "srand48_r",
          "seed48_r",
          "lcong48_r",
          "atexit",
          "at_quick_exit",
          "onexit",
          "getenv",
          "mktemp",
          "mkdtemp",
          "realpath",
          "bsearch",
          "ecvt",
          "fcvt",
          "gcvt",
          "qecvt",
          "qfcvt",
          "qgcvt",
          "ecvt_r",
          "fcvt_r",
          "qecvt_r",
          "qfcvt_r");
  static final ImmutableSet<String> NAMES_OF_UNSUPPORTED_NONDET_FUNCTIONS = ImmutableSet.of();

  private CToSvLibTransformationConstants() {
    throw new AssertionError("Cannot instantiate TransformationConstants");
  }
}
