// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqMainFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalAndExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CMultiSelectionStatementBuilder;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

public record ReduceIgnoreSleepInjector(
    MPOROptions options,
    MPORThread activeThread,
    ImmutableSet<MPORThread> otherThreads,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    GhostElements ghostElements,
    SequentializationUtils utils) {

  public ReduceIgnoreSleepInjector {
    checkArgument(
        options.reduceIgnoreSleep(),
        "reduceIgnoreSleep must be enabled when a ReduceIgnoreSleepInjector is created.");
  }

  /**
   * Returns a {@link CIfStatement} that encodes the Ignore Sleep (IS) reduction for {@link
   * NondeterminismSource#NUM_STATEMENTS}. The statement precedes a thread simulation and takes the
   * following form:
   *
   * <pre>{@code
   * if (round_max == 0 && Ti_SYNC == 0) {
   *     assume(*Ti in at least one conflict*);
   * }
   * }</pre>
   *
   * <p>This ensures that if thread {@code i} was not chosen for execution, i.e., {@code round_max
   * == 0}, then it must be in conflict with at least one other thread. Otherwise, the simulation
   * aborts and the thread always executes and ignores that it should actually sleep.
   */
  public CIfStatement buildIgnoreSleepInstrumentation() throws UnrecognizedCodeException {
    checkState(options.nondeterminismSource().equals(NondeterminismSource.NUM_STATEMENTS));

    // (round_max == 0 && Ti_SYNC == 0)
    CExpression roundMaxEqualsZero =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.ROUND_MAX,
                SeqIntegerLiteralExpressions.INT_0,
                BinaryOperator.EQUALS);
    CExpression syncEqualsZero = buildSyncEqualsZeroExpression();
    CLogicalAndExpression logicalAnd = CLogicalAndExpression.of(roundMaxEqualsZero, syncEqualsZero);

    // assume(*Ti in at least one conflict*);
    CExportExpression bitVectorExpression = buildBitVectorEvaluationExpression();
    CExportStatement assumeCallStatement =
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(bitVectorExpression);

    return new CIfStatement(logicalAnd, new CCompoundStatement(assumeCallStatement));
  }

  /**
   * Returns a {@link CIfStatement} that encodes the Ignore Sleep (IS) reduction when the {@link
   * NondeterminismSource} is {@link NondeterminismSource#NEXT_THREAD} or {@link
   * NondeterminismSource#NEXT_THREAD_AND_NUM_STATEMENTS}. The statement is used to check whether a
   * thread can soundly be chosen deterministically. Example with two threads:
   *
   * <pre>{@code
   * if (pc1 != 0 && T1_SYNC == 0 && *T1 not in any conflict*) {
   *    next_thread = 1;
   * } else {
   *    if (pc2 != 0 && T2_SYNC == 0 && *T2 not in any conflict*) {
   *      next_thread = 2;
   *    } else {
   *      // if all threads are in at least one conflict, choose nondeterministically
   *      next_thread = nondet();
   *    }
   * }
   * }</pre>
   */
  public static CIfStatement buildNextThreadIgnoreSleepInstrumentation(
      MPOROptions pOptions, SequentializationFields pFields, SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<CExportExpression, CCompoundStatementElement> ifElseChain =
        ImmutableListMultimap.builder();

    for (MPORThread thread : pFields.threads) {
      ImmutableSet<MPORThread> otherThreads =
          MPORUtil.withoutElement(pFields.clauses.keySet(), thread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(pFields.clauses.get(thread));
      ReduceIgnoreSleepInjector reduceIgnoreSleepInjector =
          new ReduceIgnoreSleepInjector(
              pOptions, thread, otherThreads, labelClauseMap, pFields.ghostElements, pUtils);

      // create the expression used for the 'if (...)' conditions
      CExportExpression ifCondition =
          reduceIgnoreSleepInjector.buildNextThreadIgnoreSleepExpression();

      // create the 'next_thread = {thread_id};' assignment
      CExpressionAssignmentStatement nextThreadAssignment =
          SeqStatementBuilder.buildExpressionAssignmentStatement(
              SeqIdExpressions.NEXT_THREAD,
              SeqExpressionBuilder.buildIntegerLiteralExpression(thread.id()));

      ifElseChain.put(ifCondition, new CStatementWrapper(nextThreadAssignment));
    }

    // for the nondeterministic choice add an 'if (1) { ... }' statements
    ifElseChain.putAll(
        new CExpressionWrapper(CIntegerLiteralExpression.ONE),
        SeqMainFunctionBuilder.buildNextThreadNondeterministicStatements(
            pOptions, pFields.numThreads, pFields.ghostElements, pUtils.binaryExpressionBuilder()));

    return CMultiSelectionStatementBuilder.buildIfElseChain(ifElseChain.build());
  }

  /** Returns the expression {@code (pci != 0 && Ti_SYNC == 0 && *Ti not in conflict*)}. */
  private CExportExpression buildNextThreadIgnoreSleepExpression()
      throws UnrecognizedCodeException {

    CExpression threadActiveExpression =
        ghostElements.getPcVariables().getThreadActiveExpression(activeThread.id());
    CExpression syncEqualsZero = buildSyncEqualsZeroExpression();
    CExportExpression bitVectorExpression = buildBitVectorEvaluationExpression();
    return CLogicalAndExpression.of(
        new CExpressionWrapper(threadActiveExpression),
        new CExpressionWrapper(syncEqualsZero),
        bitVectorExpression.negate());
  }

  private CExpression buildSyncEqualsZeroExpression() throws UnrecognizedCodeException {
    return utils
        .binaryExpressionBuilder()
        .buildBinaryExpression(
            ghostElements.threadSyncFlags().getSyncFlag(activeThread),
            SeqIntegerLiteralExpressions.INT_0,
            BinaryOperator.EQUALS);
  }

  private CExportExpression buildBitVectorEvaluationExpression() throws UnrecognizedCodeException {
    SeqBitVectorVariables bitVectorVariables = ghostElements.bitVectorVariables().orElseThrow();
    Optional<CExportExpression> bitVectorEvaluationExpression =
        BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
            options, activeThread, otherThreads, bitVectorVariables, utils);
    return bitVectorEvaluationExpression.isPresent()
        ? bitVectorEvaluationExpression.orElseThrow()
        : new CExpressionWrapper(CIntegerLiteralExpression.ZERO);
  }

  SeqThreadStatement tryInjectSyncUpdateIntoStatement(SeqThreadStatement pStatement) {
    if (pStatement.targetPc().isPresent()) {
      // int target is present -> retrieve label by pc from map
      int targetPc = pStatement.targetPc().orElseThrow();
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause targetClause =
            Objects.requireNonNull(labelClauseMap.get(targetPc));
        return injectSyncUpdateIntoStatement(pStatement, Optional.of(targetClause));
      } else {
        return injectSyncUpdateIntoStatement(pStatement, Optional.empty());
      }
    }
    // no int target pc -> no injection
    return pStatement;
  }

  private SeqThreadStatement injectSyncUpdateIntoStatement(
      SeqThreadStatement pStatement, Optional<SeqThreadStatementClause> pTargetClause) {

    boolean isSync =
        pTargetClause.isPresent()
            && SeqThreadStatementUtil.anySynchronizesThreads(
                pTargetClause.orElseThrow().getAllStatements());
    CIntegerLiteralExpression value =
        isSync ? SeqIntegerLiteralExpressions.INT_1 : SeqIntegerLiteralExpressions.INT_0;
    CIdExpression syncFlag = ghostElements.threadSyncFlags().getSyncFlag(activeThread);
    SeqInstrumentation syncUpdate =
        SeqInstrumentationBuilder.buildThreadSyncUpdateStatement(syncFlag, value);
    return SeqThreadStatementUtil.appendedInstrumentationStatement(pStatement, syncUpdate);
  }
}
