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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAccessEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorAccessInjector {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> inject(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.globalVariableIds.isEmpty()) {
      pLogger.log(
          Level.WARNING,
          "Bit Vectors over global variables are enabled, but the input program does not contain"
              + " any global variables.");
      return pCaseClauses; // no global variables -> no bit vectors
    }
    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> injected =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      Optional<BitVectorEvaluationExpression> fullBitVectorEvaluation =
          BitVectorEvaluationBuilder.buildBitVectorAccessEvaluationByEncoding(
              pOptions, thread, pBitVectorVariables, pBinaryExpressionBuilder);
      injected.put(
          entry.getKey(),
          injectBitVectors(
              pOptions,
              entry.getKey(),
              pBitVectorVariables,
              entry.getValue(),
              fullBitVectorEvaluation));
    }
    return injectBitVectorInitializations(pOptions, pBitVectorVariables, injected.buildOrThrow());
  }

  private static ImmutableList<SeqThreadStatementClause> injectBitVectors(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      Optional<BitVectorEvaluationExpression> pFullBitVectorEvaluation) {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapLabelNumberToClause(pCaseClauses);
    for (SeqThreadStatementClause clause : pCaseClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : clause.block.getStatements()) {
        newStatements.add(
            injectBitVectorsIntoSingleStatement(
                pOptions,
                pThread,
                pFullBitVectorEvaluation,
                statement,
                pBitVectorVariables,
                labelValueMap));
      }
      rInjected.add(clause.cloneWithBlock(clause.block.cloneWithStatements(newStatements.build())));
    }
    return rInjected.build();
  }

  private static SeqThreadStatement injectBitVectorsIntoSingleStatement(
      MPOROptions pOptions,
      final MPORThread pThread,
      final Optional<BitVectorEvaluationExpression> pFullBitVectorEvaluation,
      SeqThreadStatement pCurrentStatement,
      final BitVectorVariables pBitVectorVariables,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelValueMap) {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      // inject previous injected statements, e.g. mutex lock
      newInjected.addAll(pCurrentStatement.getInjectedStatements());
      int intTargetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (intTargetPc == Sequentialization.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        newInjected.addAll(
            buildBitVectorAssignments(pOptions, pThread, pBitVectorVariables, ImmutableList.of()));
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target case
        SeqThreadStatementClause newTarget =
            Objects.requireNonNull(pLabelValueMap.get(intTargetPc));
        // always need context switch when targeting critical section start -> no bit vectors
        if (!newTarget.isCriticalSectionStart()) {
          ImmutableList<CVariableDeclaration> accessedVariables =
              SeqThreadStatementClauseUtil.findGlobalVariablesInCaseClauseByReductionType(
                  newTarget, BitVectorReduction.ACCESS_ONLY);
          ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
              buildBitVectorAssignments(pOptions, pThread, pBitVectorVariables, accessedVariables);
          newInjected.addAll(bitVectorAssignments);
          Optional<SeqBitVectorAccessEvaluationStatement> evaluation =
              buildBitVectorEvaluationStatements(
                  pCurrentStatement,
                  bitVectorAssignments,
                  BitVectorEvaluationBuilder.buildPrunedAccessBitVectorEvaluationByEncoding(
                      pOptions,
                      pThread,
                      bitVectorAssignments,
                      pBitVectorVariables,
                      pFullBitVectorEvaluation),
                  newTarget);
          if (evaluation.isPresent()) {
            newInjected.add(evaluation.orElseThrow());
          }
        }
      }
      return pCurrentStatement.cloneWithInjectedStatements(newInjected.build());
    }
    // no valid target pc (e.g. exit pc) -> return statement as is
    return pCurrentStatement;
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<CVariableDeclaration> pAccessedVariables) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.bitVectorEncoding.equals(BitVectorEncoding.SCALAR)) {
      for (var entry : pBitVectorVariables.scalarAccessBitVectors.orElseThrow().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> accessVariables = entry.getValue().variables;
        boolean value = pAccessedVariables.contains(entry.getKey());
        ScalarBitVectorExpression scalarBitVectorExpression = new ScalarBitVectorExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                accessVariables.get(pThread), scalarBitVectorExpression));
      }
    } else {
      CExpression bitVectorVariable =
          pBitVectorVariables.getDenseBitVectorByAccessType(BitVectorAccessType.ACCESS, pThread);
      BitVectorExpression bitVectorExpression =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.globalVariableIds, pAccessedVariables);
      rStatements.add(new SeqBitVectorAssignmentStatement(bitVectorVariable, bitVectorExpression));
    }
    return rStatements.build();
  }

  private static Optional<SeqBitVectorAccessEvaluationStatement> buildBitVectorEvaluationStatements(
      SeqThreadStatement pCurrentStatement,
      ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
      BitVectorEvaluationExpression pBitVectorEvaluation,
      SeqThreadStatementClause pTarget) {

    // no bit vector evaluation if prior to critical sections, so that loop head is evaluated
    if (!SeqThreadStatementClauseUtil.priorCriticalSection(pCurrentStatement)) {
      boolean allZero = pBitVectorAssignments.stream().allMatch(a -> a.value.isZero());
      // TODO a direct goto makes the following statements unreachable (r < K, break, etc)
      Optional<SeqLogicalNotExpression> expression =
          allZero
              ? Optional.empty()
              : Optional.of(new SeqLogicalNotExpression(pBitVectorEvaluation));
      SeqBitVectorAccessEvaluationStatement rEvaluation =
          new SeqBitVectorAccessEvaluationStatement(expression, pTarget.block.getGotoLabel());
      return Optional.of(rEvaluation);
    }
    return Optional.empty();
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
        newClauses.add(clause.cloneWithBlockAndMergedBlock(newBlock, newMergedBlocks.build()));
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
        ImmutableList<CVariableDeclaration> pAccessVariables =
            SeqThreadStatementClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
                firstClause, BitVectorAccessType.ACCESS);
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorAssignments(
                pOptions, threadCreation.createdThread, pBitVectorVariables, pAccessVariables);
        newStatements.add(threadCreation.cloneWithBitVectorAssignments(bitVectorAssignments));
      } else {
        newStatements.add(statement);
      }
    }
    return pBlock.cloneWithStatements(newStatements.build());
  }
}
