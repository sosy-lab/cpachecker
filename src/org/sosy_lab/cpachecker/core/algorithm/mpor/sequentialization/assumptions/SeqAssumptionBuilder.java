// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalOrExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumptionBuilder {

  /**
   * Creates assume function calls to handle total strict orders (TSOs) induced by thread
   * simulations and pthread methods inside the sequentialization.
   */
  public static ImmutableList<SeqFunctionCallExpression> createThreadSimulationAssumptions(
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAssumptions = ImmutableList.builder();

    // add all assumptions to simulate threads
    rAssumptions.addAll(
        buildMutexAssumptions(pThreadSimulationVariables, pBinaryExpressionBuilder));
    rAssumptions.addAll(
        buildJoinAssumptions(pPcVariables, pThreadSimulationVariables, pBinaryExpressionBuilder));
    rAssumptions.addAll(
        buildAtomicAssumptions(pThreadSimulationVariables, pBinaryExpressionBuilder));

    return rAssumptions.build();
  }

  /** Assumptions over mutexes: {@code assume(!(m_locked && ti_locks_m) || next_thread != i)} */
  private static ImmutableList<SeqFunctionCallExpression> buildMutexAssumptions(
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rMutexAssumptions = ImmutableList.builder();

    for (var lockedEntry : pThreadSimulationVariables.locked.entrySet()) {
      CIdExpression pthreadMutexT = lockedEntry.getKey();
      MutexLocked locked = lockedEntry.getValue();
      // search for the awaits variable corresponding to pthreadMutexT
      for (var awaitsEntry : pThreadSimulationVariables.locks.entrySet()) {
        MPORThread thread = awaitsEntry.getKey();
        for (var awaitsValue : awaitsEntry.getValue().entrySet()) {
          if (pthreadMutexT.equals(awaitsValue.getKey())) {
            ThreadLocksMutex awaits = awaitsValue.getValue();
            SeqLogicalNotExpression notLockedAndAwaits =
                new SeqLogicalNotExpression(
                    new SeqLogicalAndExpression(locked.idExpression, awaits.idExpression));
            CToSeqExpression nextThreadNotId =
                new CToSeqExpression(
                    SeqExpressionBuilder.buildNextThreadUnequal(
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
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rJoinAssumptions = ImmutableList.builder();

    for (var join : pThreadSimulationVariables.joins.entrySet()) {
      MPORThread jThread = join.getKey();
      for (var joinValue : join.getValue().entrySet()) {
        MPORThread iThread = joinValue.getKey();
        ThreadJoinsThread joinVar = joinValue.getValue();
        SeqLogicalNotExpression notActiveAndJoins =
            new SeqLogicalNotExpression(
                new SeqLogicalAndExpression(
                    SeqExpressionBuilder.buildPcUnequalExitPc(
                        pPcVariables, iThread.id, pBinaryExpressionBuilder),
                    joinVar.idExpression));
        CToSeqExpression nextThreadNotId =
            new CToSeqExpression(
                SeqExpressionBuilder.buildNextThreadUnequal(jThread.id, pBinaryExpressionBuilder));
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
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqFunctionCallExpression> rAtomicAssumptions = ImmutableList.builder();

    for (var entry : pThreadSimulationVariables.begins.entrySet()) {
      assert pThreadSimulationVariables.atomicLocked.isPresent();
      MPORThread thread = entry.getKey();
      ThreadBeginsAtomic begins = entry.getValue();
      SeqLogicalNotExpression notAtomicLockedAndBegins =
          new SeqLogicalNotExpression(
              new SeqLogicalAndExpression(
                  pThreadSimulationVariables.atomicLocked.orElseThrow().idExpression,
                  begins.idExpression));
      CToSeqExpression nextThreadNotId =
          new CToSeqExpression(
              SeqExpressionBuilder.buildNextThreadUnequal(thread.id, pBinaryExpressionBuilder));
      SeqLogicalOrExpression assumption =
          new SeqLogicalOrExpression(notAtomicLockedAndBegins, nextThreadNotId);
      SeqFunctionCallExpression assumeCall =
          new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(assumption));
      rAtomicAssumptions.add(assumeCall);
    }
    return rAtomicAssumptions.build();
  }
}
