// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ThreadSyncFlagsBuilder {

  // Public Interface ==============================================================================

  public static ThreadSyncFlags buildThreadSyncFlags(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableSet<CIdExpression> condExpressions =
        getIdExpressionsByObjectType(pThreads, PthreadObjectType.PTHREAD_COND_T);
    ImmutableSet<CIdExpression> mutexExpressions =
        getIdExpressionsByObjectType(pThreads, PthreadObjectType.PTHREAD_MUTEX_T);
    ImmutableSet<CIdExpression> rwLockExpressions =
        getIdExpressionsByObjectType(pThreads, PthreadObjectType.PTHREAD_RWLOCK_T);
    return new ThreadSyncFlags(
        buildCondSignaledFlags(condExpressions, pBinaryExpressionBuilder),
        buildMutexLockedFlags(pOptions, mutexExpressions, pBinaryExpressionBuilder),
        buildRwLockFlags(pOptions, rwLockExpressions, pBinaryExpressionBuilder),
        buildSyncFlags(pOptions, pThreads));
  }

  private static ImmutableSet<CIdExpression> getIdExpressionsByObjectType(
      ImmutableList<MPORThread> pThreads, PthreadObjectType pObjectType) {

    Set<CIdExpression> rIdExpressions = new HashSet<>();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        CFAEdge cfaEdge = threadEdge.cfaEdge;
        if (PthreadUtil.isCallToAnyPthreadFunctionWithObjectType(cfaEdge, pObjectType)) {
          rIdExpressions.add(PthreadUtil.extractPthreadObject(cfaEdge, pObjectType));
        }
      }
    }
    return ImmutableSet.copyOf(rIdExpressions);
  }

  // Private Builder Methods =======================================================================

  private static ImmutableMap<CIdExpression, CondSignaledFlag> buildCondSignaledFlags(
      ImmutableSet<CIdExpression> pCondExpressions,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CIdExpression, CondSignaledFlag> rCondSignaledFlags =
        ImmutableMap.builder();
    for (CIdExpression condExpression : pCondExpressions) {
      String varName = SeqNameUtil.buildCondSignaledName(condExpression.getName());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression condSignaled =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, varName, SeqInitializer.INT_0);
      CBinaryExpression isSignaledExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              condSignaled, SeqIntegerLiteralExpression.INT_1, BinaryOperator.EQUALS);
      rCondSignaledFlags.put(
          condExpression, new CondSignaledFlag(condSignaled, isSignaledExpression));
    }
    return rCondSignaledFlags.buildOrThrow();
  }

  /**
   * Links the {@link CIdExpression}s of {@code pthread_mutex_t} to their {@link MutexLockedFlag}
   * ghost variables. We use {@link CIdExpression} of substituted expressions instead of {@link
   * CVariableDeclaration} due to call-context sensitivity.
   */
  private static ImmutableMap<CIdExpression, MutexLockedFlag> buildMutexLockedFlags(
      MPOROptions pOptions,
      ImmutableSet<CIdExpression> pMutexExpressions,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CIdExpression, MutexLockedFlag> rMutexLockedFlags = ImmutableMap.builder();
    for (CIdExpression mutexExpression : pMutexExpressions) {
      String varName = SeqNameUtil.buildMutexLockedName(pOptions, mutexExpression.getName());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression mutexLocked =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, varName, SeqInitializer.INT_0);
      CBinaryExpression isLockedExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              mutexLocked, SeqIntegerLiteralExpression.INT_1, BinaryOperator.EQUALS);
      CBinaryExpression notLockedExpression =
          pBinaryExpressionBuilder.buildBinaryExpression(
              mutexLocked, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS);
      rMutexLockedFlags.put(
          mutexExpression,
          new MutexLockedFlag(mutexLocked, isLockedExpression, notLockedExpression));
    }
    return rMutexLockedFlags.buildOrThrow();
  }

  private static ImmutableMap<CIdExpression, RwLockNumReadersWritersFlag> buildRwLockFlags(
      MPOROptions pOptions,
      ImmutableSet<CIdExpression> pRwLockExpressions,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<CIdExpression, RwLockNumReadersWritersFlag> rFlags =
        ImmutableMap.builder();
    for (CIdExpression rwLockExpression : pRwLockExpressions) {
      String readersVarName =
          SeqNameUtil.buildRwLockReadersName(pOptions, rwLockExpression.getName());
      String writersVarName =
          SeqNameUtil.buildRwLockWritersName(pOptions, rwLockExpression.getName());
      // use int (32 bit), we increment the READERS flag for every rdlock
      CIdExpression readersIdExpression =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_INT, readersVarName, SeqInitializer.INT_0);
      // use unsigned char (8 bit), we only need values 0 and 1 (only one writer at a time)
      CIdExpression writersIdExpression =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, writersVarName, SeqInitializer.INT_0);

      CBinaryExpression readersEqualsZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              readersIdExpression, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS);
      CBinaryExpression writersEqualsZero =
          pBinaryExpressionBuilder.buildBinaryExpression(
              writersIdExpression, SeqIntegerLiteralExpression.INT_0, BinaryOperator.EQUALS);

      CExpressionAssignmentStatement readersIncrement =
          SeqStatementBuilder.buildIncrementStatement(
              readersIdExpression, pBinaryExpressionBuilder);
      CExpressionAssignmentStatement readersDecrement =
          SeqStatementBuilder.buildDecrementStatement(
              readersIdExpression, pBinaryExpressionBuilder);

      rFlags.put(
          rwLockExpression,
          new RwLockNumReadersWritersFlag(
              readersIdExpression,
              writersIdExpression,
              readersEqualsZero,
              writersEqualsZero,
              readersIncrement,
              readersDecrement));
    }
    return rFlags.buildOrThrow();
  }

  private static ImmutableMap<MPORThread, CIdExpression> buildSyncFlags(
      MPOROptions pOptions, ImmutableList<MPORThread> pThreads) {

    ImmutableMap.Builder<MPORThread, CIdExpression> rSyncFlags = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      String name = SeqNameUtil.buildSyncName(pOptions, thread.getId());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression sync =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              // TODO a thread could also start with pthread_mutex_lock -> initialize with 1
              true, CNumericTypes.UNSIGNED_CHAR, name, SeqInitializer.INT_0);
      rSyncFlags.put(thread, sync);
    }
    return rSyncFlags.buildOrThrow();
  }
}
