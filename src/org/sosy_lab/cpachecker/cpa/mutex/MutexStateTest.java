// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock.MutexLockType;

public class MutexStateTest {

  private static final CProblemType PROBLEM_TYPE = new CProblemType("<mutex-test-type>");
  private static final CPointerType POINTER_TO_PROBLEM_TYPE =
      new CPointerType(CTypeQualifiers.NONE, PROBLEM_TYPE);

  private static CVariableDeclaration variable(String qualifiedName, CType type) {
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        /* pIsGlobal= */ true,
        CStorageClass.AUTO,
        type,
        qualifiedName,
        qualifiedName,
        qualifiedName,
        /* pInitializer= */ null);
  }

  /**
   * Builds a fresh AST for {@code &arrayDecl[index].fieldName}: a new set of expression nodes every
   * call, as two separate occurrences of the same source text (e.g. at a lock call site and an
   * unlock call site) would each get their own parsed argument expression, referencing the same
   * underlying array variable declaration.
   */
  private static CExpression addressOfArrayFieldAccess(
      CVariableDeclaration arrayDecl, int index, String fieldName) {
    CIdExpression arrayId = new CIdExpression(FileLocation.DUMMY, arrayDecl);
    CArraySubscriptExpression subscript =
        new CArraySubscriptExpression(
            FileLocation.DUMMY,
            PROBLEM_TYPE,
            arrayId,
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(index)));
    CFieldReference field =
        new CFieldReference(
            FileLocation.DUMMY,
            PROBLEM_TYPE,
            fieldName,
            subscript,
            /* pIsPointerDereference= */ false);
    return new CUnaryExpression(
        FileLocation.DUMMY, POINTER_TO_PROBLEM_TYPE, field, UnaryOperator.AMPER);
  }

  /**
   * Same as {@link #addressOfArrayFieldAccess}, but the array index is itself a variable (e.g. a
   * loop counter) instead of an integer literal, so the accessed storage location can differ across
   * evaluations and no single canonical key can describe it.
   */
  private static CExpression addressOfSymbolicIndexArrayFieldAccess(
      CVariableDeclaration arrayDecl, CVariableDeclaration indexDecl, String fieldName) {
    CIdExpression arrayId = new CIdExpression(FileLocation.DUMMY, arrayDecl);
    CIdExpression indexId = new CIdExpression(FileLocation.DUMMY, indexDecl);
    CArraySubscriptExpression subscript =
        new CArraySubscriptExpression(FileLocation.DUMMY, PROBLEM_TYPE, arrayId, indexId);
    CFieldReference field =
        new CFieldReference(
            FileLocation.DUMMY,
            PROBLEM_TYPE,
            fieldName,
            subscript,
            /* pIsPointerDereference= */ false);
    return new CUnaryExpression(
        FileLocation.DUMMY, POINTER_TO_PROBLEM_TYPE, field, UnaryOperator.AMPER);
  }

  /**
   * Builds a {@code functionName(firstArgument)} CFA edge, e.g. to simulate {@code
   * pthread_mutex_lock(&cache[i].refs_mutex)}.
   */
  private static CFAEdge functionCallEdge(String functionName, CExpression firstArgument) {
    CFunctionDeclaration functionDecl =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            CFunctionType.NO_ARGS_VOID_FUNCTION,
            functionName,
            ImmutableList.of(),
            ImmutableSet.of());
    CFunctionCallExpression callExpr =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            PROBLEM_TYPE,
            new CIdExpression(FileLocation.DUMMY, functionDecl),
            ImmutableList.of(firstArgument),
            functionDecl);
    CFunctionCallStatement callStmt = new CFunctionCallStatement(FileLocation.DUMMY, callExpr);
    return new CStatementEdge(
        functionName + "(...)",
        callStmt,
        FileLocation.DUMMY,
        CFANode.newDummyCFANode(),
        CFANode.newDummyCFANode());
  }

  @Test
  public void twoReadersOnSameRwlockDoNotCrash() {
    MutexLock read = new MutexLock(new MutexHandle("rwlock"), MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit(new MutexHandle("rwlock"));
    state = state.withLock(read, 1).get();
    // A 2nd concurrent reader used to throw IllegalArgumentException("Multiple entries with
    // same key") because withLock's builder put()'d the already-putAll()'d key again.
    state = state.withLock(read, 2).get();

    assertThat(state.getHolders(read)).containsExactly(1, 2);
  }

  @Test
  public void unlockingOneReaderKeepsTheOtherLocked() {
    MutexLock read = new MutexLock(new MutexHandle("rwlock"), MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit(new MutexHandle("rwlock"));
    state = state.withLock(read, 1).get();
    state = state.withLock(read, 2).get();
    state = state.withUnlock(read, 1);

    assertThat(state.getHolders(read)).containsExactly(2);
    assertThat(state.isMutexBlockedFor(read, 2)).isFalse();
    assertThat(state.isMutexBlockedFor(read, 3)).isFalse();
    assertThat(
            state.isMutexBlockedFor(
                new MutexLock(new MutexHandle("rwlock"), MutexLockType.WRITE), 3))
        .isTrue();
  }

  @Test
  public void unlockingLastReaderRemovesTheEntry() {
    MutexLock read = new MutexLock(new MutexHandle("rwlock"), MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit(new MutexHandle("rwlock"));
    state = state.withLock(read, 1).get();
    state = state.withUnlock(read, 1);

    assertThat(state.isLocked(read)).isFalse();
  }

  @Test
  public void arrayFieldMutex_isNotResolved_evenWithConstantIndex() {
    // `&cache[0].refs_mutex`: extractMutexName only canonicalises a plain identifier and the
    // address of one, so a field of an array element is not resolved even though its index is a
    // compile-time constant. Such a handle is therefore never a mutex-handle candidate, and the
    // lock/unlock pair is not tracked rather than tracked imprecisely.
    CVariableDeclaration cacheDecl = variable("cache", PROBLEM_TYPE);

    assertThat(
            MutexFunctions.extractMutexName(addressOfArrayFieldAccess(cacheDecl, 0, "refs_mutex")))
        .isEmpty();
  }

  @Test
  public void arrayFieldMutex_withSymbolicIndex_isNotResolved() {
    CVariableDeclaration cacheDecl = variable("cache", PROBLEM_TYPE);
    CVariableDeclaration indexDecl = variable("i", CNumericTypes.INT);
    CExpression symbolicMutexExpr =
        addressOfSymbolicIndexArrayFieldAccess(cacheDecl, indexDecl, "refs_mutex");

    // A runtime-computed index (e.g. a loop variable) can denote a different storage location on
    // every evaluation, so no canonical key can be computed statically.
    assertThat(MutexFunctions.extractMutexName(symbolicMutexExpr)).isEmpty();
  }

  @Test
  public void unresolvableMutexCall_isRejected() {
    // `pthread_mutex_lock(&(cache[i]).refs_mutex)` with a runtime index: the handle cannot be
    // determined statically. The mutex CPA does not silently treat such an edge as a non-mutex
    // operation, which would drop the lock; it refuses it instead, so a caller that reaches one
    // without pre-filtering it through the handle candidates fails loudly.
    CVariableDeclaration cacheDecl = variable("cache", PROBLEM_TYPE);
    CVariableDeclaration indexDecl = variable("i", CNumericTypes.INT);
    CExpression symbolicMutexExpr =
        addressOfSymbolicIndexArrayFieldAccess(cacheDecl, indexDecl, "refs_mutex");
    CFAEdge lockEdge = functionCallEdge("pthread_mutex_lock", symbolicMutexExpr);

    assertThrows(UnsupportedOperationException.class, () -> MutexFunctions.getLockMutex(lockEdge));
    assertThrows(UnsupportedOperationException.class, () -> MutexFunctions.isLockCall(lockEdge));
  }

  @Test
  public void mutexLock_rejectsNullHandle() {
    assertThrows(NullPointerException.class, () -> new MutexLock(null, MutexLockType.BOTH));
  }
}
