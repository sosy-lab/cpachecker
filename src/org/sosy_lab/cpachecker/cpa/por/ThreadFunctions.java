// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

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
   * @throws UnsupportedCodeException if the third parameter is not (optionally cast to some
   *     function pointer type) a {@code &function} expression naming the entry point directly,
   *     e.g. a function pointer computed at runtime
   */
  public static String extractCreateFunctionName(List<? extends AExpression> params)
      throws UnsupportedCodeException {
    checkCreateParams(params);
    AExpression threadArg = stripCasts(params.get(2));
    if (!(threadArg instanceof CUnaryExpression cUnaryExpression)
        || cUnaryExpression.getOperator() != UnaryOperator.AMPER) {
      throw new UnsupportedCodeException(
          "Malformed pthread_create (Thread not unary expression with reference): "
              + params.get(2),
          null);
    }
    if (!(cUnaryExpression.getOperand() instanceof CIdExpression idExpression)) {
      throw new UnsupportedCodeException(
          "Malformed pthread_create (Thread not CIdExpression): " + cUnaryExpression.getOperand(),
          null);
    }
    return idExpression.getName();
  }

  /**
   * Strips any surrounding (possibly nested) {@link CCastExpression}s, e.g. so that {@code
   * (void *(*)(void *))(&f)} is seen as {@code &f}. Grouping parentheses need no separate
   * handling: CPAchecker's C frontend does not represent them as AST nodes.
   */
  private static AExpression stripCasts(AExpression expression) {
    AExpression result = expression;
    while (result instanceof CCastExpression cast) {
      result = cast.getOperand();
    }
    return result;
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

  /**
   * The name a thread's private copy of {@code pQualifiedName} gets. Single definition of the
   * per-thread renaming scheme: {@code PorAstCloner} renames every local, parameter and {@code
   * __thread} variable through here, and the transfer relations synthesize their per-thread
   * initializations against the very same name (see {@link #threadLocalGlobals}). The two must
   * agree exactly, or a thread would initialize a copy nobody ever reads.
   */
  public static String perThreadName(int pThreadId, String pQualifiedName) {
    return "T%d_%s".formatted(pThreadId, pQualifiedName);
  }

  /**
   * Every {@code __thread}/{@code _Thread_local} variable declared in {@code pCfa}, in declaration
   * order. Each of these lives at file scope but has one private copy per thread, so both
   * concurrent analyses privatize it (see {@link #perThreadName}) and must then initialize the
   * copy of every thread they spawn: a spawned thread starts at its start routine's entry and
   * never executes the file-scope declaration edge, which only exists in the main thread's clone,
   * so without an injected initialization its copy would be indeterminate rather than zero.
   *
   * <p>Scanning the CFA is the only way to find these: no analysis has a ready accessor for global
   * declarations. Callers are expected to do this once and cache it, never per edge.
   */
  public static ImmutableList<CVariableDeclaration> threadLocalGlobals(CFA pCfa) {
    // the same declaration is reachable through several edges (and appears in every function's
    // clone), so deduplicate on the qualified name, which names the object
    Map<String, CVariableDeclaration> byName = new LinkedHashMap<>();
    for (CFAEdge edge : pCfa.edges()) {
      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration declaration
          && declaration.isGlobal()
          && declaration.isThreadLocal()) {
        byName.putIfAbsent(declaration.getQualifiedName(), declaration);
      }
    }
    return ImmutableList.copyOf(byName.values());
  }

  /**
   * The value a freshly spawned thread's private copy of {@code pDeclaration} starts out with: the
   * declaration's own initializer, or the type's zero when it has none (C zero-initializes static
   * storage, §6.7.9 (10) — which is what makes {@code __thread int data = 0;} and {@code __thread
   * int data;} behave identically, and what the spurious-race benchmark relies on).
   *
   * <p>Only a scalar with a literal initializer is supported. An aggregate is rejected rather than
   * approximated: its copy would be a whole memory region, not one value symbol, so the
   * single-assignment injection the callers perform could not initialize it and would silently
   * leave it indeterminate — reporting a violation that cannot happen, or worse, missing one.
   *
   * @throws UnsupportedCodeException if the declaration is an aggregate, has an incomplete type, is
   *     {@code extern}, or its initializer is not a literal
   */
  public static CExpression threadLocalInitValue(
      CVariableDeclaration pDeclaration, MachineModel pMachineModel, @Nullable CFAEdge pEdge)
      throws UnsupportedCodeException {
    CType type = pDeclaration.getType();
    if (!isScalar(type)) {
      throw new UnsupportedCodeException(
          "thread-local variable of non-scalar type: " + pDeclaration.getQualifiedName(), pEdge);
    }
    CInitializer initializer = pDeclaration.getInitializer();
    if (initializer == null) {
      if (pDeclaration.getCStorageClass() == CStorageClass.EXTERN) {
        // defined in another translation unit: its initial value is whatever that definition says,
        // which is not necessarily zero, so it must not be invented here
        throw new UnsupportedCodeException(
            "extern thread-local variable with unknown initial value: "
                + pDeclaration.getQualifiedName(),
            pEdge);
      }
      initializer = CDefaults.forType(pMachineModel, type, pDeclaration.getFileLocation());
    }
    if (initializer instanceof CInitializerExpression initializerExpression
        && stripCasts(initializerExpression.getExpression()) instanceof CLiteralExpression) {
      return initializerExpression.getExpression();
    }
    throw new UnsupportedCodeException(
        "thread-local variable with a non-literal initializer: " + pDeclaration.getQualifiedName(),
        pEdge);
  }

  /** Whether an object of this type is one value, as opposed to a region of several cells. */
  private static boolean isScalar(CType pType) {
    CType canonical = pType.getCanonicalType();
    return !(canonical instanceof CArrayType
        || canonical instanceof CCompositeType
        || canonical instanceof CElaboratedType);
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
