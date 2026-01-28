// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionTree;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLabelStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CWrapperExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

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
  public String buildSingleThreadSimulation(MPORThread pThread) throws UnrecognizedCodeException {

    StringBuilder rLines = new StringBuilder();

    // add "T{thread_id}: label", if present
    Optional<CLabelStatement> threadLabel =
        Optional.ofNullable(ghostElements.threadLabels().get(pThread));
    if (threadLabel.isPresent()) {
      rLines.append(threadLabel.orElseThrow().toASTString());
    }

    // add "if (pc != 0 ...)" condition
    CBinaryExpression ifCondition =
        ghostElements.getPcVariables().getThreadActiveExpression(pThread.id());
    ImmutableList.Builder<String> ifBlock = ImmutableList.builder();

    // add the round_max = nondet assignment for this thread
    ifBlock.add(
        VerifierNondetFunctionType.buildNondetIntegerAssignment(options, SeqIdExpressions.ROUND_MAX)
            .toASTString());

    // if (round_max > 0) ...
    ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(clauses.keySet(), pThread);
    CAstExpression innerIfCondition = buildRoundMaxGreaterZeroExpression(pThread, otherThreads);
    ImmutableList.Builder<String> innerIfBlock = ImmutableList.builder();

    // add the thread simulation statements
    innerIfBlock.add(buildSingleThreadMultiControlStatement(pThread).toASTString());
    SeqBranchStatement innerIfStatement =
        new SeqBranchStatement(innerIfCondition.toASTString(), innerIfBlock.build());
    ifBlock.add(innerIfStatement.toASTString());
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(ifCondition.toASTString(), ifBlock.build());

    // add all and return
    return rLines.append(ifStatement.toASTString()).toString();
  }

  @Override
  public String buildAllThreadSimulations() throws UnrecognizedCodeException {
    StringBuilder rLines = new StringBuilder();
    for (MPORThread thread : clauses.keySet()) {
      rLines.append(buildSingleThreadSimulation(thread));
    }
    return rLines.toString();
  }

  @Override
  public ImmutableList<String> buildPrecedingStatements(MPORThread pThread) {
    // assume("pc active") is not necessary since the simulation starts with 'if (pc* != 0)'
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationBuilder.buildRoundReset();
    return ImmutableList.<String>builder().add(roundReset.toASTString()).build();
  }

  private CAstExpression buildRoundMaxGreaterZeroExpression(
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
      return new CWrapperExpression(roundMaxGreaterZero);
    }
    // if enabled, add bit vector evaluation: "round_max > 0 || {bitvector_evaluation}"
    Optional<CExpressionTree> bitVectorEvaluationExpression =
        BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
            options,
            pActiveThread,
            pOtherThreads,
            ghostElements.bitVectorVariables().orElseThrow(),
            utils);
    // ensure that thread is not at a thread sync location: !sync && !conflict
    CIdExpression syncFlag = ghostElements.threadSyncFlags().getSyncFlag(pActiveThread);
    CBinaryExpression notSync =
        utils.binaryExpressionBuilder().negateExpressionAndSimplify(syncFlag);
    ImmutableList<CAstExpression> expressionList =
        ImmutableList.of(
            new CWrapperExpression(notSync), bitVectorEvaluationExpression.orElseThrow().negate());
    ExpressionTree<CAstExpression> notSyncAndNotConflict =
        And.of(transformedImmutableListCopy(expressionList, LeafExpression::of));
    // the usual bit vector expression is true if there is a conflict
    //  -> negate (we want no conflict if we ignore round_max == 0)
    return new CExpressionTree(
        Or.of(
            LeafExpression.of(new CWrapperExpression(roundMaxGreaterZero)), notSyncAndNotConflict));
  }
}
