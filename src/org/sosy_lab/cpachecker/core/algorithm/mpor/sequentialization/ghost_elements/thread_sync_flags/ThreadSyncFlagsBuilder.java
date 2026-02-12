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
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record ThreadSyncFlagsBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> threads,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  public ThreadSyncFlags buildThreadSyncFlags() throws UnrecognizedCodeException {
    ImmutableSet<CIdExpression> condExpressions =
        getIdExpressionsByObjectType(PthreadObjectType.PTHREAD_COND_T);
    ImmutableSet<CIdExpression> mutexExpressions =
        getIdExpressionsByObjectType(PthreadObjectType.PTHREAD_MUTEX_T);
    ImmutableSet<CIdExpression> rwLockExpressions =
        getIdExpressionsByObjectType(PthreadObjectType.PTHREAD_RWLOCK_T);
    return new ThreadSyncFlags(
        buildCondSignaledFlags(condExpressions),
        buildMutexLockedFlags(mutexExpressions),
        buildRwLockFlags(rwLockExpressions),
        buildSyncFlags());
  }

  private ImmutableSet<CIdExpression> getIdExpressionsByObjectType(PthreadObjectType pObjectType)
      throws UnsupportedCodeException {
    Set<CIdExpression> rIdExpressions = new HashSet<>();
    for (MPORThread thread : threads) {
      for (CFAEdgeForThread threadEdge : thread.cfa().threadEdges) {
        Optional<CFunctionCall> functionCallOptional =
            PthreadUtil.tryGetFunctionCallFromCfaEdge(threadEdge.cfaEdge);
        if (functionCallOptional.isPresent()) {
          CFunctionCall functionCall = functionCallOptional.orElseThrow();
          if (PthreadUtil.isCallToAnyPthreadFunctionWithObjectType(functionCall, pObjectType)) {
            rIdExpressions.add(PthreadUtil.extractPthreadObject(functionCall, pObjectType));
          }
        }
      }
    }
    return ImmutableSet.copyOf(rIdExpressions);
  }

  // Private Builder Methods =======================================================================

  private ImmutableMap<CIdExpression, CondSignaledFlag> buildCondSignaledFlags(
      ImmutableSet<CIdExpression> pCondExpressions) throws UnrecognizedCodeException {

    ImmutableMap.Builder<CIdExpression, CondSignaledFlag> rCondSignaledFlags =
        ImmutableMap.builder();
    for (CIdExpression condExpression : pCondExpressions) {
      String varName = buildCondSignaledName(condExpression.getName());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression condSignaled =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, varName, SeqInitializers.INT_0);
      CBinaryExpression isSignaledExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              condSignaled, SeqIntegerLiteralExpressions.INT_1, BinaryOperator.EQUALS);
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
  private ImmutableMap<CIdExpression, MutexLockedFlag> buildMutexLockedFlags(
      ImmutableSet<CIdExpression> pMutexExpressions) throws UnrecognizedCodeException {

    ImmutableMap.Builder<CIdExpression, MutexLockedFlag> rMutexLockedFlags = ImmutableMap.builder();
    for (CIdExpression mutexExpression : pMutexExpressions) {
      String varName = buildMutexLockedName(mutexExpression.getName());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression mutexLocked =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, varName, SeqInitializers.INT_0);
      CBinaryExpression isLockedExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              mutexLocked, SeqIntegerLiteralExpressions.INT_1, BinaryOperator.EQUALS);
      CBinaryExpression notLockedExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              mutexLocked, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);
      rMutexLockedFlags.put(
          mutexExpression,
          new MutexLockedFlag(mutexLocked, isLockedExpression, notLockedExpression));
    }
    return rMutexLockedFlags.buildOrThrow();
  }

  private ImmutableMap<CIdExpression, RwLockNumReadersWritersFlag> buildRwLockFlags(
      ImmutableSet<CIdExpression> pRwLockExpressions) throws UnrecognizedCodeException {

    ImmutableMap.Builder<CIdExpression, RwLockNumReadersWritersFlag> rFlags =
        ImmutableMap.builder();
    for (CIdExpression rwLockExpression : pRwLockExpressions) {
      String readersVarName = buildRwLockReadersName(rwLockExpression.getName());
      String writersVarName = buildRwLockWritersName(rwLockExpression.getName());
      // use int (32 bit), we increment the READERS flag for every rdlock
      CIdExpression readersIdExpression =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_INT, readersVarName, SeqInitializers.INT_0);
      // use unsigned char (8 bit), we only need values 0 and 1 (only one writer at a time)
      CIdExpression writersIdExpression =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, writersVarName, SeqInitializers.INT_0);

      CBinaryExpression readersEqualsZero =
          binaryExpressionBuilder.buildBinaryExpression(
              readersIdExpression, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);
      CBinaryExpression writersEqualsZero =
          binaryExpressionBuilder.buildBinaryExpression(
              writersIdExpression, SeqIntegerLiteralExpressions.INT_0, BinaryOperator.EQUALS);

      CExpressionAssignmentStatement readersIncrement =
          SeqStatementBuilder.buildIncrementStatement(readersIdExpression, binaryExpressionBuilder);
      CExpressionAssignmentStatement readersDecrement =
          SeqStatementBuilder.buildDecrementStatement(readersIdExpression, binaryExpressionBuilder);

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

  private ImmutableMap<MPORThread, CIdExpression> buildSyncFlags() {
    ImmutableMap.Builder<MPORThread, CIdExpression> rSyncFlags = ImmutableMap.builder();
    for (MPORThread thread : threads) {
      String name = buildSyncVariableName(thread.id());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression sync =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              // TODO a thread could also start with pthread_mutex_lock -> initialize with 1
              true, CNumericTypes.UNSIGNED_CHAR, name, SeqInitializers.INT_0);
      rSyncFlags.put(thread, sync);
    }
    return rSyncFlags.buildOrThrow();
  }

  // Name Utility ==================================================================================

  private static String buildCondSignaledName(String pCondName) {
    return pCondName + "_SIGNALED";
  }

  private static String buildMutexLockedName(String pMutexName) {
    return pMutexName + "_LOCKED";
  }

  private static String buildRwLockReadersName(String pRwLockName) {
    return pRwLockName + "_NUM_READERS";
  }

  private static String buildRwLockWritersName(String pRwLockName) {
    return pRwLockName + "_NUM_WRITERS";
  }

  private String buildSyncVariableName(int pThreadId) {
    return SeqNameUtil.buildThreadPrefix(options, pThreadId) + "_SYNC";
  }
}
