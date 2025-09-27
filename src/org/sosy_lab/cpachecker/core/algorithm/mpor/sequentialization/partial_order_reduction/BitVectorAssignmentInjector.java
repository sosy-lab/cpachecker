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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.assignment.BitVectorAccessAssignmentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.assignment.BitVectorReadWriteAssignmentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorAssignmentInjector {

  // Public Interfaces =============================================================================

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> injectBitVectorAssignments(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      LogManager pLogger) {

    if (pMemoryModel.getRelevantMemoryLocationAmount() == 0) {
      pLogger.log(
          Level.INFO,
          "bit vectors are enabled, but the program does not contain any global memory locations.");
      return pClauses; // no relevant memory locations -> no bit vectors needed
    }
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
          injectBitVectorAssignmentsIntoClauses(
              pOptions,
              activeThread,
              clauses,
              labelClauseMap,
              labelBlockMap,
              pBitVectorVariables,
              pMemoryModel));
    }
    return rInjected.build();
  }

  // Private Methods ===============================================================================

  private static ImmutableList<SeqThreadStatementClause> injectBitVectorAssignmentsIntoClauses(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(
            injectBitVectorAssignmentsIntoBlock(
                pOptions,
                block,
                pActiveThread,
                pLabelClauseMap,
                pLabelBlockMap,
                pBitVectorVariables,
                pMemoryModel));
      }
      rInjected.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return rInjected.build();
  }

  private static SeqThreadStatementBlock injectBitVectorAssignmentsIntoBlock(
      MPOROptions pOptions,
      SeqThreadStatementBlock pBlock,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(
          injectBitVectorAssignmentsIntoStatement(
              pOptions,
              pActiveThread,
              statement,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement injectBitVectorAssignmentsIntoStatement(
      MPOROptions pOptions,
      final MPORThread pActiveThread,
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      final BitVectorVariables pBitVectorVariables,
      final MemoryModel pMemoryModel) {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorResets =
            buildBitVectorResetsByReductionMode(
                pOptions, pActiveThread, pBitVectorVariables, pMemoryModel);
        newInjected.addAll(bitVectorResets);
        return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target block
        SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        // the assignment is injected after the evaluation, it is only needed when commute fails
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorAssignmentsByReduction(
                pOptions,
                pActiveThread,
                newTarget.getFirstBlock(),
                pLabelClauseMap,
                pLabelBlockMap,
                pBitVectorVariables,
                pMemoryModel);
        newInjected.addAll(bitVectorAssignments);
        return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }

  // Bit Vector Assignments ========================================================================

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorResetsByReductionMode(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build assignments for reduction " + pOptions.bitVectorReduction);
      case ACCESS_ONLY ->
          BitVectorAccessAssignmentBuilder.buildAccessBitVectorAssignments(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              ImmutableSet.of(),
              ImmutableSet.of());
      case READ_AND_WRITE ->
          BitVectorReadWriteAssignmentBuilder.buildReadWriteBitVectorAssignments(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              ImmutableSet.of(),
              ImmutableSet.of(),
              ImmutableSet.of(),
              ImmutableSet.of());
    };
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement>
      buildBitVectorAssignmentsByReduction(
          MPOROptions pOptions,
          MPORThread pActiveThread,
          SeqThreadStatementBlock pTargetBlock,
          ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
          ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
          BitVectorVariables pBitVectorVariables,
          MemoryModel pMemoryModel) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build assignments for reduction " + pOptions.bitVectorReduction);
      case ACCESS_ONLY ->
          BitVectorAccessAssignmentBuilder.buildAccessBitVectorAssignments(
              pOptions,
              pActiveThread,
              pTargetBlock,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel);
      case READ_AND_WRITE ->
          BitVectorReadWriteAssignmentBuilder.buildReadWriteBitVectorAssignments(
              pOptions,
              pActiveThread,
              pTargetBlock,
              pLabelClauseMap,
              pLabelBlockMap,
              pBitVectorVariables,
              pMemoryModel);
    };
  }
}
