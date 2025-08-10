// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.transfer;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/** Represents special function types that require custom handling in the pointer analysis. */
public enum FunctionType {
  /** The memory allocation function, e.g., {@code malloc}. */
  MALLOC("malloc"),

  /** The memory deallocation function, e.g., {@code free}. */
  FREE("free"),

  /**
   * A function that returns a non-deterministic pointer, e.g., {@code __VERIFIER_nondet_pointer}.
   */
  NON_DETERMINISTIC_POINTER("__VERIFIER_nondet_pointer"),

  /** A regular or unknown function that does not require special handling. */
  UNKNOWN("unknown");

  @NonNull private final String functionName;

  /** A static, immutable map for efficient reverse lookup of a function type by its name. */
  private static final Map<String, FunctionType> NAME_TO_TYPE_MAP;

  static {
    NAME_TO_TYPE_MAP =
        Arrays.stream(values())
            .collect(
                ImmutableMap.toImmutableMap(
                    type -> Objects.requireNonNull(type).functionName, Function.identity()));
  }

  FunctionType(String pFunctionName) {
    functionName = pFunctionName;
  }

  /**
   * Determines the {@link FunctionType} from a function name expression.
   *
   * @param pFunctionNameExpression The CFA expression representing the function name.
   * @return The corresponding {@link FunctionType}, or {@link #UNKNOWN} if the function is not one
   *     of the special types.
   */
  public static FunctionType fromExpression(CExpression pFunctionNameExpression) {
    return getFunctionName(pFunctionNameExpression)
        .map(name -> NAME_TO_TYPE_MAP.getOrDefault(name, UNKNOWN))
        .orElse(UNKNOWN);
  }

  private static Optional<String> getFunctionName(CExpression pExpression) {
    if (pExpression instanceof CIdExpression idExpr) {
      return Optional.of(idExpr.getName());
    }
    return Optional.empty();
  }
}
