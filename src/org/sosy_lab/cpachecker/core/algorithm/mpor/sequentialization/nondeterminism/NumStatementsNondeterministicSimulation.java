// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalAndExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CLogicalOrExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CLabelStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

class NumStatementsNondeterministicSimulation extends NondeterministicSimulation {

  NumStatementsNondeterministicSimulation(
      MPOROptions pOptions,
      Optional<MemoryModel> pMemoryModel,
      GhostElements pGhostElements,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      SequentializationUtils pUtils) {

    super(pOptions, pMemoryModel, pGhostElements, pClauses, pUtils);
  }

  @Override
  public ImmutableList<CExportStatement> buildSingleThreadSimulation(MPORThread pThread)
      throws UnrecognizedCodeException {
    ImmutableList.Builder<CExportStatement> rSimulation = ImmutableList.builder();

    // add "T{thread_id}: label", if present
    Optional<CLabelStatement> threadLabel =
        Optional.ofNullable(ghostElements.threadLabels().get(pThread));
    if (threadLabel.isPresent()) {
      rSimulation.add(threadLabel.orElseThrow());
    }

    // add "if (pc != 0 ...)" condition
    CBinaryExpression ifCondition =
        ghostElements.getPcVariables().getThreadActiveExpression(pThread.id());
    ImmutableList.Builder<CExportStatement> ifBlock = ImmutableList.builder();

    // add the round_max = nondet assignment for this thread
    ifBlock.add(
        new CStatementWrapper(
            VerifierNondetFunctionType.buildNondetIntegerAssignment(
                options, SeqIdExpressions.ROUND_MAX)));

    // if (round_max > 0) ...
    ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(clauses.keySet(), pThread);
    CExportExpression innerIfCondition = buildRoundMaxGreaterZeroExpression(pThread, otherThreads);
    ImmutableList.Builder<CExportStatement> innerIfBlock = ImmutableList.builder();

    // add the thread simulation statements
    innerIfBlock.addAll(buildAllPrecedingStatements(pThread));
    innerIfBlock.add(buildSingleThreadMultiControlStatement(pThread));
    CIfStatement innerIfStatement =
        new CIfStatement(innerIfCondition, new CCompoundStatement(innerIfBlock.build()));
    ifBlock.add(innerIfStatement);
    CIfStatement ifStatement =
        new CIfStatement(
            new CExpressionWrapper(ifCondition), new CCompoundStatement(ifBlock.build()));

    // add all and return
    return rSimulation.add(ifStatement).build();
  }

  @Override
  public ImmutableList<CExportStatement> buildAllThreadSimulations()
      throws UnrecognizedCodeException {
    ImmutableList.Builder<CExportStatement> rThreadSimulations = ImmutableList.builder();
    for (MPORThread thread : clauses.keySet()) {
      rThreadSimulations.addAll(buildSingleThreadSimulation(thread));
    }
    return rThreadSimulations.build();
  }

  @Override
  public ImmutableList<CExportStatement> buildPrecedingStatements(MPORThread pThread) {
    // assume("pc active") is not necessary since the simulation starts with 'if (pc* != 0)'
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationBuilder.buildRoundReset();
    return ImmutableList.<CExportStatement>builder().add(new CStatementWrapper(roundReset)).build();
  }

  private CExportExpression buildRoundMaxGreaterZeroExpression(
      MPORThread pActiveThread, ImmutableSet<MPORThread> pOtherThreads)
      throws UnrecognizedCodeException {

    // round_max > 0
    CExpression roundMaxGreaterZero =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.ROUND_MAX,
                SeqIntegerLiteralExpressions.INT_0,
                BinaryOperator.GREATER_THAN);

    if (!options.reduceIgnoreSleep()) {
      return new CExpressionWrapper(roundMaxGreaterZero);
    }
    // if enabled, add bit vector evaluation: "round_max > 0 || {bitvector_evaluation}"
    Optional<CExportExpression> bitVectorEvaluationExpression =
        BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
            options,
            pActiveThread,
            pOtherThreads,
            ghostElements.bitVectorVariables().orElseThrow(),
            utils);
    // if the bv evaluation is empty, then the program contains no global memory locations -> prune
    if (bitVectorEvaluationExpression.isEmpty()) {
      return new CExpressionWrapper(roundMaxGreaterZero);
    }
    // ensure that thread is not at a thread sync location: !sync && !conflict
    CIdExpression syncFlag = ghostElements.threadSyncFlags().getSyncFlag(pActiveThread);
    CBinaryExpression notSync =
        utils.binaryExpressionBuilder().negateExpressionAndSimplify(syncFlag);
    CLogicalAndExpression notSyncAndNotConflict =
        CLogicalAndExpression.of(
            new CExpressionWrapper(notSync), bitVectorEvaluationExpression.orElseThrow().negate());
    // the usual bit vector expression is true if there is a conflict
    //  -> negate (we want no conflict if we ignore round_max == 0)
    return CLogicalOrExpression.of(
        new CExpressionWrapper(roundMaxGreaterZero), notSyncAndNotConflict);
  }
}
