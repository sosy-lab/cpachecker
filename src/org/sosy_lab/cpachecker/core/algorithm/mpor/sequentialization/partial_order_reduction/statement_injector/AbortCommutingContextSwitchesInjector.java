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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.PrevSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables.SeqSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.SeqProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;

public record AbortCommutingContextSwitchesInjector(
    MPOROptions options,
    MPORThread activeThread,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    SeqBitVectorVariables bitVectorVariables,
    SeqPointerAliasingMap pointerAliasingMap,
    SequentializationUtils utils) {

  /**
   * Returns a {@link CIfStatement} that encodes the abortCommutingContextSwitches reduction. The
   * statement precedes a thread simulation and takes the following form:
   *
   * <pre>{@code
   * if (prev_thread < current_thread_id) {
   *    assume(*conflict between prev_thread and current_thread*);
   * }
   * }</pre>
   *
   * <p>This ensures that if {@code prev_thread < current_thread_id}, the simulation performs a
   * context switch only when a conflict exists between the two threads.
   */
  public CIfStatement buildAbortCommutingContextSwitchesStatement(MPORThread pThread)
      throws UnrecognizedCodeException {

    checkArgument(
        !pThread.isMain(),
        "Cannot build a statement for abortCommutingContextSwitches (ACS) when pThread is the main"
            + " thread. The ACS statement contains a guard of the form 'if (prev_thread <"
            + " current_thread_id)' where current_thread_id = 0 for the main thread and prev_thread"
            + " is in the interval [0;NUM_THREADS]. This means that the guard always evaluates to"
            + " false for the main thread, and the ACS statement should be pruned entirely.");

    // prev_thread < n
    CBinaryExpression prevThreadLessThanThreadId =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.PREV_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(activeThread.id()),
                BinaryOperator.LESS_THAN);

    // *conflict between prev_thread and current_thread*
    SeqThreadStatementBlock firstBlock =
        Objects.requireNonNull(labelBlockMap.get(SeqProgramCounterVariables.INIT_PC));
    Optional<CExportExpression> prevBitVectorEvaluation =
        BitVectorEvaluationBuilder.buildPrevBitVectorEvaluation(
            options,
            pThread,
            labelClauseMap,
            labelBlockMap,
            firstBlock,
            bitVectorVariables,
            pointerAliasingMap,
            utils);

    // if (prev_thread < n) ...
    CExpressionWrapper ifCondition = new CExpressionWrapper(prevThreadLessThanThreadId);
    if (prevBitVectorEvaluation.isEmpty()) {
      return new CIfStatement(
          ifCondition,
          // if the evaluation is empty, it results in assume(0) i.e. abort()
          new CCompoundStatement(
              new CStatementWrapper(SeqAssumeFunctionBuilder.ABORT_FUNCTION_CALL_STATEMENT)));
    } else {
      // assume(*conflict*) i.e. continue in thread n only if it is in conflict with prev_thread
      return new CIfStatement(
          ifCondition,
          new CCompoundStatement(
              SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                  prevBitVectorEvaluation.orElseThrow())));
    }
  }

  // Prev Updates ==================================================================================

  SeqThreadStatement injectPrevBitVectorUpdatesIntoStatement(SeqThreadStatement pStatement) {
    if (pStatement.targetPc().isPresent()) {
      ImmutableList<CExpressionAssignmentStatement> prevBitVectorUpdates =
          buildPrevAccessBitVectorUpdatesByEncoding();
      return SeqThreadStatementUtil.appendedInstrumentationStatement(
          pStatement,
          SeqInstrumentationBuilder.buildPrevBitVectorUpdateStatement(prevBitVectorUpdates));
    }
    // no valid target pc -> no conflict order required
    return pStatement;
  }

  // Prev Access Bit Vectors =======================================================================

  private ImmutableList<CExpressionAssignmentStatement>
      buildPrevAccessBitVectorUpdatesByEncoding() {

    return switch (options.bitVectorEncoding()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build updates for bitVectorEncoding %s", options.bitVectorEncoding()));
      case BINARY, OCTAL, DECIMAL, HEXADECIMAL -> buildDensePrevBitVectorUpdates();
      case SPARSE -> buildSparsePrevBitVectorUpdates();
    };
  }

  private ImmutableList<CExpressionAssignmentStatement> buildDensePrevBitVectorUpdates() {
    return switch (options.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build updates for partialOrderReductionMode %s",
                  options.partialOrderReductionMode()));
      case ACCESS_ONLY -> buildDensePrevBitVectorUpdatesByAccessType(SeqMemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(buildDensePrevBitVectorUpdatesByAccessType(SeqMemoryAccessType.READ))
              .addAll(buildDensePrevBitVectorUpdatesByAccessType(SeqMemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<CExpressionAssignmentStatement> buildSparsePrevBitVectorUpdates() {
    return switch (options.partialOrderReductionMode()) {
      case NONE ->
          throw new IllegalArgumentException(
              String.format(
                  "cannot build updates for partialOrderReductionMode %s",
                  options.partialOrderReductionMode()));
      case ACCESS_ONLY -> buildSparsePrevBitVectorUpdatesByAccessType(SeqMemoryAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(buildSparsePrevBitVectorUpdatesByAccessType(SeqMemoryAccessType.READ))
              .addAll(buildSparsePrevBitVectorUpdatesByAccessType(SeqMemoryAccessType.WRITE))
              .build();
    };
  }

  private ImmutableList<CExpressionAssignmentStatement> buildDensePrevBitVectorUpdatesByAccessType(
      SeqMemoryAccessType pAccessType) {

    PrevDenseBitVector prevDenseBitVector =
        bitVectorVariables.getPrevDenseBitVectorByAccessType(pAccessType);
    CExpression rightHandSide =
        bitVectorVariables.getDenseBitVector(activeThread, pAccessType, SeqMemoryReachType.DIRECT);
    CExpressionAssignmentStatement update =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            prevDenseBitVector.directVariable(), rightHandSide);
    return ImmutableList.of(update);
  }

  private ImmutableList<CExpressionAssignmentStatement> buildSparsePrevBitVectorUpdatesByAccessType(
      SeqMemoryAccessType pAccessType) {

    ImmutableList.Builder<CExpressionAssignmentStatement> rUpdates = ImmutableList.builder();
    ImmutableMap<SeqMemoryLocation, PrevSparseBitVector> prevSparseBitVectors =
        bitVectorVariables.getPrevSparseBitVectorByAccessType(pAccessType);
    ImmutableMap<SeqMemoryLocation, SeqSparseBitVector> sparseBitVectors =
        bitVectorVariables.getSparseBitVectorByAccessType(pAccessType);
    for (var entry : sparseBitVectors.entrySet()) {
      Optional<CIdExpression> directVariable =
          entry
              .getValue()
              .tryGetVariableByReachTypeAndThread(SeqMemoryReachType.DIRECT, activeThread);
      // If directVariable is present, then set the prev bit vector to the threads bit vector.
      // Otherwise, then there is no bit vector for the thread and the prev bit vector is set to 0.
      CExpression rightHandSide =
          directVariable.isPresent()
              ? directVariable.orElseThrow()
              : CIntegerLiteralExpression.ZERO;
      PrevSparseBitVector prevSparseBitVector =
          Objects.requireNonNull(prevSparseBitVectors.get(entry.getKey()));
      CExpressionAssignmentStatement update =
          SeqStatementBuilder.buildExpressionAssignmentStatement(
              prevSparseBitVector.directVariable(), rightHandSide);
      rUpdates.add(update);
    }
    return rUpdates.build();
  }
}
