// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqLastBitVectorUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
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
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;

public record ReduceLastThreadOrderInjector(
    MPOROptions options,
    int numThreads,
    MPORThread activeThread,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    BitVectorVariables bitVectorVariables,
    MemoryModel memoryModel,
    SequentializationUtils utils) {

  // Private =======================================================================================

  public SeqBranchStatement buildLastThreadOrderStatement(MPORThread pThread)
      throws UnrecognizedCodeException {

    checkArgument(
        !pThread.isMain(),
        "Cannot build a last thread order statement for the main thread because LAST_THREAD < 0"
            + " never holds.");

    SeqThreadStatementBlock firstBlock =
        Objects.requireNonNull(labelBlockMap.get(ProgramCounterVariables.INIT_PC));
    Optional<BitVectorEvaluationExpression> lastBitVectorEvaluation =
        BitVectorEvaluationBuilder.buildLastBitVectorEvaluation(
            options,
            labelClauseMap,
            labelBlockMap,
            firstBlock,
            bitVectorVariables,
            memoryModel,
            utils);

    // LAST_THREAD < n
    CBinaryExpression lastThreadLessThanThreadId =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.id()),
                BinaryOperator.LESS_THAN);
    // LAST_THREAD_SYNC == 0
    CBinaryExpression lastThreadSyncFalse =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.LAST_THREAD_SYNC,
                SeqIntegerLiteralExpressions.INT_0,
                BinaryOperator.EQUALS);
    // (LAST_THREAD < n && LAST_THREAD_SYNC == 0)
    ExpressionTree<CBinaryExpression> ifGuard =
        And.of(
            LeafExpression.of(lastThreadLessThanThreadId), LeafExpression.of(lastThreadSyncFalse));

    // if (LAST_THREAD < n)
    final String ifBlock;
    if (lastBitVectorEvaluation.isEmpty()) {
      // if the evaluation is empty, it results in assume(0) i.e. abort()
      ifBlock = SeqAssumeFunction.ABORT_FUNCTION_CALL_STATEMENT.toASTString();
    } else {
      // assume(*conflict*) i.e. continue in thread n only if it is not in conflict with LAST_THREAD
      ifBlock =
          SeqAssumeFunction.buildAssumeFunctionCallStatement(
              lastBitVectorEvaluation.orElseThrow().expression());
    }
    return new SeqBranchStatement(ifGuard.toString(), ImmutableList.of(ifBlock));
  }

  // Last Updates ==================================================================================

  CSeqThreadStatement injectLastUpdatesIntoStatement(CSeqThreadStatement pStatement) {
    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
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
