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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.conflict.SeqLastUpdateStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.LastDenseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.LastSparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.SparseBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class ConflictResolver {

  // Public Interface ==============================================================================

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> resolve(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      PointerAssignments pPointerAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.getMemoryLocationAmount() == 0) {
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
      // step 1: inject conflict order assumptions
      ImmutableList<SeqThreadStatementClause> withConflictOrder =
          addConflictOrdersToClauses(
              pOptions,
              pClauses.get(activeThread),
              activeThread,
              labelBlockMap,
              pAllMemoryLocations,
              pBitVectorVariables,
              pPointerAssignments,
              pBinaryExpressionBuilder);
      // step 2: inject updates to last_... variables
      rResolved.putAll(
          activeThread,
          addLastUpdatesToClauses(
              pOptions,
              pClauses.keySet().size(),
              withConflictOrder,
              activeThread,
              pBitVectorVariables));
    }
    return rResolved.build();
  }

  // Conflict Order ================================================================================

  private static ImmutableList<SeqThreadStatementClause> addConflictOrdersToClauses(
      MPOROptions pOptions,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      PointerAssignments pPointerAssignments,
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
                pAllMemoryLocations,
                pBitVectorVariables,
                pPointerAssignments,
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
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      PointerAssignments pPointerAssignments,
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
              pAllMemoryLocations,
              pBitVectorVariables,
              pPointerAssignments,
              pBinaryExpressionBuilder));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement addConflictOrderToStatement(
      MPOROptions pOptions,
      SeqThreadStatement pStatement,
      MPORThread pActiveThread,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      BitVectorVariables pBitVectorVariables,
      PointerAssignments pPointerAssignments,
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
              targetBlock,
              pAllMemoryLocations,
              pBitVectorVariables,
              pPointerAssignments,
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

  // Last Updates ==================================================================================

  private static ImmutableList<SeqThreadStatementClause> addLastUpdatesToClauses(
      MPOROptions pOptions,
      int pNumThreads,
      ImmutableList<SeqThreadStatementClause> pClauses,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables) {

    ImmutableList.Builder<SeqThreadStatementClause> rWithOrders = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.getBlocks()) {
        newBlocks.add(
            addLastUpdatesToBlock(
                pOptions, pNumThreads, mergedBlock, pActiveThread, pBitVectorVariables));
      }
      rWithOrders.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return rWithOrders.build();
  }

  private static SeqThreadStatementBlock addLastUpdatesToBlock(
      MPOROptions pOptions,
      int pNumThreads,
      SeqThreadStatementBlock pBlock,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables) {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(
          addLastUpdatesToStatement(
              pOptions, pNumThreads, statement, pActiveThread, pBitVectorVariables));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement addLastUpdatesToStatement(
      MPOROptions pOptions,
      int pNumThreads,
      SeqThreadStatement pStatement,
      MPORThread pActiveThread,
      BitVectorVariables pBitVectorVariables) {

    if (pStatement.getTargetPc().isPresent()) {
      int targetPc = pStatement.getTargetPc().orElseThrow();
      if (targetPc == Sequentialization.EXIT_PC) {
        // if a thread exits, set last_thread to NUM_THREADS - 1
        CExpressionAssignmentStatement lastThreadExit =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                SeqIdExpression.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pNumThreads));
        SeqLastUpdateStatement lastUpdateStatement =
            new SeqLastUpdateStatement(lastThreadExit, ImmutableList.of());
        return pStatement.cloneAppendingInjectedStatements(ImmutableList.of(lastUpdateStatement));
      } else {
        // for all other target pc, set last_thread to current thread id and update last bitvectors
        CExpressionAssignmentStatement lastThreadUpdate =
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                SeqIdExpression.LAST_THREAD,
                SeqExpressionBuilder.buildIntegerLiteralExpression(pActiveThread.id));
        SeqLastUpdateStatement lastUpdateStatement =
            new SeqLastUpdateStatement(
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
              "no bitVectorEncoding set, but conflictReduction is enabled");
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
              "no reductionMode set, but conflictReduction is enabled");
      case ACCESS_ONLY ->
          buildDenseLastBitVectorUpdatesByAccessType(
              pActiveThread, pBitVectorVariables, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(
                  buildDenseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, BitVectorAccessType.ACCESS))
              .addAll(
                  buildDenseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, BitVectorAccessType.WRITE))
              .build();
    };
  }

  private static ImmutableList<CExpressionAssignmentStatement> buildSparseLastBitVectorUpdates(
      MPOROptions pOptions, MPORThread pActiveThread, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "no reductionMode set, but conflictReduction is enabled");
      case ACCESS_ONLY ->
          buildSparseLastBitVectorUpdatesByAccessType(
              pActiveThread, pBitVectorVariables, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableList.<CExpressionAssignmentStatement>builder()
              .addAll(
                  buildSparseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, BitVectorAccessType.ACCESS))
              .addAll(
                  buildSparseLastBitVectorUpdatesByAccessType(
                      pActiveThread, pBitVectorVariables, BitVectorAccessType.WRITE))
              .build();
    };
  }

  private static ImmutableList<CExpressionAssignmentStatement>
      buildDenseLastBitVectorUpdatesByAccessType(
          MPORThread pActiveThread,
          BitVectorVariables pBitVectorVariables,
          BitVectorAccessType pAccessType) {

    LastDenseBitVector lastDenseBitVector =
        pBitVectorVariables.getLastDenseBitVectorByAccessType(pAccessType);
    CExpression rightHandSide =
        pBitVectorVariables.getDenseReachableBitVectorByAccessType(pAccessType, pActiveThread);
    CExpressionAssignmentStatement update =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            lastDenseBitVector.reachableVariable, rightHandSide);
    return ImmutableList.of(update);
  }

  private static ImmutableList<CExpressionAssignmentStatement>
      buildSparseLastBitVectorUpdatesByAccessType(
          MPORThread pActiveThread,
          BitVectorVariables pBitVectorVariables,
          BitVectorAccessType pAccessType) {

    ImmutableList.Builder<CExpressionAssignmentStatement> rUpdates = ImmutableList.builder();
    ImmutableMap<MemoryLocation, LastSparseBitVector> lastSparseBitVectors =
        pBitVectorVariables.getLastSparseBitVectorByAccessType(pAccessType);
    ImmutableMap<MemoryLocation, SparseBitVector> sparseBitVectors =
        pBitVectorVariables.getSparseBitVectorByAccessType(pAccessType);
    for (var entry : sparseBitVectors.entrySet()) {
      for (var innerEntry : entry.getValue().variables.entrySet()) {
        if (innerEntry.getKey().equals(pActiveThread)) {
          MemoryLocation memoryLocation = entry.getKey();
          LastSparseBitVector lastSparseBitVector = lastSparseBitVectors.get(memoryLocation);
          assert lastSparseBitVector != null;
          CIdExpression rightHandSide = innerEntry.getValue();
          CExpressionAssignmentStatement update =
              SeqStatementBuilder.buildExpressionAssignmentStatement(
                  lastSparseBitVector.variable, rightHandSide);
          rUpdates.add(update);
        }
      }
    }
    return rUpdates.build();
  }
}
