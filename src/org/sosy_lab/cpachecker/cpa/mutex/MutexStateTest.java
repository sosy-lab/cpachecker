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
    MutexLock read = new MutexLock("rwlock", MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit("rwlock");
    state = state.withLock(read, 1);
    // A 2nd concurrent reader used to throw IllegalArgumentException("Multiple entries with
    // same key") because withLock's builder put()'d the already-putAll()'d key again.
    state = state.withLock(read, 2);

    assertThat(state.getHolders(read)).containsExactly(1, 2);
  }

  @Test
  public void unlockingOneReaderKeepsTheOtherLocked() {
    MutexLock read = new MutexLock("rwlock", MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit("rwlock");
    state = state.withLock(read, 1);
    state = state.withLock(read, 2);
    state = state.withUnlock(read, 1);

    assertThat(state.getHolders(read)).containsExactly(2);
    assertThat(state.isMutexBlockedFor(read, 2)).isFalse();
    assertThat(state.isMutexBlockedFor(read, 3)).isFalse();
    assertThat(state.isMutexBlockedFor(new MutexLock("rwlock", MutexLockType.WRITE), 3)).isTrue();
  }

  @Test
  public void unlockingLastReaderRemovesTheEntry() {
    MutexLock read = new MutexLock("rwlock", MutexLockType.READ);

    MutexState state = MutexState.EMPTY.withInit("rwlock");
    state = state.withLock(read, 1);
    state = state.withUnlock(read, 1);

    assertThat(state.isLocked(read)).isFalse();
  }

  @Test
  public void arrayFieldMutex_lockAndUnlockSiteResolveToSameKey() {
    // Mirrors real code like `pthread_mutex_lock(&(cache[i]).refs_mutex)`, but with a
    // compile-time-constant index, e.g. `&cache[0].refs_mutex`: the lock and unlock call sites
    // each parse their own (structurally identical, but not object-identical) argument
    // expression, and MutexFunctions must resolve both to the same canonical key so the unlock
    // actually clears the lock the earlier lock call set.
    CVariableDeclaration cacheDecl = variable("cache", PROBLEM_TYPE);

    String lockKey =
        MutexFunctions.extractMutexName(addressOfArrayFieldAccess(cacheDecl, 0, "refs_mutex"));
    String unlockKey =
        MutexFunctions.extractMutexName(addressOfArrayFieldAccess(cacheDecl, 0, "refs_mutex"));

    assertThat(lockKey).isNotNull();
    assertThat(unlockKey).isEqualTo(lockKey);

    MutexLock lock = new MutexLock(lockKey, MutexLockType.BOTH);
    MutexLock unlock = new MutexLock(unlockKey, MutexLockType.BOTH);
    assertThat(unlock).isEqualTo(lock);

    MutexState state = MutexState.EMPTY.withInit(lockKey);
    state = state.withLock(lock, 1);
    assertThat(state.isLocked(lock)).isTrue();

    state = state.withUnlock(unlock, 1);
    assertThat(state.isLocked(lock)).isFalse();
  }

  @Test
  public void arrayFieldMutex_withSymbolicIndex_isNotResolved() {
    CVariableDeclaration cacheDecl = variable("cache", PROBLEM_TYPE);
    CVariableDeclaration indexDecl = variable("i", CNumericTypes.INT);
    CExpression symbolicMutexExpr =
        addressOfSymbolicIndexArrayFieldAccess(cacheDecl, indexDecl, "refs_mutex");

    // A runtime-computed index (e.g. a loop variable) can denote a different storage location on
    // every evaluation, so no canonical key can be computed statically.
    assertThat(MutexFunctions.extractMutexName(symbolicMutexExpr)).isNull();
  }

  @Test
  public void unresolvableMutexCall_yieldsNoMutexLock_notANullHandleOne() {
    // Regression test for the crash this fix addresses: `pthread_mutex_lock(&(cache[i]).mutex)`
    // (sv-benchmarks goblint-regression/28-race_reach_73-funloop_hard_racefree.i) used to make
    // getMutexLockForFunctionSet build `new MutexLock(null, BOTH)`, which then blew up later with
    // a NullPointerException in MutexState#withUnlock
    // (entry.getKey().handle().equals(mutex.handle()) on a null handle).
    CVariableDeclaration cacheDecl = variable("cache", PROBLEM_TYPE);
    CVariableDeclaration indexDecl = variable("i", CNumericTypes.INT);
    CExpression symbolicMutexExpr =
        addressOfSymbolicIndexArrayFieldAccess(cacheDecl, indexDecl, "refs_mutex");
    CFAEdge lockEdge = functionCallEdge("pthread_mutex_lock", symbolicMutexExpr);

    // The sound fallback: this edge is simply not recognized as a mutex operation at all, rather
    // than producing a MutexLock with a null handle.
    assertThat(MutexFunctions.getLockMutex(lockEdge)).isNull();
    assertThat(MutexFunctions.isLockCall(lockEdge)).isFalse();

    // MutexState#update must likewise treat it as a no-op (edge not recognized), not crash.
    MutexState state = MutexState.EMPTY;
    assertThat(state.update(lockEdge, 1)).isEqualTo(state);
  }

  @Test
  public void mutexLock_rejectsNullHandle() {
    assertThrows(NullPointerException.class, () -> new MutexLock(null, MutexLockType.BOTH));
  }
}
