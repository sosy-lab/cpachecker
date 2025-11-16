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
import java.util.Objects;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqLastBitVectorUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.ReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

record ReduceLastThreadOrderInjector(
    MPOROptions options,
    int numThreads,
    MPORThread activeThread,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    BitVectorVariables bitVectorVariables,
    MemoryModel memoryModel,
    SequentializationUtils utils) {

  CSeqThreadStatement injectLastThreadOrderReductionIntoStatement(CSeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    CSeqThreadStatement withConflictOrder = injectConflictOrderIntoStatement(pStatement);
    return injectLastUpdatesIntoStatement(withConflictOrder);
  }

  // Private =======================================================================================

  private CSeqThreadStatement injectConflictOrderIntoStatement(CSeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    if (activeThread.isMain()) {
      // do not inject for main thread, because last_thread < 0 never holds
      return pStatement;
    }
    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause targetClause = labelClauseMap.get(targetPc);
      assert targetClause != null : "could not find targetPc in pLabelBlockMap";
      if (StatementInjector.isReductionAllowed(options, targetClause)) {
        SeqThreadStatementBlock targetBlock = Objects.requireNonNull(labelBlockMap.get(targetPc));
        // build conflict order statement (with bit vector evaluations based on targetBlock)
        Optional<BitVectorEvaluationExpression> lastBitVectorEvaluation =
            BitVectorEvaluationBuilder.buildLastBitVectorEvaluation(
                options, labelBlockMap, targetBlock, bitVectorVariables, memoryModel, utils);
        SeqConflictOrderStatement conflictOrderStatement =
            new SeqConflictOrderStatement(
                activeThread, lastBitVectorEvaluation, utils.binaryExpressionBuilder());
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, conflictOrderStatement);
      }
    }
    // no conflict order injected
    return pStatement;
  }

  // Last Updates ==================================================================================

  private CSeqThreadStatement injectLastUpdatesIntoStatement(CSeqThreadStatement pStatement) {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        // if a thread exits, set last_thread to NUM_THREADS - 1
        CExpressionAssignmentStatement lastThreadExit =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                SeqIdExpressions.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(numThreads));
        SeqLastBitVectorUpdateStatement lastUpdateStatement =
            new SeqLastBitVectorUpdateStatement(lastThreadExit, ImmutableList.of());
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, lastUpdateStatement);

      } else {
        // for all other target pc, set last_thread to current thread id and update last bitvectors
        CExpressionAssignmentStatement lastThreadUpdate =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                SeqIdExpressions.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.id()));
        SeqLastBitVectorUpdateStatement lastUpdateStatement =
            new SeqLastBitVectorUpdateStatement(
                lastThreadUpdate, buildLastAccessBitVectorUpdatesByEncoding());
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, lastUpdateStatement);
      }
    } else {
      // no valid target pc -> no conflict order required
      return pStatement;
    }
  }

  // Last Access Bit Vectors =======================================================================

  private ImmutableList<CExpressionAssignmentStatement>
      buildLastAccessBitVectorUpdatesByEncoding() {

    return switch (options.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build updates for bitVectorEncoding %s", options.bitVectorEncoding()));
      case BINARY, DECIMAL, HEXADECIMAL -> buildDenseLastBitVectorUpdates();
      case SPARSE -> buildSparseLastBitVectorUpdates();
    };
  }

  private ImmutableList<CExpressionAssignmentStatement> buildDenseLastBitVectorUpdates() {
    return switch (options.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format("cannot build updates for reductionMode %s", options.reductionMode()));
      case ACCESS_ONLY -> buildDenseLastBitVectorUpdatesByAccessType(MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(buildDenseLastBitVectorUpdatesByAccessType(MemoryAccessType.ACCESS))
              .addAll(buildDenseLastBitVectorUpdatesByAccessType(MemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<CExpressionAssignmentStatement> buildSparseLastBitVectorUpdates() {
    return switch (options.reductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format("cannot build updates for reductionMode %s", options.reductionMode()));
      case ACCESS_ONLY -> buildSparseLastBitVectorUpdatesByAccessType(MemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(buildSparseLastBitVectorUpdatesByAccessType(MemoryAccessType.ACCESS))
              .addAll(buildSparseLastBitVectorUpdatesByAccessType(MemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<CExpressionAssignmentStatement> buildDenseLastBitVectorUpdatesByAccessType(
      MemoryAccessType pAccessType) {

    LastDenseBitVector lastDenseBitVector =
        bitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
    CExpression rightHandSide =
        bitVectorVariables.getDenseBitVector(activeThread, pAccessType, ReachType.REACHABLE);
    CExpressionAssignmentStatement update =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            lastDenseBitVector.reachableVariable(), rightHandSide);
    return ImmutableList.of(update);
  }

  private ImmutableList<CExpressionAssignmentStatement> buildSparseLastBitVectorUpdatesByAccessType(
      MemoryAccessType pAccessType) {

    ImmutableList.Builder<CExpressionAssignmentStatement> rUpdates = ImmutableList.builder();
    ImmutableMap<SeqMemoryLocation, LastSparseBitVector> lastSparseBitVectors =
        bitVectorVariables.getLastSparseBitVectorByAccessType(pAccessType);
    ImmutableMap<SeqMemoryLocation, SparseBitVector> sparseBitVectors =
        bitVectorVariables.getSparseBitVectorByAccessType(pAccessType);
    for (var entry : sparseBitVectors.entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> reachableVariableMap =
          entry.getValue().getVariablesByReachType(ReachType.REACHABLE);
      for (var reachableVariable : reachableVariableMap.entrySet()) {
        if (reachableVariable.getKey().equals(activeThread)) {
          SeqMemoryLocation memoryLocation = entry.getKey();
          LastSparseBitVector lastSparseBitVector = lastSparseBitVectors.get(memoryLocation);
          assert lastSparseBitVector != null;
          CIdExpression rightHandSide = reachableVariable.getValue();
          CExpressionAssignmentStatement update =
              SeqStatementBuilder.buildExpressionAssignmentStatement(
                  lastSparseBitVector.reachableVariable(), rightHandSide);
          rUpdates.add(update);
        }
      }
    }
    return rUpdates.build();
  }
}
