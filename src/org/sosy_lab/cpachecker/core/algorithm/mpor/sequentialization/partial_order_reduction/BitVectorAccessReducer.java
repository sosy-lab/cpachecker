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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAccessEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorAccessReducer {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> reduce(
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
    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rInjected =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      Optional<BitVectorEvaluationExpression> fullBitVectorEvaluation =
          BitVectorEvaluationBuilder.buildBitVectorAccessEvaluationByEncoding(
              pOptions, thread, pBitVectorVariables, pBinaryExpressionBuilder);
      rInjected.put(
          entry.getKey(),
          injectBitVectors(
              pOptions,
              entry.getKey(),
              pBitVectorVariables,
              entry.getValue(),
              fullBitVectorEvaluation));
    }
    return rInjected.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> injectBitVectors(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      Optional<BitVectorEvaluationExpression> pFullBitVectorEvaluation) {

    ImmutableList.Builder<SeqThreadStatementClause> rInjected = ImmutableList.builder();
    ImmutableMap<Integer, SeqThreadStatementClause> labelValueMap =
        SeqThreadStatementClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    for (SeqThreadStatementClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
      for (SeqThreadStatement statement : caseClause.block.statements) {
        newStatements.add(
            recursivelyInjectBitVectors(
                pOptions,
                pThread,
                pFullBitVectorEvaluation,
                statement,
                pBitVectorVariables,
                labelValueMap));
      }
      rInjected.add(caseClause.cloneWithBlock(new SeqThreadStatementBlock(newStatements.build())));
    }
    return rInjected.build();
  }

  private static SeqThreadStatement recursivelyInjectBitVectors(
      MPOROptions pOptions,
      final MPORThread pThread,
      final Optional<BitVectorEvaluationExpression> pFullBitVectorEvaluation,
      SeqThreadStatement pCurrentStatement,
      final BitVectorVariables pBitVectorVariables,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelValueMap) {

    // step 1: recursively inject bit vector into concatenated statements
    if (pCurrentStatement.isConcatenable()) {
      if (!pCurrentStatement.getConcatenatedStatements().isEmpty()) {
        ImmutableList.Builder<SeqThreadStatement> newStatements = ImmutableList.builder();
        for (SeqThreadStatement concatStatement : pCurrentStatement.getConcatenatedStatements()) {
          newStatements.add(
              recursivelyInjectBitVectors(
                  pOptions,
                  pThread,
                  pFullBitVectorEvaluation,
                  concatStatement,
                  pBitVectorVariables,
                  pLabelValueMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }
    // step 2: if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      // inject previous injected statements, e.g. entering atomic section
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
      return pCurrentStatement.cloneWithInjectedStatements(newInjected.build());
    }
    // no concat statements and no valid target pc (e.g. exit pc) -> return statement as is
    return pCurrentStatement;
  }

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<CVariableDeclaration> pAccessedVariables) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.porBitVectorEncoding.equals(BitVectorEncoding.SCALAR)) {
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
          new SeqBitVectorAccessEvaluationStatement(expression, pTarget.gotoLabel);
      return Optional.of(rEvaluation);
    }
    return Optional.empty();
  }
}
