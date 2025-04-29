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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.ScalarBitVectorExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorReadWriteEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorReadWriteReducer {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> reduce(
      MPOROptions pOptions,
      BitVectorVariables pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pBitVectorVariables.globalVariableIds.isEmpty()) {
      pLogger.log(
          Level.INFO,
          "bit vectors are enabled, but the program does not contain any global variables.");
      return pCaseClauses; // no global variables -> no bit vectors
    }
    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rInjected =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      Optional<BitVectorEvaluationExpression> bitVectorEvaluation =
          SeqExpressionBuilder.buildBitVectorReadWriteEvaluationByEncoding(
              pOptions, thread, pBitVectorVariables, pBinaryExpressionBuilder);
      SeqThreadLoopLabelStatement switchLabel =
          new SeqThreadLoopLabelStatement(
              SeqNameUtil.buildThreadSwitchLabelName(pOptions, thread.id));
      rInjected.put(
          entry.getKey(),
          injectBitVectors(
              pOptions,
              entry.getKey(),
              pBitVectorVariables,
              entry.getValue(),
              bitVectorEvaluation,
              switchLabel));
    }
    return rInjected.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> injectBitVectors(
      MPOROptions pOptions,
      MPORThread pThread,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqCaseClause> pCaseClauses,
      Optional<BitVectorEvaluationExpression> pBitVectorEvaluation,
      SeqThreadLoopLabelStatement pSwitchLabel) {

    ImmutableList.Builder<SeqCaseClause> rInjected = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        newStatements.add(
            recursivelyInjectBitVectors(
                pOptions,
                pThread,
                pBitVectorEvaluation,
                pSwitchLabel,
                statement,
                pBitVectorVariables,
                labelValueMap));
      }
      rInjected.add(caseClause.cloneWithBlock(new SeqCaseBlock(newStatements.build())));
    }
    return rInjected.build();
  }

  private static SeqCaseBlockStatement recursivelyInjectBitVectors(
      MPOROptions pOptions,
      final MPORThread pThread,
      final Optional<BitVectorEvaluationExpression> pFullBitVectorEvaluation,
      SeqThreadLoopLabelStatement pSwitchLabel,
      SeqCaseBlockStatement pCurrentStatement,
      final BitVectorVariables pBitVectorVariables,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    // step 1: recursively inject bit vector into concatenated statements
    if (pCurrentStatement.isConcatenable()) {
      if (!pCurrentStatement.getConcatenatedStatements().isEmpty()) {
        ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
        for (SeqCaseBlockStatement concatStatement :
            pCurrentStatement.getConcatenatedStatements()) {
          newStatements.add(
              recursivelyInjectBitVectors(
                  pOptions,
                  pThread,
                  pFullBitVectorEvaluation,
                  pSwitchLabel,
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
        SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(intTargetPc));
        ImmutableList<CVariableDeclaration> readVariables =
            SeqCaseClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
                newTarget, BitVectorAccessType.READ);
        ImmutableList<CVariableDeclaration> writeVariables =
            SeqCaseClauseUtil.findGlobalVariablesInCaseClauseByAccessType(
                newTarget, BitVectorAccessType.WRITE);
        ImmutableList<SeqBitVectorAssignmentStatement> bitVectorAssignments =
            buildBitVectorReadWriteAssignments(
                pOptions, pThread, pBitVectorVariables, readVariables, writeVariables);
        newInjected.addAll(bitVectorAssignments);
        Optional<SeqBitVectorReadWriteEvaluationStatement> evaluation =
            buildBitVectorReadWriteEvaluationStatements(
                pCurrentStatement,
                bitVectorAssignments,
                SeqExpressionBuilder.buildPrunedReadWriteBitVectorEvaluationByEncoding(
                    pOptions,
                    pThread,
                    bitVectorAssignments,
                    pBitVectorVariables,
                    pFullBitVectorEvaluation),
                pSwitchLabel);
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
          SeqCaseBlockStatement pCurrentStatement,
          ImmutableList<SeqBitVectorAssignmentStatement> pBitVectorAssignments,
          BitVectorEvaluationExpression pBitVectorEvaluation,
          SeqThreadLoopLabelStatement pSwitchLabel) {

    // no bit vector evaluation if prior to critical sections, so that loop head is evaluated
    if (!SeqCaseClauseUtil.priorCriticalSection(pCurrentStatement)) {
      boolean allZero = pBitVectorAssignments.stream().allMatch(a -> a.value.isZero());
      // TODO a direct goto makes the following statements unreachable (r < K, break, etc)
      Optional<BitVectorEvaluationExpression> expression =
          allZero ? Optional.empty() : Optional.of(pBitVectorEvaluation);
      SeqBitVectorReadWriteEvaluationStatement rEvaluation =
          new SeqBitVectorReadWriteEvaluationStatement(expression, pSwitchLabel);
      return Optional.of(rEvaluation);
    }
    return Optional.empty();
  }
}
