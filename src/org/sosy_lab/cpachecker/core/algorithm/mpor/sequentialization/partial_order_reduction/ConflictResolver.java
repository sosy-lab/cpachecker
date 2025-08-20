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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ConflictResolver {

  // Public Interface ==============================================================================

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> resolve(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
      BitVectorVariables pBitVectorVariables,
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
    for (MPORThread activeThread : pClauses.keySet()) {
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(activeThread);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses);
      rResolved.putAll(
          activeThread,
          addConflictOrdersToClauses(
              pOptions,
              pClauses.get(activeThread),
              activeThread,
              labelBlockMap,
              pPointerAssignments,
              pPointerParameterAssignments,
              pBitVectorVariables,
              pBinaryExpressionBuilder));
    }
    return rResolved.build();
  }

  // Private =======================================================================================

  private static ImmutableList<SeqThreadStatementClause> addConflictOrdersToClauses(
      MPOROptions pOptions,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rWithOrders = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.getBlocks()) {
        newBlocks.add(
            addConflictOrdersToBlock(
                pOptions,
                mergedBlock,
                pActiveThread,
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pBitVectorVariables,
                pBinaryExpressionBuilder));
      }
      rWithOrders.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return rWithOrders.build();
  }

  private static SeqThreadStatementBlock addConflictOrdersToBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(
          addConflictOrderToStatement(
              pOptions,
              statement,
              pActiveThread,
              pLabelBlockMap,
              pPointerAssignments,
              pPointerParameterAssignments,
              pBitVectorVariables,
              pBinaryExpressionBuilder));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement addConflictOrderToStatement(
      MPOROptions pOptions,
      SeqThreadStatement pStatement,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pActiveThread.isMain()) {
      // do not inject for main thread, because last_thread < 0 never holds
      return pStatement;
    }
    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementBlock targetBlock = pLabelBlockMap.get(targetPc);
      // build conflict order statement (with bit vector evaluations based on pTargetBlock)
      BitVectorEvaluationExpression lastBitVectorEvaluation =
          BitVectorEvaluationBuilder.buildLastBitVectorEvaluation(
              pOptions,
              pLabelBlockMap,
              pPointerAssignments,
              pPointerParameterAssignments,
              targetBlock,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      SeqConflictOrderStatement conflictOrderStatement =
          new SeqConflictOrderStatement(
              pActiveThread, lastBitVectorEvaluation, pBinaryExpressionBuilder);
      return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(conflictOrderStatement));
    } else {
      // no valid target pc -> no conflict order required
      return pStatement;
    }
  }
}
