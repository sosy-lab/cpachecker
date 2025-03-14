// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAssumptionBuilder {

  public static SeqFunctionCallExpression buildAssumeCall(SeqExpression pCondition) {
    return new SeqFunctionCallExpression(SeqIdExpression.ASSUME, ImmutableList.of(pCondition));
  }

  /**
   * Creates assume function calls to handle total strict orders (TSOs) induced by thread
   * simulations and pthread methods inside the sequentialization.
   */
  public static ImmutableListMultimap<MPORThread, SeqAssumption> createThreadSimulationAssumptions(
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqAssumption> rAssumptions =
        ImmutableListMultimap.builder();

    // add all assumptions to simulate threads
    rAssumptions.putAll(
        buildMutexAssumptions(pThreadSimulationVariables, pBinaryExpressionBuilder));
    rAssumptions.putAll(
        buildJoinAssumptions(pPcVariables, pThreadSimulationVariables, pBinaryExpressionBuilder));
    rAssumptions.putAll(
        buildAtomicAssumptions(pThreadSimulationVariables, pBinaryExpressionBuilder));

    return rAssumptions.build();
  }

  /** Assumptions over mutexes: {@code assume(!(m_locked && ti_locks_m) || next_thread != i)} */
  private static ImmutableListMultimap<MPORThread, SeqAssumption> buildMutexAssumptions(
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqAssumption> rMutexAssumptions =
        ImmutableListMultimap.builder();

    for (var lockedEntry : pThreadSimulationVariables.locked.entrySet()) {
      CIdExpression pthreadMutexT = lockedEntry.getKey();
      MutexLocked locked = lockedEntry.getValue();
      // search for the awaits variable corresponding to pthreadMutexT
      for (var awaitsEntry : pThreadSimulationVariables.locks.entrySet()) {
        MPORThread thread = awaitsEntry.getKey();
        for (var awaitsValue : awaitsEntry.getValue().entrySet()) {
          if (pthreadMutexT.equals(awaitsValue.getKey())) {
            ThreadLocksMutex awaits = awaitsValue.getValue();
            SeqLogicalNotExpression antecedent =
                new SeqLogicalNotExpression(
                    new SeqLogicalAndExpression(locked.idExpression, awaits.idExpression));
            CBinaryExpression consequent =
                SeqExpressionBuilder.buildNextThreadUnequal(thread.id, pBinaryExpressionBuilder);
            SeqAssumption assumption = new SeqAssumption(antecedent, consequent);
            rMutexAssumptions.put(thread, assumption);
          }
        }
      }
    }
    return rMutexAssumptions.build();
  }

  /** Assumptions over joins: {@code assume(!(pc[i] != -1 && tj_joins_ti) || next_thread != j)} */
  private static ImmutableListMultimap<MPORThread, SeqAssumption> buildJoinAssumptions(
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqAssumption> rJoinAssumptions =
        ImmutableListMultimap.builder();

    for (var join : pThreadSimulationVariables.joins.entrySet()) {
      MPORThread jThread = join.getKey();
      for (var joinValue : join.getValue().entrySet()) {
        MPORThread iThread = joinValue.getKey();
        ThreadJoinsThread joinVar = joinValue.getValue();
        SeqLogicalNotExpression antecedent =
            new SeqLogicalNotExpression(
                new SeqLogicalAndExpression(
                    SeqExpressionBuilder.buildPcUnequalExitPc(
                        pPcVariables, iThread.id, pBinaryExpressionBuilder),
                    joinVar.idExpression));
        CBinaryExpression consequent =
            SeqExpressionBuilder.buildNextThreadUnequal(jThread.id, pBinaryExpressionBuilder);
        SeqAssumption assumption = new SeqAssumption(antecedent, consequent);
        rJoinAssumptions.put(jThread, assumption);
      }
    }
    return rJoinAssumptions.build();
  }

  /**
   * Atomic assumptions: {@code assume(!(atomic_locked && ti_begins_atomic) || next_thread != i)}
   */
  private static ImmutableListMultimap<MPORThread, SeqAssumption> buildAtomicAssumptions(
      ThreadSimulationVariables pThreadSimulationVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqAssumption> rAtomicAssumptions =
        ImmutableListMultimap.builder();

    for (var entry : pThreadSimulationVariables.begins.entrySet()) {
      assert pThreadSimulationVariables.atomicLocked.isPresent();
      MPORThread thread = entry.getKey();
      ThreadBeginsAtomic begins = entry.getValue();
      SeqLogicalNotExpression antecedent =
          new SeqLogicalNotExpression(
              new SeqLogicalAndExpression(
                  pThreadSimulationVariables.atomicLocked.orElseThrow().idExpression,
                  begins.idExpression));
      CBinaryExpression consequent =
          SeqExpressionBuilder.buildNextThreadUnequal(thread.id, pBinaryExpressionBuilder);
      SeqAssumption assumption = new SeqAssumption(antecedent, consequent);
      rAtomicAssumptions.put(thread, assumption);
    }
    return rAtomicAssumptions.build();
  }
}
