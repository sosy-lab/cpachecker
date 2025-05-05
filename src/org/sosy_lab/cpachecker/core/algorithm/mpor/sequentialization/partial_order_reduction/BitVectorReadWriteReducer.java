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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorReadWriteEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorReadWriteReducer {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> reduce(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.globalVariableIds.isEmpty()) {
      pLogger.log(
          Level.INFO,
          "bit vectors are enabled, but the program does not contain any global variables.");
      return pCaseClauses; // no global variables -> no bit vectors
    }
    ImmutableMap.Builder<MPORThread, ImmutableList<SeqThreadStatementClause>> rInjected =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      Optional<BitVectorEvaluationExpression> bitVectorEvaluation =
          BitVectorEvaluationBuilder.buildBitVectorReadWriteEvaluationByEncoding(
              pOptions, thread, pBitVectorVariables, pBinaryExpressionBuilder);
      rInjected.put(
          entry.getKey(),
          injectBitVectors(
              pOptions,
              entry.getKey(),
              pBitVectorVariables,
              entry.getValue(),
              bitVectorEvaluation));
    }
    return rInjected.buildOrThrow();
  }

  private static ImmutableList<SeqThreadStatementClause> injectBitVectors(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqThreadStatementClause> pCaseClauses,
      Optional<BitVectorEvaluationExpression> pBitVectorEvaluation) {

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
                pBitVectorEvaluation,
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
        // exit pc -> reset bit vector to 0s so that no interference with still active threads
        newInjected.addAll(
            buildBitVectorReadWriteAssignments(
                pOptions, pThread, pBitVectorVariables, ImmutableList.of(), ImmutableList.of()));
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target case
        SeqThreadStatementClause newTarget =
            Objects.requireNonNull(pLabelValueMap.get(intTargetPc));
        ImmutableList<CVariableDeclaration> readVariables =
            SeqThreadStatementClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
                newTarget, BitVectorAccessType.READ);
        ImmutableList<CVariableDeclaration> writeVariables =
            SeqThreadStatementClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
                newTarget, BitVectorAccessType.WRITE);
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorReadWriteAssignments(
                pOptions, pThread, pBitVectorVariables, readVariables, writeVariables);
        newInjected.addAll(bitVectorAssignments);
        Optional<SeqBitVectorReadWriteEvaluationStatement> evaluation =
            buildBitVectorReadWriteEvaluationStatements(
                pCurrentStatement,
                bitVectorAssignments,
                BitVectorEvaluationBuilder.buildPrunedReadWriteBitVectorEvaluationByEncoding(
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

  private static ImmutableList<SeqBitVectorAssignmentStatement> buildBitVectorReadWriteAssignments(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<CVariableDeclaration> pReadVariables,
      ImmutableList<CVariableDeclaration> pWrittenVariables) {

    ImmutableList.Builder<SeqBitVectorAssignmentStatement> rStatements = ImmutableList.builder();
    if (pOptions.porBitVectorEncoding.equals(BitVectorEncoding.SCALAR)) {
      for (var entry : pBitVectorVariables.scalarReadBitVectors.orElseThrow().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> readVariables = entry.getValue().variables;
        boolean value = pReadVariables.contains(entry.getKey());
        ScalarBitVectorExpression scalarBitVectorExpression = new ScalarBitVectorExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                readVariables.get(pThread), scalarBitVectorExpression));
      }
      for (var entry : pBitVectorVariables.scalarWriteBitVectors.orElseThrow().entrySet()) {
        ImmutableMap<MPORThread, CIdExpression> writeVariables = entry.getValue().variables;
        boolean value = pWrittenVariables.contains(entry.getKey());
        ScalarBitVectorExpression scalarBitVectorExpression = new ScalarBitVectorExpression(value);
        rStatements.add(
            new SeqBitVectorAssignmentStatement(
                writeVariables.get(pThread), scalarBitVectorExpression));
      }
    } else {
      CExpression readBitVectorVariable =
          pBitVectorVariables.getDenseBitVectorByAccessType(BitVectorAccessType.READ, pThread);
      BitVectorExpression readBitVectorExpression =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.globalVariableIds, pReadVariables);
      rStatements.add(
          new SeqBitVectorAssignmentStatement(readBitVectorVariable, readBitVectorExpression));
      CExpression writeBitVectorVariable =
          pBitVectorVariables.getDenseBitVectorByAccessType(BitVectorAccessType.WRITE, pThread);
      BitVectorExpression writeBitVectorExpression =
          BitVectorUtil.buildBitVectorExpression(
              pOptions, pBitVectorVariables.globalVariableIds, pWrittenVariables);
      rStatements.add(
          new SeqBitVectorAssignmentStatement(writeBitVectorVariable, writeBitVectorExpression));
    }
    return rStatements.build();
  }

  private static Optional<SeqBitVectorReadWriteEvaluationStatement>
      buildBitVectorReadWriteEvaluationStatements(
          SeqThreadStatement pCurrentStatement,
          ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
          BitVectorEvaluationExpression pBitVectorEvaluation,
          SeqThreadStatementClause pTarget) {

    // no bit vector evaluation if prior to critical sections, so that loop head is evaluated
    if (!SeqThreadStatementClauseUtil.priorCriticalSection(pCurrentStatement)) {
      boolean allZero = pBitVectorAssignments.stream().allMatch(a -> a.value.isZero());
      // TODO a direct goto makes the following statements unreachable (r < K, break, etc)
      Optional<BitVectorEvaluationExpression> expression =
          allZero ? Optional.empty() : Optional.of(pBitVectorEvaluation);
      SeqBitVectorReadWriteEvaluationStatement rEvaluation =
          new SeqBitVectorReadWriteEvaluationStatement(expression, pTarget.gotoLabel.orElseThrow());
      return Optional.of(rEvaluation);
    }
    return Optional.empty();
  }
}
