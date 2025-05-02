// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.CToSeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalAndExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.MutexLocked;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadBeginsAtomic;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadJoinsThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadLocksMutex;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
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

  public static SeqFunctionCallExpression buildNextThreadAssumption(
      boolean pIsSigned,
      CIdExpression pNumThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return new SeqFunctionCallExpression(
        SeqIdExpression.ASSUME,
        buildNextThreadAssumptionExpression(pIsSigned, pNumThreads, pBinaryExpressionBuilder));
  }

  public static ImmutableList<LineOfCode> buildSingleLoopAssumptions(
      ImmutableListMultimap<MPORThread, SeqAssumption> pThreadAssumptions) {

    ImmutableList.Builder<LineOfCode> rAssumptions = ImmutableList.builder();
    for (SeqAssumption assumption : pThreadAssumptions.values()) {
      // for single loops, we use the entire OR expression
      SeqFunctionCallExpression assumeCall =
          SeqAssumptionBuilder.buildAssumeCall(assumption.toLogicalOrExpression());
      rAssumptions.add(LineOfCode.of(2, assumeCall.toASTString() + SeqSyntax.SEMICOLON));
    }
    return rAssumptions.build();
  }

  /** Returns the expression {@code 0 <= next_thread && next_thread < NUM_THREADS} */
  private static ImmutableList<SeqExpression> buildNextThreadAssumptionExpression(
      boolean pIsSigned,
      CIdExpression pNumThreads,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqExpression> rParameters = ImmutableList.builder();
    // next_thread < NUM_THREADS is used for both signed and unsigned
    CBinaryExpression nextThreadLessThanNumThreads =
        pBinaryExpressionBuilder.buildBinaryExpression(
            SeqIdExpression.NEXT_THREAD, pNumThreads, BinaryOperator.LESS_THAN);
    rParameters.add(
        pIsSigned
            ? new SeqLogicalAndExpression(
                pBinaryExpressionBuilder.buildBinaryExpression(
                    SeqIntegerLiteralExpression.INT_0,
                    SeqIdExpression.NEXT_THREAD,
                    BinaryOperator.LESS_EQUAL),
                nextThreadLessThanNumThreads)
            : new CToSeqExpression(nextThreadLessThanNumThreads));
    return rParameters.build();
  }

  public static SeqStatement buildPcNextThreadAssumption(
      MPOROptions pOptions,
      int pNumThreads,
      boolean pScalarPc,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pScalarPc) {
      // scalar pc int: switch statement with individual case i: assume(pci != -1);
      ImmutableList.Builder<SeqCaseClause> assumeCaseClauses = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        // ensure pc
        Verify.verify(pPcVariables.get(i) instanceof CIdExpression);
        SeqFunctionCallStatement assumeCall =
            new SeqFunctionCallStatement(
                new SeqFunctionCallExpression(
                    SeqIdExpression.ASSUME,
                    ImmutableList.of(
                        new CToSeqExpression(
                            pBinaryExpressionBuilder.buildBinaryExpression(
                                pPcVariables.get(i),
                                SeqIntegerLiteralExpression.INT_EXIT_PC,
                                BinaryOperator.NOT_EQUALS)))));
        assumeCaseClauses.add(
            new SeqCaseClause(
                pOptions,
                false,
                false,
                Optional.empty(),
                i,
                new SeqCaseBlock(
                    ImmutableList.of(
                        SeqCaseBlockStatementBuilder.buildScalarPcAssumeStatement(assumeCall)))));
      }
      return new SeqSwitchStatement(
          pOptions, SeqIdExpression.NEXT_THREAD, assumeCaseClauses.build(), 0);
    } else {
      // pc array: single assume(pc[next_thread] != -1);
      return new SeqFunctionCallStatement(
          new SeqFunctionCallExpression(
              SeqIdExpression.ASSUME,
              ImmutableList.of(
                  new CToSeqExpression(
                      pBinaryExpressionBuilder.buildBinaryExpression(
                          SeqExpressionBuilder.buildPcSubscriptExpression(
                              SeqIdExpression.NEXT_THREAD),
                          SeqIntegerLiteralExpression.INT_EXIT_PC,
                          BinaryOperator.NOT_EQUALS)))));
    }
  }
}
