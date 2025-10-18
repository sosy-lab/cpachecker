// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import java.util.Arrays;
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

  public enum CAtomicOperationType {
    STORE,
    LOAD,
    EXCHANGE,
    CMP_XCHG,
    FETCH
  }

  public enum CAtomicOperationsLookup {
    ATOMIC_STORE_N("__atomic_store_n", CAtomicOperationType.STORE),
    ATOMIC_STORE("__atomic_store", CAtomicOperationType.STORE),
    ATOMIC_LOAD_N("__atomic_load_n", CAtomicOperationType.LOAD),
    ATOMIC_LOAD("__atomic_load", CAtomicOperationType.LOAD),
    ATOMIC_EXCHANGE_N("__atomic_exchange_n", CAtomicOperationType.EXCHANGE),
    ATOMIC_EXCHANGE("__atomic_exchange", CAtomicOperationType.EXCHANGE),
    ATOMIC_CMP_XCHG_N("__atomic_compare_exchange_n", CAtomicOperationType.CMP_XCHG),
    ATOMIC_CMP_XCHG("__atomic_compare_exchange", CAtomicOperationType.CMP_XCHG),
    ATOMIC_FETCH_ADD("__atomic_fetch_add", CAtomicOperationType.FETCH),
    ATOMIC_FETCH_SUB("__atomic_fetch_sub", CAtomicOperationType.FETCH),
    ATOMIC_FETCH_AND("__atomic_fetch_and", CAtomicOperationType.FETCH),
    ATOMIC_FETCH_OR("__atomic_fetch_or", CAtomicOperationType.FETCH),
    ATOMIC_FETCH_XOR("__atomic_fetch_xor", CAtomicOperationType.FETCH),
    ATOMIC_FETCH_NAND("__atomic_fetch_nand", CAtomicOperationType.FETCH);

    private final String representation;
    private final CAtomicOperationType operationType;

    CAtomicOperationsLookup(String pRepresentation, CAtomicOperationType pOperationType) {
      representation = pRepresentation;
      operationType = pOperationType;
    }

    public String getRepresentation() {
      return representation;
    }

    public CAtomicOperationType getOperationType() {
      return operationType;
    }

    public static CAtomicOperationsLookup fromString(String s) {
      return Arrays.stream(values())
          .filter(it -> it.representation.equals(s))
          .findFirst()
          .orElse(null);
    }
  }

  /** Check whether a given function name identifies an atomic builtin. */
  public static boolean isBuiltinAtomicFunction(String pFunctionName) {
    return pFunctionName != null && CAtomicOperationsLookup.fromString(pFunctionName) != null;
  }

  public static boolean matchesStore(String pFunctionName) {
    return CAtomicOperationsLookup.fromString(pFunctionName).operationType
        == CAtomicOperationType.STORE;
  }

  public static boolean matchesLoad(String pFunctionName) {
    return CAtomicOperationsLookup.fromString(pFunctionName).operationType
        == CAtomicOperationType.LOAD;
  }

  public static boolean matchesExchange(String pFunctionName) {
    return CAtomicOperationsLookup.fromString(pFunctionName).operationType
        == CAtomicOperationType.EXCHANGE;
  }

  public static boolean matchesCompareExchange(String pFunctionName) {
    return CAtomicOperationsLookup.fromString(pFunctionName).operationType
        == CAtomicOperationType.CMP_XCHG;
  }

  public static boolean matchesFetchOp(String pFunctionName) {
    return CAtomicOperationsLookup.fromString(pFunctionName).operationType
        == CAtomicOperationType.FETCH;
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
