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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqLastBitVectorUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqLastThreadOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
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

  CSeqThreadStatement injectLastThreadOrderReductionIntoStatement(
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap)
      throws UnrecognizedCodeException {

    CSeqThreadStatement withConflictOrder = injectLastThreadOrderIntoStatement(pStatement);
    return injectLastUpdatesIntoStatement(withConflictOrder, pLabelClauseMap);
  }

  // Private =======================================================================================

  private CSeqThreadStatement injectLastThreadOrderIntoStatement(CSeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    if (activeThread.isMain()) {
      // do not inject for main thread, because last_thread < 0 never holds
      return pStatement;
    }
    if (pStatement.isTargetPcValid()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause targetClause = labelClauseMap.get(targetPc);
      assert targetClause != null : "could not find targetPc in pLabelBlockMap";
      if (StatementInjector.isReductionAllowed(options, targetClause)) {
        SeqThreadStatementBlock targetBlock = Objects.requireNonNull(labelBlockMap.get(targetPc));
        // build last thread order statement (with bit vector evaluations based on targetBlock)
        Optional<BitVectorEvaluationExpression> lastBitVectorEvaluation =
            BitVectorEvaluationBuilder.buildLastBitVectorEvaluation(
                options, labelBlockMap, targetBlock, bitVectorVariables, memoryModel, utils);
        SeqLastThreadOrderStatement lastThreadOrderStatement =
            new SeqLastThreadOrderStatement(
                activeThread, lastBitVectorEvaluation, utils.binaryExpressionBuilder());
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, lastThreadOrderStatement);
      }
    }
    // no last thread order injected
    return pStatement;
  }

  // Last Updates ==================================================================================

  private CSeqThreadStatement injectLastUpdatesIntoStatement(
      CSeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      // if a thread exits, set last_thread to NUM_THREADS - 1.
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
        return injectLastThreadUpdateIntoStatement(pStatement, numThreads, ImmutableList.of());
      }
      // if targetPc != EXIT_PC, then pLabelClause contains targetPc, otherwise NPE
      SeqThreadStatementClause targetClause = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
      // for sync locations, set LAST_THREAD to NUM_THREADS - 1. this is necessary, otherwise
      // the analysis is unsound.
      // simple example: LAST_THREAD is at a sync location that uses assume. the current thread
      // has a reduceLastThreadOrder instrumentation and because it is not in conflict with
      // LAST_THREAD, current thread aborts. but LAST_THREAD may e.g. call pthread_join on the
      // current thread -> both abort, and no thread makes any progress
      if (SeqThreadStatementUtil.anySynchronizesThreads(targetClause.getAllStatements())) {
        return injectLastThreadUpdateIntoStatement(pStatement, numThreads, ImmutableList.of());
      } else {
        // bit vector updates are only added when the LAST_THREAD != NUM_THREADS -1.
        // this is because the bit vectors are only accessed anyway if LAST_THREAD < some_int holds.
        ImmutableList<CExpressionAssignmentStatement> lastBitVectorUpdates =
            buildLastAccessBitVectorUpdatesByEncoding();
        // for all other target pc, set last_thread to current thread id and update last bitvectors
        return injectLastThreadUpdateIntoStatement(
            pStatement, activeThread.id(), lastBitVectorUpdates);
      }
    }
    // no valid target pc -> no conflict order required
    return pStatement;
  }

  private CSeqThreadStatement injectLastThreadUpdateIntoStatement(
      CSeqThreadStatement pStatement,
      int pLastThreadValue,
      ImmutableList<CExpressionAssignmentStatement> pLastBitVectorUpdates) {

    CExpressionAssignmentStatement lastThreadExit =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpressions.LAST_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(pLastThreadValue));
    SeqLastBitVectorUpdateStatement lastUpdateStatement =
        new SeqLastBitVectorUpdateStatement(lastThreadExit, pLastBitVectorUpdates);
    return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
        pStatement, lastUpdateStatement);
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
