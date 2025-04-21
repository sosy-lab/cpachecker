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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.SeqBitVector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorGlobalVariable;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class BitVectorInjector {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> inject(
      MPOROptions pOptions,
      BitVectorVariables pBitVectors,
      ImmutableList<BitVectorGlobalVariable> pBitVectorGlobalVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rInjected =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      MPORThread thread = entry.getKey();
      CIdExpression bitVector = pBitVectors.get(thread);
      CBinaryExpression bitVectorEvaluation =
          SeqExpressionBuilder.buildBitVectorEvaluation(
              bitVector,
              pBitVectors.bitVectors.values().stream()
                  .filter(b -> !b.equals(bitVector))
                  .collect(ImmutableSet.toImmutableSet()),
              pBinaryExpressionBuilder);
      SeqThreadLoopLabelStatement switchLabel =
          new SeqThreadLoopLabelStatement(
              SeqNameUtil.buildThreadSwitchLabelName(pOptions, thread.id));
      rInjected.put(
          entry.getKey(),
          injectBitVectors(
              pOptions,
              entry.getKey(),
              pBitVectorGlobalVariables,
              pBitVectors,
              entry.getValue(),
              bitVectorEvaluation,
              switchLabel));
    }
    return rInjected.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> injectBitVectors(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<BitVectorGlobalVariable> pBitVectorGlobalVariables,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqCaseClause> pCaseClauses,
      CBinaryExpression pBitVectorEvaluation,
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
                pBitVectorGlobalVariables,
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
      final CBinaryExpression pBitVectorEvaluation,
      SeqThreadLoopLabelStatement pSwitchLabel,
      SeqCaseBlockStatement pCurrentStatement,
      final ImmutableList<BitVectorGlobalVariable> pBitVectorGlobalVariables,
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
                  pBitVectorEvaluation,
                  pSwitchLabel,
                  concatStatement,
                  pBitVectorGlobalVariables,
                  pBitVectorVariables,
                  pLabelValueMap));
        }
        return pCurrentStatement.cloneWithConcatenatedStatements(newStatements.build());
      }
    }
    // step 2: if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int intTargetPc = pCurrentStatement.getTargetPc().orElseThrow();
      if (intTargetPc == Sequentialization.EXIT_PC) {
        // for the exit pc, reset the bit vector to just 0s
        CIdExpression bitVector = pBitVectorVariables.get(pThread);
        newInjected.addAll(pCurrentStatement.getInjectedStatements());
        int binaryLength = BitVectorUtil.getBinaryLength(pBitVectorGlobalVariables.size());
        newInjected.add(
            new SeqBitVectorAssignment(
                bitVector, BitVectorUtil.createZeroBitVector(pOptions, binaryLength)));
      } else {
        // for all other target pc, set the bit vector based on global accesses in the target case
        SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(intTargetPc));
        ImmutableList<CVariableDeclaration> globalVariables =
            SeqCaseClauseUtil.findAllGlobalVariablesInCaseClause(newTarget);
        CIdExpression bitVectorVariable = pBitVectorVariables.get(pThread);
        newInjected.addAll(pCurrentStatement.getInjectedStatements());
        SeqBitVector bitVector =
            BitVectorUtil.createBitVector(pOptions, pBitVectorGlobalVariables, globalVariables);
        newInjected.add(new SeqBitVectorAssignment(bitVectorVariable, bitVector));
        // no bit vector evaluation if prior to critical sections, so that loop head is evaluated
        if (!SeqCaseClauseUtil.priorCriticalSection(pCurrentStatement)) {
          // TODO a direct goto makes the following statements unreachable (r < K, break, etc)
          newInjected.add(
              new SeqBitVectorGotoStatement(
                  bitVector.isZero()
                      ? Optional.empty()
                      : Optional.of(new SeqLogicalNotExpression(pBitVectorEvaluation)),
                  pSwitchLabel));
        }
      }
      return pCurrentStatement.cloneWithInjectedStatements(newInjected.build());
    }
    // no concat statements and no valid target pc (e.g. exit pc) -> return statement as is
    return pCurrentStatement;
  }
}
