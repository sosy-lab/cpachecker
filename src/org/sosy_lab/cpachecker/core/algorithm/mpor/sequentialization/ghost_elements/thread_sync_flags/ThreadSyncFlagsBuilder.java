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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record ThreadSyncFlagsBuilder(
    MPOROptions options,
    ImmutableList<MPORThread> threads,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  public ThreadSyncFlags buildThreadSyncFlags() throws UnrecognizedCodeException {
    ImmutableSet<SeqMemoryLocation> condMemoryLocations =
        getMemoryLocationsByObjectType(PthreadObjectType.PTHREAD_COND_T);
    ImmutableSet<SeqMemoryLocation> mutexMemoryLocations =
        getMemoryLocationsByObjectType(PthreadObjectType.PTHREAD_MUTEX_T);
    ImmutableSet<SeqMemoryLocation> rwLockMemoryLocations =
        getMemoryLocationsByObjectType(PthreadObjectType.PTHREAD_RWLOCK_T);
    return new ThreadSyncFlags(
        buildCondSignaledFlags(condMemoryLocations),
        buildMutexLockedFlags(mutexMemoryLocations),
        buildRwLockFlags(rwLockMemoryLocations),
        buildSyncFlags());
  }

  private ImmutableSet<SeqMemoryLocation> getMemoryLocationsByObjectType(
      PthreadObjectType pObjectType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemoryLocations = ImmutableSet.builder();
    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();

    for (MPORThread thread : threads) {
      for (CFAEdgeForThread threadEdge : thread.cfa().threadEdges) {
        SubstituteEdge substituteEdge = Objects.requireNonNull(substituteEdges.get(threadEdge));
        if (substituteEdge.cfaEdge instanceof CDeclarationEdge declarationEdge) {
          if (declarationEdge.getDeclaration()
              instanceof CVariableDeclaration variableDeclaration) {
            CType type = variableDeclaration.getType();
            if (!(type instanceof CPointerType)) {
              if (SeqPointerAliasingUtil.isAnyTypeTargetName(type, pObjectType.name, stopNames)) {
                if (pObjectType.equalsType(type)) {
                  rMemoryLocations.add(
                      SeqMemoryLocation.of(substituteEdge.getCallContext(), variableDeclaration));
                }
                for (CCompositeTypeMemberDeclaration declaration :
                    SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationsByTypeName(
                        type, pObjectType.name)) {
                  rMemoryLocations.add(
                      SeqMemoryLocation.of(
                          substituteEdge.getCallContext(), variableDeclaration, declaration));
                }
              }
            }
          }
        }
      }
    }

    return rMemoryLocations.build();
  }

  // Private Builder Methods =======================================================================

  private ImmutableMap<SeqMemoryLocation, CondSignaledFlag> buildCondSignaledFlags(
      ImmutableSet<SeqMemoryLocation> pCondMemoryLocations) throws UnrecognizedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, CondSignaledFlag> rCondSignaledFlags =
        ImmutableMap.builder();
    for (SeqMemoryLocation condMemoryLocation : pCondMemoryLocations) {
      String variableName = buildCondSignaledName(condMemoryLocation.getName());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression condSignaled =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, variableName, SeqInitializers.INT_0);
      CBinaryExpression isSignaledExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              condSignaled, CIntegerLiteralExpression.ONE, BinaryOperator.EQUALS);
      rCondSignaledFlags.put(
          condMemoryLocation, new CondSignaledFlag(condSignaled, isSignaledExpression));
    }
    return rCondSignaledFlags.buildOrThrow();
  }

  /**
   * Links the {@link CIdExpression}s of {@code pthread_mutex_t} to their {@link MutexLockedFlag}
   * ghost variables. We use {@link CIdExpression} of substituted expressions instead of {@link
   * CVariableDeclaration} due to call-context sensitivity.
   */
  private ImmutableMap<SeqMemoryLocation, MutexLockedFlag> buildMutexLockedFlags(
      ImmutableSet<SeqMemoryLocation> pMutexExpressions) throws UnrecognizedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, MutexLockedFlag> rMutexLockedFlags =
        ImmutableMap.builder();
    for (SeqMemoryLocation mutexMemoryLocation : pMutexExpressions) {
      String variableName = buildMutexLockedName(mutexMemoryLocation.getName());
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression mutexLocked =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, variableName, SeqInitializers.INT_0);
      CBinaryExpression isLockedExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              mutexLocked, CIntegerLiteralExpression.ONE, BinaryOperator.EQUALS);
      CBinaryExpression notLockedExpression =
          binaryExpressionBuilder.buildBinaryExpression(
              mutexLocked, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
      rMutexLockedFlags.put(
          mutexMemoryLocation,
          new MutexLockedFlag(mutexLocked, isLockedExpression, notLockedExpression));
    }
    return rMutexLockedFlags.buildOrThrow();
  }

  private ImmutableMap<SeqMemoryLocation, RwLockNumReadersWritersFlag> buildRwLockFlags(
      ImmutableSet<SeqMemoryLocation> pRwLockMemoryLocations) throws UnrecognizedCodeException {

    ImmutableMap.Builder<SeqMemoryLocation, RwLockNumReadersWritersFlag> rFlags =
        ImmutableMap.builder();
    for (SeqMemoryLocation rwLockMemoryLocation : pRwLockMemoryLocations) {
      String readersVariableName = buildRwLockReadersName(rwLockMemoryLocation.getName());
      String writersVariableName = buildRwLockWritersName(rwLockMemoryLocation.getName());
      // use int (32 bit), we increment the READERS flag for every rdlock
      CIdExpression readersIdExpression =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_INT, readersVariableName, SeqInitializers.INT_0);
      // use unsigned char (8 bit), we only need values 0 and 1 (only one writer at a time)
      CIdExpression writersIdExpression =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              true, CNumericTypes.UNSIGNED_CHAR, writersVariableName, SeqInitializers.INT_0);

      CBinaryExpression readersEqualsZero =
          binaryExpressionBuilder.buildBinaryExpression(
              readersIdExpression, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
      CBinaryExpression writersEqualsZero =
          binaryExpressionBuilder.buildBinaryExpression(
              writersIdExpression, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);

      CExpressionAssignmentStatement readersIncrement =
          SeqStatementBuilder.buildIncrementStatement(readersIdExpression, binaryExpressionBuilder);
      CExpressionAssignmentStatement readersDecrement =
          SeqStatementBuilder.buildDecrementStatement(readersIdExpression, binaryExpressionBuilder);

      rFlags.put(
          rwLockMemoryLocation,
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
