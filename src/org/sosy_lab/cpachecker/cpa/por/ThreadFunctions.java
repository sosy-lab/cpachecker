// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;

/**
 * Utility class for detecting and extracting information from pthread-related C function calls
 * ({@code pthread_create}, {@code pthread_join}).
 */
public final class ThreadFunctions {

  private static final ImmutableSet<String> CREATE_FUNCTIONS = ImmutableSet.of("pthread_create");
  private static final ImmutableSet<String> JOIN_FUNCTIONS = ImmutableSet.of("pthread_join");
  private static final ImmutableSet<String> THREAD_EXIT_FUNCTIONS = ImmutableSet.of("pthread_exit");

  private ThreadFunctions() {
  }

  /**
   * Returns {@code true} if the given function name is a thread creation function.
   */
  public static boolean isCreateFunction(String functionName) {
    return CREATE_FUNCTIONS.contains(functionName);
  }

  /**
   * Returns {@code true} if the given function name is a thread join function.
   */
  public static boolean isJoinFunction(String functionName) {
    return JOIN_FUNCTIONS.contains(functionName);
  }

  /**
   * Returns {@code true} if the given function name is a thread exit function.
   */
  public static boolean isThreadExitFunction(String functionName) {
    return THREAD_EXIT_FUNCTIONS.contains(functionName);
  }

  /**
   * Checks that a {@code pthread_create} call has the expected 4 arguments. The thread handle
   * argument itself (params.get(0)) is not further restricted: any pointer-typed expression is
   * accepted, and its identity is established at runtime via a synthetic thread-id write (see
   * OrderingConsistencyTransferRelation#handleCreate / PORTransferRelation's create dispatch), not
   * by statically resolving a variable name here.
   */
  public static void checkCreateParams(List<? extends AExpression> params) {
    checkState(params.size() == 4, "Malformed pthread_create (not 4 params): %s", params);
  }

  /**
   * Extracts the started function's name from a {@code pthread_create} call's parameter list.
   *
   * @param params the parameter expressions of the {@code pthread_create} call (must have 4
   *               elements)
   * @return the simple name of the function to be started in the new thread
   */
  public static String extractCreateFunctionName(List<? extends AExpression> params) {
    checkCreateParams(params);
    checkState(
        params.get(2) instanceof CUnaryExpression cUnaryExpression
            && cUnaryExpression.getOperator() == UnaryOperator.AMPER,
        "Malformed pthread_create (Thread not unary expression with reference): %s",
        params.get(2));
    checkState(
        ((CUnaryExpression) params.get(2)).getOperand() instanceof CIdExpression,
        "Malformed pthread_create (Thread not CIdExpression): %s",
        ((CUnaryExpression) params.get(2)).getOperand());
    return ((CIdExpression) ((CUnaryExpression) params.get(2)).getOperand()).getName();
  }

  /**
   * Checks that a {@code pthread_join} call has the expected 2 arguments. As with {@link
   * #checkCreateParams}, the handle argument (params.get(0)) itself is unrestricted: which thread
   * instance it identifies is resolved by candidate-set branching over the live thread instances
   * at the join site, not by statically resolving a variable name here.
   */
  public static void checkJoinParams(List<? extends AExpression> params) {
    checkState(params.size() == 2, "Malformed pthread_join (not 2 params): %s", params);
  }

  /**
   * A string key identifying the storage location a {@code pthread_create}/{@code pthread_join}
   * handle addresses, or null if that cannot be determined purely syntactically. Used by both
   * {@link PORTransferRelation} (to populate/consult the fast-path join hint) and {@link
   * PORState#isJoinCurrentlyEnabled} (which must decide, consistently with the transfer relation,
   * whether a join is actually enabled without introducing any synthetic branching) — the two
   * call sites must agree on what counts as a resolvable handle, or a join could be offered by one
   * and rejected by the other, silently dropping every schedule that reaches that state (see git
   * history for the resulting soundness bug this exact mismatch caused).
   *
   * <p>Beyond a plain variable ({@code t}), this also resolves array elements and struct fields
   * reached through a chain of <b>literal</b> array indices and <b>non-pointer</b> field accesses
   * (e.g. {@code t[0]}, {@code s.handles[1]}): such a path denotes the same storage location on
   * every evaluation, so two occurrences with the same key are provably the same location without
   * needing runtime information. A path that goes through a runtime-computed index (a loop
   * variable, say) or a pointer dereference (which could alias in ways this syntactic check cannot
   * rule out) returns null, falling back to general candidate-set branching.
   */
  public static @Nullable String canonicalHandleLvalueKey(CExpression handle) {
    return handle instanceof CLeftHandSide lvalue ? canonicalLvalueKey(lvalue) : null;
  }

  /** Same as {@link #canonicalHandleLvalueKey}, but for a {@code pthread_create} handle, which is
   * syntactically {@code &lvalue} (the lvalue itself, not the address-of expression, is the key).
   */
  public static @Nullable String canonicalHandleAddressKey(CExpression handle) {
    if (handle instanceof CUnaryExpression unary
        && unary.getOperator() == UnaryOperator.AMPER
        && unary.getOperand() instanceof CLeftHandSide lvalue) {
      return canonicalLvalueKey(lvalue);
    }
    return null;
  }

  private static @Nullable String canonicalLvalueKey(CLeftHandSide lvalue) {
    if (lvalue instanceof CIdExpression id) {
      return id.getDeclaration().getQualifiedName();
    }
    if (lvalue instanceof CArraySubscriptExpression subscript
        && subscript.getArrayExpression() instanceof CLeftHandSide array
        && subscript.getSubscriptExpression() instanceof CIntegerLiteralExpression literal) {
      String arrayKey = canonicalLvalueKey(array);
      return arrayKey == null ? null : arrayKey + "[" + literal.getValue() + "]";
    }
    if (lvalue instanceof CFieldReference field && !field.isPointerDereference()
        && field.getFieldOwner() instanceof CLeftHandSide owner) {
      String ownerKey = canonicalLvalueKey(owner);
      return ownerKey == null ? null : ownerKey + "." + field.getFieldName();
    }
    return null;
  }
}
