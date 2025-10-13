// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType.PTHREAD_COND_WAIT;
import static org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType.PTHREAD_MUTEX_LOCK;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ThreadSyncFlagsBuilder {

  // Public Interface ==============================================================================

  public static ThreadSyncFlags buildThreadSyncFlags(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return new ThreadSyncFlags(
        buildCondSignaledFlags(pThreads, pSubstituteEdges, pBinaryExpressionBuilder),
        buildMutexLockedFlags(pOptions, pThreads, pSubstituteEdges, pBinaryExpressionBuilder),
        buildSyncFlags(pOptions, pThreads));
  }

  // Private Builder Methods =======================================================================

  /**
   * Links the {@link CIdExpression}s of {@code pthread_mutex_t} to their {@link MutexLockedFlag}
   * ghost variables. We use {@link CIdExpression} of substituted expressions instead of {@link
   * CVariableDeclaration} due to call-context sensitivity.
   */
  private static ImmutableMap<CIdExpression, MutexLockedFlag> buildMutexLockedFlags(
      MPOROptions pOptions,
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Map<CIdExpression, MutexLockedFlag> rMutexLockedFlags = new HashMap<>();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          CFAEdge cfaEdge = substituteEdge.cfaEdge;

          // extract pthread_mutex_t based on function calls to pthread_mutex_lock
          if (PthreadUtil.isCallToPthreadFunction(cfaEdge, PTHREAD_MUTEX_LOCK)) {
            CIdExpression pthreadMutexT = PthreadUtil.extractPthreadMutexT(threadEdge.cfaEdge);
            if (!rMutexLockedFlags.containsKey(pthreadMutexT)) { // add mutex only once
              String varName = SeqNameUtil.buildMutexLockedName(pOptions, pthreadMutexT.getName());
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
                  pthreadMutexT,
                  new MutexLockedFlag(mutexLocked, isLockedExpression, notLockedExpression));
            }
          }
        }
      }
    }
    return ImmutableMap.copyOf(rMutexLockedFlags);
  }

  private static ImmutableMap<CIdExpression, CondSignaledFlag> buildCondSignaledFlags(
      ImmutableList<MPORThread> pThreads,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    Map<CIdExpression, CondSignaledFlag> rCondSignaledFlags = new HashMap<>();
    for (MPORThread thread : pThreads) {
      for (ThreadEdge threadEdge : thread.cfa.threadEdges) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          CFAEdge cfaEdge = substituteEdge.cfaEdge;

          // extract pthread_cond_t based on function calls to pthread_cond_wait
          if (PthreadUtil.isCallToPthreadFunction(cfaEdge, PTHREAD_COND_WAIT)) {
            CIdExpression pthreadCondT = PthreadUtil.extractPthreadCondT(threadEdge.cfaEdge);
            if (!rCondSignaledFlags.containsKey(pthreadCondT)) { // add cond only once
              String varName = SeqNameUtil.buildCondSignaledName(pthreadCondT.getName());
              // use unsigned char (8 bit), we only need values 0 and 1
              CIdExpression condSignaled =
                  SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
                      true, CNumericTypes.UNSIGNED_CHAR, varName, SeqInitializer.INT_0);
              CBinaryExpression isSignaledExpression =
                  pBinaryExpressionBuilder.buildBinaryExpression(
                      condSignaled, SeqIntegerLiteralExpression.INT_1, BinaryOperator.EQUALS);
              rCondSignaledFlags.put(
                  pthreadCondT, new CondSignaledFlag(condSignaled, isSignaledExpression));
            }
          }
        }
      }
    }
    return ImmutableMap.copyOf(rCondSignaledFlags);
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
