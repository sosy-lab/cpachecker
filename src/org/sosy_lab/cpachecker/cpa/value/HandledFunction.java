// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinIoFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;

/**
 * The kinds of function call that the value analysis handles itself instead of treating the called
 * function as an unknown function.
 *
 * <p>This enum is the single place that decides which calls these are. Every dispatch that handles
 * such a call asks {@link #of(String)} for the kind it has to handle, and {@link
 * ValueAnalysisTransferRelation#handleUnknownOrUnhandledFunctionCalls} reports every call without a
 * kind as unknown or unhandled, which weakens the verdict of the analyses that ask (cf. {@link
 * org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA}). Teaching the value analysis another
 * function therefore means adding its kind here and handling it where the kind is dispatched; the
 * two cannot drift apart silently any more.
 */
enum HandledFunction {

  /** A call of {@code free}, handled by {@link ValueAnalysisTransferRelation}. */
  FREE(Context.ANY),

  /**
   * A call of {@code fscanf} and friends, which {@link ValueAnalysisTransferRelation} models as an
   * assignment of a nondeterministic value.
   */
  FSCANF(Context.ANY),

  /**
   * A call that returns an input of the program, handled by {@link
   * ExpressionValueVisitorWithRandomSampling} and {@link
   * ExpressionValueVisitorWithPredefinedValues} if they are used. This is exact nondeterminism
   * rather than an unknown function, so it never weakens a verdict by itself.
   */
  NONDET_INPUT(Context.ANY),

  /**
   * A call of one of the builtin overflow functions, evaluated by {@link
   * AbstractExpressionValueVisitor}. A call that stores its result through a pointer is handled
   * only as an assignment, so {@link ValueAnalysisTransferRelation} checks the other calls against
   * the unsupported functions.
   */
  BUILTIN_OVERFLOW(Context.ASSIGNMENT),

  /**
   * A call of one of the popcount functions, evaluated by {@link AbstractExpressionValueVisitor}.
   */
  POPCOUNT(Context.ASSIGNMENT),

  /**
   * A call of one of the builtin float functions, evaluated by {@link
   * AbstractExpressionValueVisitor}.
   */
  BUILTIN_FLOAT(Context.ASSIGNMENT);

  /** The name of the function that deallocates memory. */
  static final String FREE_FUNCTION = "free";

  /** In which calls the value analysis computes the effect of a function. */
  private enum Context {
    /** In every call. */
    ANY,
    /** Only in a call whose result is assigned, because the handling computes a return value. */
    ASSIGNMENT
  }

  private final Context context;

  HandledFunction(Context pContext) {
    context = pContext;
  }

  /**
   * The kind of handling that the value analysis has for a call of the given function, or {@code
   * null} if it has none and the function is therefore unknown to it.
   */
  static @Nullable HandledFunction of(String pFunctionName) {
    if (pFunctionName.equals(FREE_FUNCTION)) {
      return FREE;
    }
    if (BuiltinIoFunctions.matchesFscanf(pFunctionName)) {
      return FSCANF;
    }
    if (pFunctionName.startsWith(ExpressionValueVisitorWithRandomSampling.PATTERN_FOR_RANDOM)) {
      return NONDET_INPUT;
    }
    if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(pFunctionName)) {
      return BUILTIN_OVERFLOW;
    }
    if (BuiltinFunctions.isBuiltinFunction(pFunctionName)
        && BuiltinFunctions.isPopcountFunction(pFunctionName)) {
      return POPCOUNT;
    }
    if (BuiltinFloatFunctions.isBuiltinFloatFunction(pFunctionName)) {
      return BUILTIN_FLOAT;
    }
    return null;
  }

  /**
   * Whether the value analysis computes the effect of a call of the given function, i.e., whether
   * the call is neither unknown nor unhandled.
   *
   * @param pIsAssignment whether the result of the call is assigned to something
   */
  static boolean isHandled(String pFunctionName, boolean pIsAssignment) {
    HandledFunction handled = of(pFunctionName);
    return handled != null && (pIsAssignment || handled.context == Context.ANY);
  }
}
