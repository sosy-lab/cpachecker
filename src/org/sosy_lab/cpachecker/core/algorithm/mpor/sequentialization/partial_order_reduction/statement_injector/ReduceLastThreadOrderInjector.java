// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqLastBitVectorUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.ASeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class ReduceLastThreadOrderInjector {

  // Public Interface ==============================================================================

  static ASeqThreadStatement injectLastThreadOrderReductionIntoStatement(
      MPOROptions pOptions,
      int pNumThreads,
      ASeqThreadStatement pStatement,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    ASeqThreadStatement withConflictOrder =
        injectConflictOrderIntoStatement(
            pOptions,
            pStatement,
            pActiveThread,
            pLabelClauseMap,
            pLabelBlockMap,
            pBitVectorVariables,
            pMemoryModel,
            pUtils);
    return injectLastUpdatesIntoStatement(
        pOptions, pNumThreads, withConflictOrder, pActiveThread, pBitVectorVariables);
  }

  // Private =======================================================================================

  private static ASeqThreadStatement injectConflictOrderIntoStatement(
      MPOROptions pOptions,
      ASeqThreadStatement pStatement,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    if (pActiveThread.isMain()) {
      // do not inject for main thread, because last_thread < 0 never holds
      return pStatement;
    }
    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause targetClause = pLabelClauseMap.get(targetPc);
      assert targetClause != null : "could not find targetPc in pLabelBlockMap";
      if (StatementInjector.isReductionAllowed(pOptions, targetClause)) {
        SeqThreadStatementBlock targetBlock = pLabelBlockMap.get(targetPc);
        // build conflict order statement (with bit vector evaluations based on targetBlock)
        BitVectorEvaluationExpression lastBitVectorEvaluation =
            BitVectorEvaluationBuilder.buildLastBitVectorEvaluation(
                pOptions, pLabelBlockMap, targetBlock, pBitVectorVariables, pMemoryModel, pUtils);
        SeqConflictOrderStatement conflictOrderStatement =
            new SeqConflictOrderStatement(
                pActiveThread, lastBitVectorEvaluation, pUtils.getBinaryExpressionBuilder());
        return pStatement.cloneAppendingInjectedStatements(
            ImmutableList.of(conflictOrderStatement));
      }
    }
    // no conflict order injected
    return pStatement;
  }

  // Last Updates ==================================================================================

  private static ASeqThreadStatement injectLastUpdatesIntoStatement(
      MPOROptions pOptions,
      int pNumThreads,
      ASeqThreadStatement pStatement,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables) {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        // if a thread exits, set last_thread to NUM_THREADS - 1
        CExpressionAssignmentStatement lastThreadExit =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                SeqIdExpressions.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads));
        SeqLastBitVectorUpdateStatement lastUpdateStatement =
            new SeqLastBitVectorUpdateStatement(lastThreadExit, ImmutableList.of());
        return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(lastUpdateStatement));
      } else {
        // for all other target pc, set last_thread to current thread id and update last bitvectors
        CExpressionAssignmentStatement lastThreadUpdate =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                SeqIdExpressions.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pActiveThread.getId()));
        SeqLastBitVectorUpdateStatement lastUpdateStatement =
            new SeqLastBitVectorUpdateStatement(
                lastThreadUpdate,
                buildLastAccessBitVectorUpdatesByEncoding(
                    pOptions, pActiveThread, pBitVectorVariables));
        return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(lastUpdateStatement));
      }
    } else {
      // no valid target pc -> no conflict order required
      return pStatement;
    }
  }

  // Last Access Bit Vectors =======================================================================

  private static ImmutableList<CExpressionAssignmentStatement>
      buildLastAccessBitVectorUpdatesByEncoding(
          MPOROptions pOptions, MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.bitVectorEncoding) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build updates for bitVectorEncoding %s", pOptions.bitVectorEncoding));
      case BINARY, DECIMAL, HEXADECIMAL ->
          buildDenseLastBitVectorUpdates(pOptions, pActiveThread, pBitVectorVariables);
      case SPARSE -> buildSparseLastBitVectorUpdates(pOptions, pActiveThread, pBitVectorVariables);
    };
  }

  private static ImmutableList<CExpressionAssignmentStatement> buildDenseLastBitVectorUpdates(
      MPOROptions pOptions, MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format("cannot build updates for reductionMode %s", pOptions.reductionMode));
      case ACCESS_ONLY ->
          buildDenseLastBitVectorUpdatesByAccessType(
              pActiveThread, pBitVectorVariables, MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(
                  buildDenseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, MemoryAccessType.ACCESS))
              .addAll(
                  buildDenseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, MemoryAccessType.WRITE))
              .build();
    };
  }

  private static ImmutableList<CExpressionAssignmentStatement> buildSparseLastBitVectorUpdates(
      MPOROptions pOptions, MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format("cannot build updates for reductionMode %s", pOptions.reductionMode));
      case ACCESS_ONLY ->
          buildSparseLastBitVectorUpdatesByAccessType(
              pActiveThread, pBitVectorVariables, MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(
                  buildSparseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, MemoryAccessType.ACCESS))
              .addAll(
                  buildSparseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, MemoryAccessType.WRITE))
              .build();
    };
  }

  private static ImmutableList<CExpressionAssignmentStatement>
      buildDenseLastBitVectorUpdatesByAccessType(
          MPORThread pActiveThread,
          BitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    LastDenseBitVector lastDenseBitVector =
        pBitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
    CExpression rightHandSide =
        pBitVectorVariables.getDenseBitVector(pActiveThread, pAccessType, ReachType.REACHABLE);
    CExpressionAssignmentStatement update =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            lastDenseBitVector.reachableVariable, rightHandSide);
    return ImmutableList.of(update);
  }

  private static ImmutableList<CExpressionAssignmentStatement>
      buildSparseLastBitVectorUpdatesByAccessType(
          MPORThread pActiveThread,
          BitVectorVariables pBitVectorVariables,
          MemoryAccessType pAccessType) {

    ImmutableList.Builder<CExpressionAssignmentStatement> rUpdates = ImmutableList.builder();
    ImmutableMap<SeqMemoryLocation, LastSparseBitVector> lastSparseBitVectors =
        pBitVectorVariables.getLastSparseBitVectorByAccessType(pAccessType);
    ImmutableMap<SeqMemoryLocation, SparseBitVector> sparseBitVectors =
        pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType);
    for (var entry : sparseBitVectors.entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> reachableVariableMap =
          entry.getValue().getVariablesByReachType(ReachType.REACHABLE);
      for (var reachableVariable : reachableVariableMap.entrySet()) {
        if (reachableVariable.getKey().equals(pActiveThread)) {
          SeqMemoryLocation memoryLocation = entry.getKey();
          LastSparseBitVector lastSparseBitVector = lastSparseBitVectors.get(memoryLocation);
          assert lastSparseBitVector != null;
          CIdExpression rightHandSide = reachableVariable.getValue();
          CExpressionAssignmentStatement update =
              SeqStatementBuilder.buildExpressionAssignmentStatement(
                  lastSparseBitVector.reachableVariable, rightHandSide);
          rUpdates.add(update);
        }
      }
    }
    return rUpdates.build();
  }
}
