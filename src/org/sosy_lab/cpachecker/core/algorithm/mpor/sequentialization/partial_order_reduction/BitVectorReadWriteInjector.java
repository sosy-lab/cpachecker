// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.ScalarBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorReadWriteEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReachType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorReadWriteInjector {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> inject(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.globalVariableIds.isEmpty()) {
      pLogger.log(
          Level.INFO,
          "bit vectors are enabled, but the program does not contain any global variables.");
      return pClauses; // no global variables -> no bit vectors
    }
    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> injected =
        ImmutableMap.builder();
    for (var entry : pClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      Optional<BitVectorEvaluationExpression> bitVectorEvaluation =
          BitVectorEvaluationBuilder.buildReadWriteBitVectorEvaluationByEncoding(
              pOptions, thread, pBitVectorVariables, pBinaryExpressionBuilder);
      injected.put(
          entry.getKey(),
          injectBitVectors(
              pOptions,
              entry.getKey(),
              pBitVectorVariables,
              entry.getValue(),
              bitVectorEvaluation,
              pBinaryExpressionBuilder));
    }
    return injectBitVectorInitializations(pOptions, pBitVectorVariables, injected.buildOrThrow());
  }

  private static ImmutableList<SeqThreadStatementClause> injectBitVectors(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqThreadStatementClause> pClauses,
      Optional<BitVectorEvaluationExpression> pBitVectorEvaluation,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pClauses);
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToBlock(pClauses);
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        newStatements.add(
            injectBitVectorsIntoSingleStatement(
                pOptions,
                pThread,
                pBitVectorEvaluation,
                statement,
                pBitVectorVariables,
                labelClauseMap,
                labelBlockMap,
                pBinaryExpressionBuilder));
      }
      SeqThreadStatementBlock newBlock = clause.block.cloneWithStatements(newStatements.build());
      ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
        ImmutableList.Builder<SeqThreadStatement> newMergedStatements = ImmutableList.builder();
        for (SeqThreadStatement statement : mergedBlock.getStatements()) {
          newMergedStatements.add(
              injectBitVectorsIntoSingleStatement(
                  pOptions,
                  pThread,
                  pBitVectorEvaluation,
                  statement,
                  pBitVectorVariables,
                  labelClauseMap,
                  labelBlockMap,
                  pBinaryExpressionBuilder));
        }
        SeqThreadStatementBlock newMergedBlock =
            mergedBlock.cloneWithStatements(newMergedStatements.build());
        newMergedBlocks.add(newMergedBlock);
      }
      rInjected.add(clause.cloneWithBlock(newBlock).cloneWithMergedBlocks(newMergedBlocks.build()));
    }
    return rInjected.build();
  }

  private static SeqThreadStatement injectBitVectorsIntoSingleStatement(
      MPOROptions pOptions,
      final MPORThread pThread,
      final Optional<BitVectorEvaluationExpression> pFullBitVectorEvaluation,
      SeqThreadStatement pCurrentStatement,
      final BitVectorVariables pBitVectorVariables,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      // inject previous injected statements, e.g. mutex lock
      newInjected.addAll(pCurrentStatement.getInjectedStatements());
      int intTargetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (intTargetPc == Sequentialization.EXIT_PC) {
        // exit pc -> reset bit vector to 0s so that no interference with still active threads
        newInjected.addAll(
            buildBitVectorReadWriteZeroAssignments(pOptions, pThread, pBitVectorVariables));
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target case
        SeqThreadStatementClause newTarget =
            Objects.requireNonNull(pLabelClauseMap.get(intTargetPc));
        // always need context switch when targeting critical section start -> no bit vectors
        if (!PartialOrderReducer.requiresAssumeEvaluation(pCurrentStatement, newTarget)) {
          ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
              buildBitVectorReadWriteAssignments(
                  pOptions,
                  pThread,
                  pBitVectorVariables,
                  newTarget.block,
                  pLabelClauseMap,
                  pLabelBlockMap);
          newInjected.addAll(bitVectorAssignments);
          SeqBitVectorReadWriteEvaluationStatement evaluation =
              buildBitVectorReadWriteEvaluationStatements(
                  bitVectorAssignments,
                  BitVectorEvaluationBuilder.buildPrunedReadWriteBitVectorEvaluationByEncoding(
                      pOptions,
                      pThread,
                      bitVectorAssignments,
                      pBitVectorVariables,
                      pFullBitVectorEvaluation,
                      pBinaryExpressionBuilder),
                  newTarget);
          newInjected.add(evaluation);
        }
      }
      return pCurrentStatement.cloneWithInjectedStatements(newInjected.build());
    }
    // no valid target pc (e.g. exit pc) -> return statement as is
    return pCurrentStatement;
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement>
      buildBitVectorReadWriteZeroAssignments(
          MPOROptions pOptions, MPORThread pThread, BitVectorVariables pBitVectorVariables) {

    return buildBitVectorReadWriteAssignments(
        pOptions,
        pThread,
        pBitVectorVariables,
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of());
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorReadWriteAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableSet<CVariableDeclaration> pReachableReadVariables,
      ImmutableSet<CVariableDeclaration> pReachableWrittenVariables) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SCALAR)) {
      for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
        boolean value = pReachableReadVariables.contains(entry.getKey());
        ScalarBitVectorExpression scalarBitVectorExpression = new ScalarBitVectorExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                // TODO change later with scalar reachable bit vectors
                BitVectorReachType.DIRECT, readVariables.get(pThread), scalarBitVectorExpression));
      }
      for (var entry : pBitVectorVariables.scalarWriteBitVectors.orElseThrow().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> writeVariables = entry.getValue().variables;
        boolean value = pReachableWrittenVariables.contains(entry.getKey());
        ScalarBitVectorExpression scalarBitVectorExpression = new ScalarBitVectorExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                // TODO change later with scalar reachable bit vectors
                BitVectorReachType.DIRECT, writeVariables.get(pThread), scalarBitVectorExpression));
      }
    } else {
      // TODO the assignments can be split, the reachable assignment is only required if the
      //  commutes check fails
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByReachType(
              pOptions,
              pThread,
              pBitVectorVariables,
              BitVectorAccessType.READ,
              BitVectorReachType.DIRECT,
              pDirectReadVariables));
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByReachType(
              pOptions,
              pThread,
              pBitVectorVariables,
              BitVectorAccessType.READ,
              BitVectorReachType.REACHABLE,
              pReachableReadVariables));
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByReachType(
              pOptions,
              pThread,
              pBitVectorVariables,
              BitVectorAccessType.WRITE,
              BitVectorReachType.DIRECT,
              pDirectWriteVariables));
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByReachType(
              pOptions,
              pThread,
              pBitVectorVariables,
              BitVectorAccessType.WRITE,
              BitVectorReachType.REACHABLE,
              pReachableWrittenVariables));
    }
    return rStatements.build();
  }

  private static SeqBitVectorAssignmentStatement
      buildDenseBitVectorReadWriteAssignmentStatementByReachType(
          MPOROptions pOptions,
          MPORThread pThread,
          BitVectorVariables pBitVectorVariables,
          BitVectorAccessType pAccessType,
          BitVectorReachType pReachType,
          ImmutableSet<CVariableDeclaration> pAccessedVariables) {

    CExpression bitVectorVariable =
        pBitVectorVariables.getDenseBitVectorByAccessAndReachType(pAccessType, pReachType, pThread);
    BitVectorExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(
            pOptions, pBitVectorVariables.globalVariableIds, pAccessedVariables);
    return new SeqBitVectorAssignmentStatement(pReachType, bitVectorVariable, bitVectorExpression);
  }

  private static SeqBitVectorReadWriteEvaluationStatement
      buildBitVectorReadWriteEvaluationStatements(
          ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
          BitVectorEvaluationExpression pBitVectorEvaluation,
          SeqThreadStatementClause pTarget) {

    boolean allZero =
        BitVectorUtil.areAllZeroAssignmentsByReachType(
            BitVectorReachType.DIRECT, pBitVectorAssignments);
    Optional<BitVectorEvaluationExpression> expression =
        allZero ? Optional.empty() : Optional.of(pBitVectorEvaluation);
    return new SeqBitVectorReadWriteEvaluationStatement(expression, pTarget.block.getGotoLabel());
  }

  // Bit Vector Initialization =====================================================================

  /**
   * Injects proper initializations of the threads respective bit vectors based on their first
   * statement when the respective thread is actually created.
   */
  private static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>>
      injectBitVectorInitializations(
          MPOROptions pOptions,
          BitVectorVariables pBitVectorVariables,
          ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pWithBitVectors) {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rInjected =
        ImmutableMap.builder();
    for (var entry : pWithBitVectors.entrySet()) {
      ImmutableList.Builder<SeqThreadStatementClause> newClauses = ImmutableList.builder();
      for (SeqThreadStatementClause clause : entry.getValue()) {
        SeqThreadStatementBlock newBlock =
            injectBitVectorInitializationsIntoBlock(
                pOptions, pBitVectorVariables, clause.block, pWithBitVectors);
        ImmutableList.Builder<SeqThreadStatementBlock> newMergedBlocks = ImmutableList.builder();
        for (SeqThreadStatementBlock mergedBlock : clause.mergedBlocks) {
          newMergedBlocks.add(
              injectBitVectorInitializationsIntoBlock(
                  pOptions, pBitVectorVariables, mergedBlock, pWithBitVectors));
        }
        newClauses.add(
            clause.cloneWithBlock(newBlock).cloneWithMergedBlocks(newMergedBlocks.build()));
      }
      rInjected.put(entry.getKey(), newClauses.build());
    }
    return rInjected.buildOrThrow();
  }

  private static SeqThreadStatementBlock injectBitVectorInitializationsIntoBlock(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      SeqThreadStatementBlock pBlock,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses) {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      if (statement instanceof SeqThreadCreationStatement threadCreation) {
        MPORThread createdThread = threadCreation.createdThread;
        SeqThreadStatementClause firstClause =
            Objects.requireNonNull(pClauses.get(createdThread)).get(0);
        ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
            SeqThreadStatementClauseUtil.mapLabelNumberToClause(
                Objects.requireNonNull(pClauses.get(createdThread)));
        ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
            SeqThreadStatementClauseUtil.mapLabelNumberToBlock(
                Objects.requireNonNull(pClauses.get(createdThread)));

        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorReadWriteAssignments(
                pOptions,
                threadCreation.createdThread,
                pBitVectorVariables,
                firstClause.block,
                labelClauseMap,
                labelBlockMap);

        newStatements.add(threadCreation.cloneWithBitVectorAssignments(bitVectorAssignments));
      } else {
        newStatements.add(statement);
      }
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  // Helper ========================================================================================

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorReadWriteAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      SeqThreadStatementBlock pBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    ImmutableSet<CVariableDeclaration> directReadVariables =
        GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
            pLabelBlockMap, pBlock, BitVectorAccessType.READ);
    ImmutableSet<CVariableDeclaration> directWrittenVariables =
        GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
            pLabelBlockMap, pBlock, BitVectorAccessType.WRITE);

    ImmutableSet<CVariableDeclaration> reachableReadVariables =
        GlobalVariableFinder.findReachableGlobalVariablesByAccessType(
            pLabelClauseMap, pLabelBlockMap, pBlock, BitVectorAccessType.READ);
    ImmutableSet<CVariableDeclaration> reachableWrittenVariables =
        GlobalVariableFinder.findReachableGlobalVariablesByAccessType(
            pLabelClauseMap, pLabelBlockMap, pBlock, BitVectorAccessType.WRITE);

    return buildBitVectorReadWriteAssignments(
        pOptions,
        pThread,
        pBitVectorVariables,
        directReadVariables,
        directWrittenVariables,
        reachableReadVariables,
        reachableWrittenVariables);
  }
}
