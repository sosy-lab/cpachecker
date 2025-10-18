// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * Helper for handling C11/GCC atomic builtin functions (names starting with {@code __atomic_}).
 *
 * <p>This class provides simple recognizers for the most common atomic builtins and exposes
 * statically-known return types when possible. Some atomic builtins (e.g. {@code __atomic_load_n}
 * or {@code __atomic_exchange_n}) have a return type that depends on the pointer argument; those
 * are intentionally left unspecified here and should be resolved from the call-site when available.
 */
public final class BuiltinAtomicFunctions {

  private BuiltinAtomicFunctions() {}

  private static final ImmutableSet<String> STORES =
      ImmutableSet.of("__atomic_store_n", "__atomic_store");
  private static final ImmutableSet<String> LOADS =
      ImmutableSet.of("__atomic_load_n", "__atomic_load");
  private static final ImmutableSet<String> EXCHANGES =
      ImmutableSet.of("__atomic_exchange_n", "__atomic_exchange");
  private static final ImmutableSet<String> COMPARE_EXCHANGES =
      ImmutableSet.of("__atomic_compare_exchange_n", "__atomic_compare_exchange");

  private static final ImmutableSet<String> FETCH_OPS =
      ImmutableSet.of(
          "__atomic_fetch_add",
          "__atomic_fetch_sub",
          "__atomic_fetch_and",
          "__atomic_fetch_or",
          "__atomic_fetch_xor",
          "__atomic_fetch_nand");

  /** Check whether a given function name identifies an atomic builtin. */
  public static boolean isBuiltinAtomicFunction(String pFunctionName) {
    return pFunctionName != null && pFunctionName.startsWith("__atomic_");
  }

  public static boolean matchesStore(String pFunctionName) {
    return STORES.contains(pFunctionName);
  }

  public static boolean matchesLoad(String pFunctionName) {
    return LOADS.contains(pFunctionName);
  }

  public static boolean matchesExchange(String pFunctionName) {
    return EXCHANGES.contains(pFunctionName);
  }

  public static boolean matchesCompareExchange(String pFunctionName) {
    return COMPARE_EXCHANGES.contains(pFunctionName);
  }

  public static boolean matchesFetchOp(String pFunctionName) {
    return FETCH_OPS.contains(pFunctionName);
  }

  /**
   * Return the statically-known return type for some atomic builtins when possible.
   *
   * <p>For example, {@code __atomic_store_n} returns void and {@code __atomic_compare_exchange_n}
   * returns a boolean success indicator. Other functions that return the value
   * stored/loaded/exchanged depend on the pointer-argument's referenced type and are therefore not
   * resolved here.
   */
  public static Optional<CType> getType(String pFunctionName) {
    if (matchesStore(pFunctionName)) {
      return Optional.of(CVoidType.VOID);
    }
    if (matchesCompareExchange(pFunctionName)) {
      // __atomic_compare_exchange_n returns a boolean-like success indicator
      return Optional.of(CNumericTypes.BOOL);
    }
    // loads, exchanges and fetch-ops return a type that depends on the pointer argument;
    // leave unspecified so callers can derive the type from the call-site if desired.
    return Optional.empty();
  }
}
