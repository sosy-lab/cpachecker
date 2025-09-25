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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.value_expression.SparseBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocationFinder;
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
            buildBitVectorResetsByReduction(
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

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorResetsByReduction(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build assignments for reduction " + pOptions.bitVectorReduction);
      case ACCESS_ONLY ->
          buildBitVectorAccessAssignments(
              pOptions,
              pThread,
              pBitVectorVariables,
              pMemoryModel,
              ImmutableSet.of(),
              ImmutableSet.of());
      case READ_AND_WRITE ->
          buildBitVectorReadWriteAssignments(
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
      case ACCESS_ONLY -> {
        ImmutableSet<MemoryLocation> directLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.ACCESS);
        ImmutableSet<MemoryLocation> reachableLocations =
            MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pTargetBlock,
                pMemoryModel,
                MemoryAccessType.ACCESS);
        yield buildBitVectorAccessAssignments(
            pOptions,
            pActiveThread,
            pBitVectorVariables,
            pMemoryModel,
            directLocations,
            reachableLocations);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<MemoryLocation> directReadLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.READ);
        ImmutableSet<MemoryLocation> reachableWriteLocations =
            MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pTargetBlock,
                pMemoryModel,
                MemoryAccessType.WRITE);

        ImmutableSet<MemoryLocation> directWriteLocations =
            MemoryLocationFinder.findDirectMemoryLocationsByAccessType(
                pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.WRITE);
        ImmutableSet<MemoryLocation> reachableReadLocations =
            MemoryLocationFinder.findReachableMemoryLocationsByAccessType(
                pLabelClauseMap, pLabelBlockMap, pTargetBlock, pMemoryModel, MemoryAccessType.READ);

        yield buildBitVectorReadWriteAssignments(
            pOptions,
            pActiveThread,
            pBitVectorVariables,
            pMemoryModel,
            directReadLocations,
            reachableWriteLocations,
            directWriteLocations,
            // combine both read and write for access
            ImmutableSet.<MemoryLocation>builder()
                .addAll(reachableReadLocations)
                .addAll(reachableWriteLocations)
                .build());
      }
    };
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAccessAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<MemoryLocation> pDirectMemoryLocations,
      ImmutableSet<MemoryLocation> pReachableMemoryLocations) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SPARSE)) {
      rStatements.addAll(
          buildSparseBitVectorAssignmentsByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pReachableMemoryLocations,
              MemoryAccessType.ACCESS));
    } else {
      if (pOptions.kIgnoreZeroReduction) {
        CExpression directVariable =
            pBitVectorVariables.getDenseDirectBitVectorByAccessType(
                MemoryAccessType.ACCESS, pThread);
        BitVectorValueExpression directValue =
            BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, pDirectMemoryLocations);
        rStatements.add(new SeqBitVectorAssignmentStatement(directVariable, directValue));
      }
      CExpression reachableVariable =
          pBitVectorVariables.getDenseReachableBitVectorByAccessType(
              MemoryAccessType.ACCESS, pThread);
      BitVectorValueExpression reachableValue =
          BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, pReachableMemoryLocations);
      rStatements.add(new SeqBitVectorAssignmentStatement(reachableVariable, reachableValue));
    }
    return rStatements.build();
  }

  // TODO split into several functions
  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorReadWriteAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      MemoryModel pMemoryModel,
      ImmutableSet<MemoryLocation> pDirectReadMemoryLocations,
      ImmutableSet<MemoryLocation> pReachableWriteMemoryLocations,
      ImmutableSet<MemoryLocation> pDirectWriteMemoryLocations,
      ImmutableSet<MemoryLocation> pReachableAccessMemoryLocations) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SPARSE)) {
      rStatements.addAll(
          buildSparseBitVectorAssignmentsByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pReachableAccessMemoryLocations,
              MemoryAccessType.ACCESS));
      rStatements.addAll(
          buildSparseBitVectorAssignmentsByAccessType(
              pOptions,
              pThread,
              pBitVectorVariables,
              pReachableWriteMemoryLocations,
              MemoryAccessType.WRITE));
    } else {
      if (pOptions.kIgnoreZeroReduction) {
        rStatements.add(
            buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
                pOptions,
                pBitVectorVariables.getDenseDirectBitVectorByAccessType(
                    MemoryAccessType.READ, pThread),
                pMemoryModel,
                pDirectReadMemoryLocations));
        rStatements.add(
            buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
                pOptions,
                pBitVectorVariables.getDenseDirectBitVectorByAccessType(
                    MemoryAccessType.WRITE, pThread),
                pMemoryModel,
                pDirectWriteMemoryLocations));
      }
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
              pOptions,
              pBitVectorVariables.getDenseReachableBitVectorByAccessType(
                  MemoryAccessType.ACCESS, pThread),
              pMemoryModel,
              pReachableAccessMemoryLocations));
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
              pOptions,
              pBitVectorVariables.getDenseReachableBitVectorByAccessType(
                  MemoryAccessType.WRITE, pThread),
              pMemoryModel,
              pReachableWriteMemoryLocations));
    }
    return rStatements.build();
  }

  private static SeqBitVectorAssignmentStatement
      buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
          MPOROptions pOptions,
          CExpression pBitVectorVariable,
          MemoryModel pMemoryModel,
          ImmutableSet<MemoryLocation> pAccessedMemoryLocations) {

    BitVectorValueExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(pOptions, pMemoryModel, pAccessedMemoryLocations);
    return new SeqBitVectorAssignmentStatement(pBitVectorVariable, bitVectorExpression);
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement>
      buildSparseBitVectorAssignmentsByAccessType(
          MPOROptions pOptions,
          MPORThread pThread,
          BitVectorVariables pBitVectorVariables,
          ImmutableSet<MemoryLocation> pReachableMemoryLocations,
          MemoryAccessType pAccessType) {

    // use list so that the assignment order is deterministic
    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rAssignments = ImmutableList.builder();
    for (var entry : pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType).entrySet()) {
      ImmutableMap<MPORThread, CIdExpression> variables = entry.getValue().variables;
      Optional<SeqBitVectorAssignmentStatement> assignment =
          buildSparseBitVectorAssignment(
              pOptions, entry.getKey(), pReachableMemoryLocations, variables.get(pThread));
      if (assignment.isPresent()) {
        rAssignments.add(assignment.orElseThrow());
      }
    }
    return rAssignments.build();
  }

  private static Optional<SeqBitVectorAssignmentStatement> buildSparseBitVectorAssignment(
      MPOROptions pOptions,
      MemoryLocation pMemoryLocation,
      ImmutableSet<MemoryLocation> pReachableMemoryLocations,
      CIdExpression pVariable) {

    // if enabled, consider only 0 writes (the memory location is not reachable anymore)
    if (!pOptions.pruneBitVectorWrite || !pReachableMemoryLocations.contains(pMemoryLocation)) {
      boolean value = pReachableMemoryLocations.contains(pMemoryLocation);
      SparseBitVectorValueExpression sparseBitVectorExpression =
          new SparseBitVectorValueExpression(value);
      return Optional.of(new SeqBitVectorAssignmentStatement(pVariable, sparseBitVectorExpression));
    }
    return Optional.empty();
  }
}
