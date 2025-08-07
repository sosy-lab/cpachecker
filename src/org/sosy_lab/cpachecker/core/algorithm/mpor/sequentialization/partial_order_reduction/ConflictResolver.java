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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ConflictResolver {

  // Public Interface ==============================================================================

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> resolve(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.getNumGlobalVariables() == 0) {
      pLogger.log(
          Level.INFO,
          "conflictReduction is enabled, but the program does not contain any global variables.");
      return pClauses; // no global variables -> no conflict resolving needed
    }
    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rResolved =
        ImmutableListMultimap.builder();
    ImmutableSet<MPORThread> allThreads = pClauses.keySet();
    for (MPORThread activeThread : pClauses.keySet()) {
      ImmutableSet<MPORThread> otherThreads = MPORUtil.withoutElement(allThreads, activeThread);
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(activeThread);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses);
      rResolved.putAll(
          activeThread,
          addConflictOrdersToClauses(
              pOptions,
              pClauses.get(activeThread),
              activeThread,
              otherThreads,
              labelBlockMap,
              pPointerAssignments,
              pBitVectorVariables,
              pPcVariables,
              pBinaryExpressionBuilder));
    }
    return rResolved.build();
  }

  // Private =======================================================================================

  private static ImmutableList<SeqThreadStatementClause> addConflictOrdersToClauses(
      MPOROptions pOptions,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rWithOrders = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      SeqThreadStatementBlock newBlock =
          addConflictOrdersToBlock(
              pOptions,
              clause.block,
              pActiveThread,
              pOtherThreads,
              pLabelBlockMap,
              pPointerAssignments,
              pBitVectorVariables,
              pPcVariables,
              pBinaryExpressionBuilder);
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        newMergedBlocks.add(
            addConflictOrdersToBlock(
                pOptions,
                mergedBlock,
                pActiveThread,
                pOtherThreads,
                pLabelBlockMap,
                pPointerAssignments,
                pBitVectorVariables,
                pPcVariables,
                pBinaryExpressionBuilder));
      }
      rWithOrders.add(
          clause.cloneWithBlock(newBlock).cloneWithMergedBlocks(newMergedBlocks.build()));
    }
    return rWithOrders.build();
  }

  private static SeqThreadStatementBlock addConflictOrdersToBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(
          addConflictOrderToStatement(
              pOptions,
              statement,
              pActiveThread,
              pOtherThreads,
              pLabelBlockMap,
              pPointerAssignments,
              pBitVectorVariables,
              pPcVariables,
              pBinaryExpressionBuilder));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement addConflictOrderToStatement(
      MPOROptions pOptions,
      SeqThreadStatement pStatement,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      BitVectorVariables pBitVectorVariables,
      PcVariables pPcVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementBlock targetBlock = pLabelBlockMap.get(targetPc);
      // build conflict order statement (with bit vector evaluations based on pTargetBlock)
      ImmutableMap<MPORThread, BitVectorEvaluationExpression> bitVectorEvaluationPairs =
          buildBitVectorEvaluationPairs(
              pOptions,
              pOtherThreads,
              pLabelBlockMap,
              pPointerAssignments,
              targetBlock,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      // use integer literal expressions for next_thread, the information is known
      CIntegerLiteralExpression nextThreadExpression =
          SeqExpressionBuilder.buildIntegerLiteralExpression(pActiveThread.id);
      SeqConflictOrderStatement conflictOrderStatement =
          new SeqConflictOrderStatement(
              pOptions,
              nextThreadExpression,
              bitVectorEvaluationPairs,
              pPcVariables,
              pBinaryExpressionBuilder);
      return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(conflictOrderStatement));
    } else {
      // no valid target pc -> no conflict order required
      return pStatement;
    }
  }

  private static ImmutableMap<MPORThread, BitVectorEvaluationExpression>
      buildBitVectorEvaluationPairs(
          MPOROptions pOptions,
          ImmutableSet<MPORThread> pOtherThreads,
          ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
          ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
          SeqThreadStatementBlock pTargetBlock,
          BitVectorVariables pBitVectorVariables,
          CBinaryExpressionBuilder pBinaryExpressionBuilder)
          throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, BitVectorEvaluationExpression> rPairs = ImmutableMap.builder();
    for (MPORThread otherThread : pOtherThreads) {
      BitVectorEvaluationExpression evaluationExpression =
          BitVectorEvaluationBuilder.buildEvaluationByDirectVariableAccesses(
              pOptions,
              // use the single other thread for the bit vector evaluation (pair wise!)
              ImmutableSet.of(otherThread),
              pLabelBlockMap,
              pPointerAssignments,
              pTargetBlock,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      rPairs.put(otherThread, evaluationExpression);
    }
    return rPairs.buildOrThrow();
  }
}
