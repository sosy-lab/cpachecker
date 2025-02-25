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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadSimulationVariables;
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
      GhostPcVariables pPcVariables,
      GhostThreadSimulationVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();

    // add all assumptions to simulate threads
    rAssumptions.addAll(buildMutexAssumptions(pThreadVariables, pBinaryExpressionBuilder));
    rAssumptions.addAll(
        buildJoinAssumptions(pPcVariables, pThreadVariables, pBinaryExpressionBuilder));
    rAssumptions.addAll(buildAtomicAssumptions(pThreadVariables, pBinaryExpressionBuilder));

    return rAssumptions.build();
  }

  /** Assumptions over mutexes: {@code assume(!(m_locked && ti_locks_m) || next_thread != i)} */
  private static ImmutableList<SeqFunctionCallExpression> buildMutexAssumptions(
      GhostThreadSimulationVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

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
                new CToSeqExpression(
                    SeqBinaryExpression.buildNextThreadUnequal(
                        thread.id, pBinaryExpressionBuilder));
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
      GhostPcVariables pPcVariables,
      GhostThreadSimulationVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rJoinAssumptions = ImmutableList.builder();

    for (var join : pThreadVariables.joins.entrySet()) {
      MPORThread jThread = join.getKey();
      for (var joinValue : join.getValue().entrySet()) {
        MPORThread iThread = joinValue.getKey();
        ThreadJoinsThread joinVar = joinValue.getValue();
        SeqLogicalNotExpression notActiveAndJoins =
            new SeqLogicalNotExpression(
                new SeqLogicalAndExpression(
                    SeqBinaryExpression.buildPcUnequalExitPc(
                        pPcVariables, iThread.id, pBinaryExpressionBuilder),
                    joinVar.idExpression));
        CToSeqExpression nextThreadNotId =
            new CToSeqExpression(
                SeqBinaryExpression.buildNextThreadUnequal(jThread.id, pBinaryExpressionBuilder));
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
      GhostThreadSimulationVariables pThreadVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

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
          new CToSeqExpression(
              SeqBinaryExpression.buildNextThreadUnequal(thread.id, pBinaryExpressionBuilder));
      SeqLogicalOrExpression assumption =
          new SeqLogicalOrExpression(notAtomicLockedAndBegins, nextThreadNotId);
      SeqFunctionCallExpression assumeCall =
          new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
      rAtomicAssumptions.add(assumeCall);
    }
    return rAtomicAssumptions.build();
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
