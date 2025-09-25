// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqKIgnoreZeroStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class KIgnoreZeroInjector {

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> injectKIgnoreZeroReduction(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rInjected =
        ImmutableListMultimap.builder();
    for (MPORThread activeThread : pClauses.keySet()) {
      ImmutableSet<MPORThread> otherThreads =
          MPORUtil.withoutElement(pClauses.keySet(), activeThread);
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(activeThread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses);
      rInjected.putAll(
          activeThread,
          injectKIgnoreZeroReductionIntoClauses(
              pOptions,
              activeThread.getKVariable(),
              otherThreads,
              clauses,
              labelClauseMap,
              labelBlockMap,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder));
    }
    return rInjected.build();
  }

  // Private =======================================================================================

  private static ImmutableList<SeqThreadStatementClause> injectKIgnoreZeroReductionIntoClauses(
      MPOROptions pOptions,
      Optional<CIdExpression> pKVariable,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(
            injectKIgnoreZeroReductionIntoBlock(
                pOptions,
                block,
                pKVariable,
                pOtherThreads,
                pLabelClauseMap,
                pLabelBlockMap,
                pBitVectorVariables,
                pMemoryModel,
                pBinaryExpressionBuilder));
      }
      rInjected.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return rInjected.build();
  }

  private static SeqThreadStatementBlock injectKIgnoreZeroReductionIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      Optional<CIdExpression> pKVariable,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(
          injectKIgnoreZeroReductionIntoStatement(
              pOptions,
              pKVariable,
              pOtherThreads,
              statement,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement injectKIgnoreZeroReductionIntoStatement(
      MPOROptions pOptions,
      Optional<CIdExpression> pKVariable,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      final BitVectorVariables pBitVectorVariables,
      final MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      // exclude exit pc, don't want 'assume(conflict)' there
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        if (PartialOrderReducer.isReductionAllowed(pOptions, newTarget)) {
          BitVectorEvaluationExpression evaluationExpression =
              PartialOrderReducer.buildBitVectorEvaluationExpression(
                  pOptions,
                  pOtherThreads,
                  pLabelBlockMap,
                  newTarget.getFirstBlock(),
                  pBitVectorVariables,
                  pMemoryModel,
                  pBinaryExpressionBuilder);
          SeqKIgnoreZeroStatement kIgnoreZeroStatement =
              buildKIgnoreZeroStatement(
                  pKVariable,
                  pCurrentStatement,
                  evaluationExpression,
                  newTarget,
                  pBinaryExpressionBuilder);
          return pCurrentStatement.cloneReplacingInjectedStatements(
              replaceReductionAssumptions(
                  pCurrentStatement.getInjectedStatements(), kIgnoreZeroStatement));
        }
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }

  private static SeqKIgnoreZeroStatement buildKIgnoreZeroStatement(
      Optional<CIdExpression> pKVariable,
      SeqThreadStatement pStatement,
      BitVectorEvaluationExpression pBitVectorEvaluationExpression,
      SeqThreadStatementClause pTargetClause,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    ImmutableList.Builder<SeqInjectedStatement> reductionAssumptions = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pStatement.getInjectedStatements()) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement bitVectorStatement) {
        reductionAssumptions.add(bitVectorStatement);
      }
      if (injectedStatement instanceof SeqConflictOrderStatement conflictOrderStatement) {
        reductionAssumptions.add(conflictOrderStatement);
      }
    }
    return new SeqKIgnoreZeroStatement(
        pKVariable.isPresent() ? pKVariable.orElseThrow() : SeqIdExpression.K,
        reductionAssumptions.build(),
        pBitVectorEvaluationExpression,
        pTargetClause.getFirstBlock().getLabel(),
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<SeqInjectedStatement> replaceReductionAssumptions(
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      SeqKIgnoreZeroStatement pKIgnoreZeroStatement) {

    ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
    newInjected.add(pKIgnoreZeroStatement);
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (!(injectedStatement instanceof SeqBitVectorEvaluationStatement)
          && !(injectedStatement instanceof SeqConflictOrderStatement)) {
        newInjected.add(injectedStatement);
      }
    }
    return newInjected.build();
  }
}
