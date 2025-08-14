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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.value.BitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.value.SparseBitVectorValueExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorInjector {

  // Public Interfaces =============================================================================

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> injectWithEvaluations(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    return inject(
        pOptions,
        true,
        pBitVectorVariables,
        pClauses,
        pPointerAssignments,
        pPointerParameterAssignments,
        pBinaryExpressionBuilder,
        pLogger);
  }

  static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> injectWithoutEvaluations(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    return inject(
        pOptions,
        false,
        pBitVectorVariables,
        pClauses,
        pPointerAssignments,
        pPointerParameterAssignments,
        pBinaryExpressionBuilder,
        pLogger);
  }

  // Private =======================================================================================
  // TODO rename the methods, the names are not very concise

  private static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> inject(
      MPOROptions pOptions,
      boolean pAddEvaluation,
      BitVectorVariables pBitVectorVariables,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.getNumGlobalVariables() == 0) {
      pLogger.log(
          Level.INFO,
          "bit vectors are enabled, but the program does not contain any global variables.");
      return pClauses; // no global variables -> no bit vectors needed
    }
    ImmutableListMultimap.Builder<MPORThread, SeqThreadStatementClause> rInjected =
        ImmutableListMultimap.builder();
    for (MPORThread activeThread : pClauses.keySet()) {
      ImmutableSet<MPORThread> otherThreads =
          MPORUtil.withoutElement(pClauses.keySet(), activeThread);
      ImmutableList<SeqThreadStatementClause> clauses = pClauses.get(activeThread);
      ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToClause(clauses);
      ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap =
          SeqThreadStatementClauseUtil.mapLabelNumberToBlock(clauses);
      rInjected.putAll(
          activeThread,
          injectBitVectors(
              pOptions,
              pAddEvaluation,
              activeThread,
              otherThreads,
              pBitVectorVariables,
              clauses,
              labelClauseMap,
              labelBlockMap,
              pPointerAssignments,
              pPointerParameterAssignments,
              pBinaryExpressionBuilder));
    }
    return rInjected.build();
  }

  private static ImmutableList<SeqThreadStatementClause> injectBitVectors(
      MPOROptions pOptions,
      boolean pAddEvaluation,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqThreadStatementClause> pClauses,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    for (SeqThreadStatementClause clause : pClauses) {
      ImmutableList.Builder<SeqThreadStatementBlock> newBlocks = ImmutableList.builder();
      for (SeqThreadStatementBlock block : clause.getBlocks()) {
        newBlocks.add(
            injectBitVectorsIntoBlock(
                pOptions,
                pAddEvaluation,
                block,
                pActiveThread,
                pOtherThreads,
                pBitVectorVariables,
                pLabelClauseMap,
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pBinaryExpressionBuilder));
      }
      rInjected.add(clause.cloneWithBlocks(newBlocks.build()));
    }
    return rInjected.build();
  }

  private static SeqThreadStatementBlock injectBitVectorsIntoBlock(
      MPOROptions pOptions,
      boolean pAddEvaluation,
      SeqThreadStatementBlock pBlock,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      newStatements.add(
          injectBitVectorsIntoStatement(
              pOptions,
              pAddEvaluation,
              pActiveThread,
              pOtherThreads,
              statement,
              pBitVectorVariables,
              pLabelClauseMap,
              pLabelBlockMap,
              pPointerAssignments,
              pPointerParameterAssignments,
              pBinaryExpressionBuilder));
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }

  private static SeqThreadStatement injectBitVectorsIntoStatement(
      MPOROptions pOptions,
      boolean pAddEvaluation,
      final MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqThreadStatement pCurrentStatement,
      final BitVectorVariables pBitVectorVariables,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      final ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      final ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int intTargetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (intTargetPc == Sequentialization.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorResets =
            buildBitVectorResetsByReduction(pOptions, pActiveThread, pBitVectorVariables);
        newInjected.addAll(bitVectorResets);
        return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target block
        SeqThreadStatementClause newTarget =
            Objects.requireNonNull(pLabelClauseMap.get(intTargetPc));
        // TODO this check is only necessary if only next_thread nondeterminism is active
        if (!SeqThreadStatementUtil.anySynchronizesThreads(newTarget.getAllStatements())) {
          if (pAddEvaluation) {
            SeqBitVectorEvaluationStatement evaluationStatement =
                buildBitVectorEvaluationStatement(
                    pOptions,
                    pOtherThreads,
                    pLabelBlockMap,
                    pPointerAssignments,
                    pPointerParameterAssignments,
                    newTarget.getFirstBlock(),
                    pBitVectorVariables,
                    pBinaryExpressionBuilder);
            newInjected.add(evaluationStatement);
          }
          // the assignment is injected after the evaluation, it is only needed when commute fails
          ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
              buildBitVectorAssignmentsByReduction(
                  pOptions,
                  pActiveThread,
                  newTarget.getFirstBlock(),
                  pLabelClauseMap,
                  pLabelBlockMap,
                  pPointerAssignments,
                  pPointerParameterAssignments,
                  pBitVectorVariables);
          newInjected.addAll(bitVectorAssignments);
          return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
        }
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }

  // Bit Vector Evaluations =======================================================================

  private static SeqBitVectorEvaluationStatement buildBitVectorEvaluationStatement(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      SeqThreadStatementBlock pTargetBlock,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    BitVectorEvaluationExpression evaluationExpression =
        BitVectorEvaluationBuilder.buildEvaluationByDirectVariableAccesses(
            pOptions,
            pOtherThreads,
            pLabelBlockMap,
            pPointerAssignments,
            pPointerParameterAssignments,
            pTargetBlock,
            pBitVectorVariables,
            pBinaryExpressionBuilder);
    return new SeqBitVectorEvaluationStatement(evaluationExpression, pTargetBlock.getLabel());
  }

  // Bit Vector Assignments ========================================================================

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorResetsByReduction(
      MPOROptions pOptions, MPORThread pThread, BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build assignments for reduction " + pOptions.bitVectorReduction);
      case ACCESS_ONLY ->
          buildBitVectorAccessAssignments(
              pOptions, pThread, pBitVectorVariables, ImmutableSet.of(), ImmutableSet.of());
      case READ_AND_WRITE ->
          buildBitVectorReadWriteAssignments(
              pOptions,
              pThread,
              pBitVectorVariables,
              ImmutableSet.of(),
              ImmutableSet.of(),
              ImmutableSet.of(),
              ImmutableSet.of());
    };
  }

  public static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAssignmentsByReduction(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      SeqThreadStatementBlock pTargetBlock,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      BitVectorVariables pBitVectorVariables) {

    return switch (pOptions.reductionMode) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build assignments for reduction " + pOptions.bitVectorReduction);
      case ACCESS_ONLY -> {
        ImmutableSet<CVariableDeclaration> directVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pTargetBlock,
                BitVectorAccessType.ACCESS);
        ImmutableSet<CVariableDeclaration> reachableVariables =
            GlobalVariableFinder.findReachableGlobalVariablesByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pTargetBlock,
                BitVectorAccessType.ACCESS);
        yield buildBitVectorAccessAssignments(
            pOptions, pActiveThread, pBitVectorVariables, directVariables, reachableVariables);
      }
      case READ_AND_WRITE -> {
        ImmutableSet<CVariableDeclaration> directReadVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pTargetBlock,
                BitVectorAccessType.READ);
        ImmutableSet<CVariableDeclaration> reachableWriteVariables =
            GlobalVariableFinder.findReachableGlobalVariablesByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pTargetBlock,
                BitVectorAccessType.WRITE);

        ImmutableSet<CVariableDeclaration> directWriteVariables =
            GlobalVariableFinder.findDirectGlobalVariablesByAccessType(
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pTargetBlock,
                BitVectorAccessType.WRITE);
        ImmutableSet<CVariableDeclaration> reachableReadVariables =
            GlobalVariableFinder.findReachableGlobalVariablesByAccessType(
                pLabelClauseMap,
                pLabelBlockMap,
                pPointerAssignments,
                pPointerParameterAssignments,
                pTargetBlock,
                BitVectorAccessType.READ);

        yield buildBitVectorReadWriteAssignments(
            pOptions,
            pActiveThread,
            pBitVectorVariables,
            directReadVariables,
            reachableWriteVariables,
            directWriteVariables,
            // combine both read and write for access
            ImmutableSet.<CVariableDeclaration>builder()
                .addAll(reachableReadVariables)
                .addAll(reachableWriteVariables)
                .build());
      }
    };
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAccessAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableSet<CVariableDeclaration> pDirectVariables,
      ImmutableSet<CVariableDeclaration> pReachableVariables) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SPARSE)) {
      for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
        boolean value = pReachableVariables.contains(entry.getKey());
        SparseBitVectorValueExpression sparseBitVectorExpression =
            new SparseBitVectorValueExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                accessVariables.get(pThread), sparseBitVectorExpression));
      }
    } else {
      if (pOptions.kIgnoreZeroReduction) {
        CExpression directVariable =
            pBitVectorVariables.getDenseDirectBitVectorByAccessType(
                BitVectorAccessType.ACCESS, pThread);
        BitVectorValueExpression directValue =
            BitVectorUtil.buildBitVectorExpression(
                pOptions, pBitVectorVariables.getGlobalVariableIds(), pDirectVariables);
        rStatements.add(new SeqBitVectorAssignmentStatement(directVariable, directValue));
      }
      CExpression reachableVariable =
          pBitVectorVariables.getDenseReachableBitVectorByAccessType(
              BitVectorAccessType.ACCESS, pThread);
      BitVectorValueExpression reachableValue =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.getGlobalVariableIds(), pReachableVariables);
      rStatements.add(new SeqBitVectorAssignmentStatement(reachableVariable, reachableValue));
    }
    return rStatements.build();
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorReadWriteAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pReachableWriteVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      ImmutableSet<CVariableDeclaration> pReachableAccessVariables) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SPARSE)) {
      // sparse access bit vectors
      for (var entry : pBitVectorVariables.getSparseAccessBitVectors().entrySet()) {
        CVariableDeclaration variable = entry.getKey();
        ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
        boolean value = pReachableAccessVariables.contains(variable);
        SparseBitVectorValueExpression sparseBitVectorExpression =
            new SparseBitVectorValueExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                accessVariables.get(pThread), sparseBitVectorExpression));
      }
      // sparse write bit vectors
      for (var entry : pBitVectorVariables.getSparseWriteBitVectors().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> writeVariables = entry.getValue().variables;
        boolean value = pReachableWriteVariables.contains(entry.getKey());
        SparseBitVectorValueExpression sparseBitVectorExpression =
            new SparseBitVectorValueExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                writeVariables.get(pThread), sparseBitVectorExpression));
      }
    } else {
      // dense bit vectors
      if (pOptions.kIgnoreZeroReduction) {
        rStatements.add(
            buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
                pOptions,
                pBitVectorVariables.getDenseDirectBitVectorByAccessType(
                    BitVectorAccessType.READ, pThread),
                pBitVectorVariables,
                pDirectReadVariables));
      }
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
              pOptions,
              pBitVectorVariables.getDenseReachableBitVectorByAccessType(
                  BitVectorAccessType.WRITE, pThread),
              pBitVectorVariables,
              pReachableWriteVariables));
      if (pOptions.kIgnoreZeroReduction) {
        rStatements.add(
            buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
                pOptions,
                pBitVectorVariables.getDenseDirectBitVectorByAccessType(
                    BitVectorAccessType.WRITE, pThread),
                pBitVectorVariables,
                pDirectWriteVariables));
      }
      rStatements.add(
          buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
              pOptions,
              pBitVectorVariables.getDenseReachableBitVectorByAccessType(
                  BitVectorAccessType.ACCESS, pThread),
              pBitVectorVariables,
              pReachableAccessVariables));
    }
    return rStatements.build();
  }

  private static SeqBitVectorAssignmentStatement
      buildDenseBitVectorReadWriteAssignmentStatementByAccessType(
          MPOROptions pOptions,
          CExpression pBitVectorVariable,
          BitVectorVariables pBitVectorVariables,
          ImmutableSet<CVariableDeclaration> pAccessedVariables) {

    BitVectorValueExpression bitVectorExpression =
        BitVectorUtil.buildBitVectorExpression(
            pOptions, pBitVectorVariables.getGlobalVariableIds(), pAccessedVariables);
    return new SeqBitVectorAssignmentStatement(pBitVectorVariable, bitVectorExpression);
  }
}
