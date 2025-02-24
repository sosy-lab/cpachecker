// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumptionBuilder {

  /**
   * Creates assume function calls to handle total strict orders (TSOs) induced by thread
   * simulations and pthread methods inside the sequentialization.
   */
  public static ImmutableList<SeqFunctionCallExpression> createThreadSimulationAssumptions(
      ImmutableSet<MPORThread> pThreads,
      GhostPcVariables pPcVariables,
      GhostThreadVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();

    // create expressions beforehand for all threads (e.g. pc[i] != n or next_thread != i)
    ImmutableMap<Integer, CBinaryExpression> nextThreadNotIdExpressions =
        mapNextThreadNotIdExpressions(pThreads, pBinaryExpressionBuilder);
    ImmutableMap<Integer, CBinaryExpression> pcNotExitPcExpressions =
        mapPcNotExitPcExpressions(pThreads, pPcVariables, pBinaryExpressionBuilder);

    // add all assumptions to simulate threads
    rAssumptions.addAll(buildMutexAssumptions(pThreadVariables, nextThreadNotIdExpressions));
    rAssumptions.addAll(
        buildJoinAssumptions(pThreadVariables, pcNotExitPcExpressions, nextThreadNotIdExpressions));
    rAssumptions.addAll(buildAtomicAssumptions(pThreadVariables, nextThreadNotIdExpressions));

    return rAssumptions.build();
  }

  /** Assumptions over mutexes: {@code assume(!(m_locked && ti_locks_m) || next_thread != i)} */
  private static ImmutableList<SeqFunctionCallExpression> buildMutexAssumptions(
      GhostThreadVariables pThreadVariables,
      ImmutableMap<Integer, CBinaryExpression> pNextThreadNotIdExpressions) {

    ImmutableList.Builder<SeqFunctionCallExpression> rMutexAssumptions = ImmutableList.builder();

    for (var lockedEntry : pThreadVariables.locked.entrySet()) {
      CIdExpression pthreadMutexT = lockedEntry.getKey();
      MutexLocked locked = lockedEntry.getValue();
      // search for the awaits variable corresponding to pthreadMutexT
      for (var awaitsEntry : pThreadVariables.locks.entrySet()) {
        MPORThread thread = awaitsEntry.getKey();
        for (var awaitsValue : awaitsEntry.getValue().entrySet()) {
          if (pthreadMutexT.equals(awaitsValue.getKey())) {
            ThreadLocksMutex awaits = awaitsValue.getValue();
            SeqLogicalNotExpression notLockedAndAwaits =
                new SeqLogicalNotExpression(
                    new SeqLogicalAndExpression(locked.idExpression, awaits.idExpression));
            CToSeqExpression nextThreadNotId =
                new CToSeqExpression(pNextThreadNotIdExpressions.get(thread.id));
            SeqLogicalOrExpression assumption =
                new SeqLogicalOrExpression(notLockedAndAwaits, nextThreadNotId);
            SeqFunctionCallExpression assumeCall =
                new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
            rMutexAssumptions.add(assumeCall);
          }
        }
      }
    }
    return rMutexAssumptions.build();
  }

  /** Assumptions over joins: {@code assume(!(pc[i] != -1 && tj_joins_ti) || next_thread != j)} */
  private static ImmutableList<SeqFunctionCallExpression> buildJoinAssumptions(
      GhostThreadVariables pThreadVariables,
      ImmutableMap<Integer, CBinaryExpression> pPcNotExitPcExpressions,
      ImmutableMap<Integer, CBinaryExpression> pNextThreadNotIdExpressions) {

    ImmutableList.Builder<SeqFunctionCallExpression> rJoinAssumptions = ImmutableList.builder();

    for (var join : pThreadVariables.joins.entrySet()) {
      MPORThread jThread = join.getKey();
      for (var joinValue : join.getValue().entrySet()) {
        MPORThread iThread = joinValue.getKey();
        ThreadJoinsThread joinVar = joinValue.getValue();
        SeqLogicalNotExpression notActiveAndJoins =
            new SeqLogicalNotExpression(
                new SeqLogicalAndExpression(
                    pPcNotExitPcExpressions.get(iThread.id), joinVar.idExpression));
        CToSeqExpression nextThreadNotId =
            new CToSeqExpression(pNextThreadNotIdExpressions.get(jThread.id));
        SeqLogicalOrExpression assumption =
            new SeqLogicalOrExpression(notActiveAndJoins, nextThreadNotId);
        SeqFunctionCallExpression assumeCall =
            new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
        rJoinAssumptions.add(assumeCall);
      }
    }
    return rJoinAssumptions.build();
  }

  /**
   * Atomic assumptions: {@code assume(!(atomic_locked && ti_begins_atomic) || next_thread != i)}
   */
  private static ImmutableList<SeqFunctionCallExpression> buildAtomicAssumptions(
      GhostThreadVariables pThreadVariables,
      ImmutableMap<Integer, CBinaryExpression> pNextThreadNotIdExpressions) {

    ImmutableList.Builder<SeqFunctionCallExpression> rAtomicAssumptions = ImmutableList.builder();

    for (var entry : pThreadVariables.begins.entrySet()) {
      assert pThreadVariables.atomicLocked.isPresent();
      MPORThread thread = entry.getKey();
      ThreadBeginsAtomic begins = entry.getValue();
      SeqLogicalNotExpression notAtomicLockedAndBegins =
          new SeqLogicalNotExpression(
              new SeqLogicalAndExpression(
                  pThreadVariables.atomicLocked.orElseThrow().idExpression, begins.idExpression));
      CToSeqExpression nextThreadNotId =
          new CToSeqExpression(pNextThreadNotIdExpressions.get(thread.id));
      SeqLogicalOrExpression assumption =
          new SeqLogicalOrExpression(notAtomicLockedAndBegins, nextThreadNotId);
      SeqFunctionCallExpression assumeCall =
          new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
      rAtomicAssumptions.add(assumeCall);
    }
    return rAtomicAssumptions.build();
  }

  // TODO these two should be in an ExpressionBuilder etc.

  // TODO these should be lists for indexing?
  /** Maps thread ids {@code i} to {@code next_thread != i} expressions. */
  private static ImmutableMap<Integer, CBinaryExpression> mapNextThreadNotIdExpressions(
      ImmutableSet<MPORThread> pThreads, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<Integer, CBinaryExpression> rExpressions = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      CIntegerLiteralExpression threadId =
          SeqIntegerLiteralExpression.buildIntegerLiteralExpression(thread.id);
      CBinaryExpression nextThreadNotId =
          pBinaryExpressionBuilder.buildBinaryExpression(
              SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.NOT_EQUALS);
      rExpressions.put(thread.id, nextThreadNotId);
    }
    return rExpressions.buildOrThrow();
  }

  // TODO these should be lists for indexing?
  /** Maps thread ids {@code i} to {@code pc[i] != i} expressions. */
  public static ImmutableMap<Integer, CBinaryExpression> mapPcNotExitPcExpressions(
      ImmutableSet<MPORThread> pThreads,
      GhostPcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<Integer, CBinaryExpression> rExpressions = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      CBinaryExpression nextThreadNotId =
          pBinaryExpressionBuilder.buildBinaryExpression(
              pPcVariables.get(thread.id),
              SeqIntegerLiteralExpression.INT_EXIT_PC,
              BinaryOperator.NOT_EQUALS);
      rExpressions.put(thread.id, nextThreadNotId);
    }
    return rExpressions.buildOrThrow();
  }

  // Partial Order Reduction =======================================================================

  // TODO remove this once we concatenate statements instead of using assumptions for POR
  /**
   * Creates the function calls for assumptions of the form {@code assume((prev_thread = i && pc[i]
   * = 0) ==> next_thread = i)}.
   */
  public static ImmutableList<SeqFunctionCallExpression> createPORAssumptions(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pPrunedCaseClauses,
      GhostPcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();
    for (var entry : pPrunedCaseClauses.entrySet()) {
      int threadId = entry.getKey().id;
      for (SeqCaseClause caseClause : entry.getValue()) {
        if (!caseClause.isGlobal && caseClause.alwaysUpdatesPc()) {
          rAssumptions.add(
              createPORAssumption(
                  threadId, caseClause.label.value, pPcVariables, pBinaryExpressionBuilder));
        }
      }
    }
    return rAssumptions.build();
  }

  private static SeqFunctionCallExpression createPORAssumption(
      int pThreadId,
      int pPc,
      GhostPcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CIntegerLiteralExpression threadId =
        SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pThreadId);
    CBinaryExpression prevEquals =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.PREV_THREAD, threadId, BinaryOperator.EQUALS);
    CBinaryExpression pcEquals =
        pBinaryExpressionBuilder.buildBinaryExpression(
            pPcVariables.get(pThreadId),
            SeqIntegerLiteralExpression.buildIntegerLiteralExpression(pPc),
            BinaryOperator.EQUALS);
    CToSeqExpression nextThread =
        new CToSeqExpression(
            pBinaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpression.NEXT_THREAD, threadId, BinaryOperator.EQUALS));
    SeqLogicalNotExpression notAnd =
        new SeqLogicalNotExpression(new SeqLogicalAndExpression(prevEquals, pcEquals));
    SeqLogicalOrExpression or = new SeqLogicalOrExpression(notAnd, nextThread);
    return new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(or));
  }
}
