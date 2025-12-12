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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqCountUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.SeqMultiControlStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;

record NumStatementsNondeterministicSimulation(
    MPOROptions options,
    ImmutableListMultimap<MPORThread, SeqThreadStatementClause> clauses,
    GhostElements ghostElements,
    SequentializationUtils utils) {

  String buildThreadSimulations() throws UnrecognizedCodeException {
    StringBuilder rLines = new StringBuilder();
    for (MPORThread thread : clauses.keySet()) {
      rLines.append(
          buildSingleThreadSimulation(thread, MPORUtil.withoutElement(clauses.keySet(), thread)));
    }
    return rLines.toString();
  }

  String buildSingleThreadSimulation(
      MPORThread pActiveThread, ImmutableSet<MPORThread> pOtherThreads)
      throws UnrecognizedCodeException {

    StringBuilder rLines = new StringBuilder();

    // add "T{thread_id}: label", if present
    Optional<SeqThreadLabelStatement> threadLabel =
        Optional.ofNullable(ghostElements.threadLabels().get(pActiveThread));
    if (threadLabel.isPresent()) {
      rLines.append(threadLabel.orElseThrow().toASTString());
    }

    // add "if (pc != 0 ...)" condition
    CBinaryExpression ifCondition =
        ghostElements.getPcVariables().getThreadActiveExpression(pActiveThread.id());
    ImmutableList.Builder<String> ifBlock = ImmutableList.builder();

    // add the round_max = nondet assignment for this thread
    ifBlock.add(
        VerifierNondetFunctionType.buildNondetIntegerAssignment(options, SeqIdExpressions.ROUND_MAX)
            .toASTString());

    // if (round_max > 0) ...
    String innerIfCondition = buildRoundMaxGreaterZeroExpression(pActiveThread, pOtherThreads);
    ImmutableList.Builder<String> innerIfBlock = ImmutableList.builder();

    // reset round only when needed i.e. after if (...) for performance
    CExpressionAssignmentStatement roundReset = NondeterministicSimulationUtil.buildRoundReset();
    innerIfBlock.add(roundReset.toASTString());

    // add the thread simulation statements
    innerIfBlock.add(buildSingleThreadClausesWithCount(pActiveThread, clauses.get(pActiveThread)));
    SeqBranchStatement innerIfStatement =
        new SeqBranchStatement(innerIfCondition, innerIfBlock.build());
    ifBlock.add(innerIfStatement.toASTString());
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(ifCondition.toASTString(), ifBlock.build());

    // add all and return
    return rLines.append(ifStatement.toASTString()).toString();
  }

  private String buildRoundMaxGreaterZeroExpression(
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
      return roundMaxGreaterZero.toASTString();
    }
    // if enabled, add bit vector evaluation: "round_max > 0 || {bitvector_evaluation}"
    Optional<BitVectorEvaluationExpression> bitVectorEvaluationExpression =
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
    ImmutableList<String> stringList =
        ImmutableList.of(
            notSync.toASTString(),
            bitVectorEvaluationExpression.orElseThrow().toNegatedASTString());
    ExpressionTree<String> notSyncAndNotConflict =
        And.of(transformedImmutableListCopy(stringList, LeafExpression::of));
    // the usual bit vector expression is true if there is a conflict
    //  -> negate (we want no conflict if we ignore round_max == 0)
    return Or.of(LeafExpression.of(roundMaxGreaterZero.toASTString()), notSyncAndNotConflict)
        .toString();
  }

  private String buildSingleThreadClausesWithCount(
      MPORThread pThread, ImmutableList<SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    StringBuilder rLines = new StringBuilder();

    ProgramCounterVariables pcVariables = ghostElements.getPcVariables();
    CLeftHandSide expression = pcVariables.getPcLeftHandSide(pThread.id());
    Optional<SeqBranchStatement> assumption =
        NondeterministicSimulationUtil.tryBuildPcUnequalExitAssumption(
            options, pcVariables, pThread);

    ImmutableList<SeqThreadStatementClause> singleThreadClauses =
        buildSingleThreadClauses(ghostElements.threadSyncFlags().getSyncFlag(pThread), pClauses);
    ImmutableMap<CExpression, ? extends SeqStatement> expressionClauseMap =
        SeqThreadStatementClauseUtil.mapExpressionToClause(
            options,
            pcVariables.getPcLeftHandSide(pThread.id()),
            singleThreadClauses,
            utils.binaryExpressionBuilder());

    SeqMultiControlStatement multiControlStatement =
        MultiControlStatementBuilder.buildMultiControlStatementByEncoding(
            options.controlEncodingStatement(),
            expression,
            assumption.isPresent()
                ? ImmutableList.of(assumption.orElseThrow().toASTString())
                : ImmutableList.of(),
            expressionClauseMap,
            utils.binaryExpressionBuilder());

    return rLines.append(multiControlStatement.toASTString()).toString();
  }

  private ImmutableList<SeqThreadStatementClause> buildSingleThreadClauses(
      CIdExpression pSyncFlag, ImmutableList<SeqThreadStatementClause> pClauses)
      throws UnrecognizedCodeException {

    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);

    ImmutableList.Builder<SeqThreadStatementClause> updatedClauses = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(injectCountAndRoundGotoIntoBlock(block, pSyncFlag, labelClauseMap));
      }
      updatedClauses.add(clause.withBlocks(newBlocks.build()));
    }
    return updatedClauses.build();
  }

  // Injections ====================================================================================

  private SeqThreadStatementBlock injectCountAndRoundGotoIntoBlock(
      SeqThreadStatementBlock pBlock,
      CIdExpression pSyncFlag,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap)
      throws UnrecognizedCodeException {

    SeqThreadStatementBlock withCountUpdate = tryInjectCountUpdatesIntoBlock(pBlock);
    SeqThreadStatementBlock withRoundGoto =
        NondeterministicSimulationUtil.injectRoundGotoIntoBlock(
            options, withCountUpdate, pLabelClauseMap, utils.binaryExpressionBuilder());
    return NondeterministicSimulationUtil.injectSyncUpdatesIntoBlock(
        options, withRoundGoto, pSyncFlag, pLabelClauseMap);
  }

  private SeqThreadStatementBlock tryInjectCountUpdatesIntoBlock(SeqThreadStatementBlock pBlock)
      throws UnrecognizedCodeException {

    if (!options.isThreadCountRequired()) {
      return pBlock;
    }
    ImmutableList.Builder<CSeqThreadStatement> newStatements = ImmutableList.builder();
    for (CSeqThreadStatement statement : pBlock.getStatements()) {
      CSeqThreadStatement withCountUpdates = tryInjectCountUpdatesIntoStatement(statement);
      newStatements.add(withCountUpdates);
    }
    return pBlock.withStatements(newStatements.build());
  }

  private CSeqThreadStatement tryInjectCountUpdatesIntoStatement(CSeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    if (pStatement instanceof SeqThreadCreationStatement) {
      CExpressionAssignmentStatement countIncrement =
          SeqStatementBuilder.buildIncrementStatement(
              SeqIdExpressions.THREAD_COUNT, utils.binaryExpressionBuilder());
      SeqCountUpdateStatement countIncrementStatement = new SeqCountUpdateStatement(countIncrement);
      return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
          pStatement, countIncrementStatement);

    } else if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
        CExpressionAssignmentStatement countDecrement =
            SeqStatementBuilder.buildDecrementStatement(
                SeqIdExpressions.THREAD_COUNT, utils.binaryExpressionBuilder());
        SeqCountUpdateStatement countDecrementStatement =
            new SeqCountUpdateStatement(countDecrement);
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, countDecrementStatement);
      }
    }
    return pStatement;
  }
}
