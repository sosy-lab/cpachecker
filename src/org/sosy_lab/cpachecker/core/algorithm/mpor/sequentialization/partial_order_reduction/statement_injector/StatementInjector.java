// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class StatementInjector {

  // Public Interface ==============================================================================

  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> injectStatements(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    // first check shortcuts: are injections necessary?
    if (pMemoryModel.getRelevantMemoryLocationAmount() == 0) {
      pLogger.log(
          Level.INFO,
          "bit vectors are enabled, but the program does not contain any global memory locations.");
      return pClauses; // no relevant memory locations -> no bit vectors needed
    }
    if (!pOptions.areBitVectorsEnabled()) {
      return pClauses;
    }
    return injectStatements(
        pOptions, pClauses, pBitVectorVariables, pMemoryModel, pBinaryExpressionBuilder);
  }

  // Private Methods ===============================================================================

  private static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> injectStatements(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rInjected =
        ImmutableListMultimap.builder();
    for (MPORThread activeThread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(activeThread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses);
      rInjected.putAll(
          activeThread,
          injectStatementsIntoClauses(
              pOptions,
              activeThread,
              MPORUtil.withoutElement(pClauses.keySet(), activeThread),
              clauses,
              labelClauseMap,
              labelBlockMap,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder));
    }
    return rInjected.build();
  }

  private static ImmutableList<SeqThreadStatementClause> injectStatementsIntoClauses(
      MPOROptions pOptions,
      MPORThread pActiveThread,
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
            injectStatementsIntoBlock(
                pOptions,
                block,
                pActiveThread,
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

  private static SeqThreadStatementBlock injectStatementsIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      MPORThread pActiveThread,
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
          injectStatementsIntoStatement(
              pOptions,
              pActiveThread,
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

  private static SeqThreadStatement injectStatementsIntoStatement(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.bitVectorReduction) {
      pStatement =
          BitVectorInjector.injectBitVectorReductionIntoStatement(
              pOptions,
              pOtherThreads,
              pStatement,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder);
    }
    if (pOptions.conflictReduction) {
      pStatement =
          ConflictOrderInjector.injectOrderStatementsIntoStatement(
              pOptions,
              pOtherThreads.size() + 1,
              pStatement,
              pActiveThread,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder);
    }
    if (pOptions.kIgnoreZeroReduction) {
      // this needs to be last, it collects the prior injections
      pStatement =
          KIgnoreZeroInjector.injectKIgnoreZeroReductionIntoStatement(
              pOptions,
              pActiveThread.getKVariable(),
              pOtherThreads,
              pStatement,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel,
              pBinaryExpressionBuilder);
    }
    // always inject bit vector assignments after evaluations i.e. reductions
    pStatement =
        BitVectorAssignmentInjector.injectBitVectorAssignmentsIntoStatement(
            pOptions,
            pActiveThread,
            pStatement,
            pLabelClauseMap,
            pLabelBlockMap,
            pBitVectorVariables,
            pMemoryModel);
    return pStatement;
  }

  // Bit Vector Evaluations =======================================================================

  static BitVectorEvaluationExpression buildBitVectorEvaluationExpression(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return BitVectorEvaluationBuilder.buildEvaluationByDirectVariableAccesses(
        pOptions,
        pOtherThreads,
        pLabelBlockMap,
        pTargetBlock,
        pBitVectorVariables,
        pMemoryModel,
        pBinaryExpressionBuilder);
  }

  static SeqBitVectorEvaluationStatement buildBitVectorEvaluationStatement(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    BitVectorEvaluationExpression evaluationExpression =
        buildBitVectorEvaluationExpression(
            pOptions,
            pOtherThreads,
            pLabelBlockMap,
            pTargetBlock,
            pBitVectorVariables,
            pMemoryModel,
            pBinaryExpressionBuilder);
    return new SeqBitVectorEvaluationStatement(
        pOptions, evaluationExpression, pTargetBlock.getLabel());
  }

  // boolean helpers ===============================================================================

  /**
   * Checks whether bit vector injections are allowed, i.e. if they do not result in interleaving
   * loss.
   */
  static boolean isReductionAllowed(MPOROptions pOptions, SeqThreadStatementClause pTarget) {
    // if the target starts with a thread synchronization (i.e. assume), do not inject
    return !SeqThreadStatementUtil.anySynchronizesThreads(pTarget.getAllStatements())
        // check based on pOptions if the target is a loop head that must remain separate
        && !SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, pTarget);
  }
}
