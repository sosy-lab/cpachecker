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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorInjector {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> inject(
      MPOROptions pOptions,
      BitVectorVariables pBitVectors,
      ImmutableSet<CVariableDeclaration> pAllGlobalVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableMap<CVariableDeclaration, Integer> globalVariableIds =
        assignGlobalVariableIds(pAllGlobalVariables);

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
      SeqThreadLoopLabelStatement assumeLabel =
          new SeqThreadLoopLabelStatement(
              SeqNameUtil.buildThreadAssumeLabelName(pOptions, thread.id));
      SeqThreadLoopLabelStatement switchLabel =
          new SeqThreadLoopLabelStatement(
              SeqNameUtil.buildThreadSwitchLabelName(pOptions, thread.id));
      rInjected.put(
          entry.getKey(),
          injectBitVectors(
              entry.getKey(),
              globalVariableIds,
              pBitVectors,
              entry.getValue(),
              bitVectorEvaluation,
              assumeLabel,
              switchLabel));
    }
    return rInjected.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> injectBitVectors(
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqCaseClause> pCaseClauses,
      CBinaryExpression pBitVectorEvaluation,
      SeqThreadLoopLabelStatement pAssumeLabel,
      SeqThreadLoopLabelStatement pSwitchLabel) {

    ImmutableList.Builder<SeqCaseClause> rInjected = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        newStatements.add(
            recursivelyInjectBitVectors(
                pThread,
                pBitVectorEvaluation,
                pAssumeLabel,
                pSwitchLabel,
                statement,
                pGlobalVariableIds,
                pBitVectorVariables,
                labelValueMap));
      }
      rInjected.add(caseClause.cloneWithBlock(new SeqCaseBlock(newStatements.build())));
    }
    return rInjected.build();
  }

  private static SeqCaseBlockStatement recursivelyInjectBitVectors(
      final MPORThread pThread,
      final CBinaryExpression pBitVectorEvaluation,
      SeqThreadLoopLabelStatement pAssumeLabel,
      SeqThreadLoopLabelStatement pSwitchLabel,
      SeqCaseBlockStatement pCurrentStatement,
      final ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      final BitVectorVariables pBitVectorVariables,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (SeqCaseClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      ImmutableSet.Builder<CVariableDeclaration> globalVariables = ImmutableSet.builder();
      for (SeqCaseBlockStatement statement : newTarget.block.statements) {
        SeqCaseClauseUtil.recursivelyGetGlobalVariables(globalVariables, statement);
      }

      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      CIdExpression bitVector = pBitVectorVariables.get(pThread);
      newInjected.addAll(pCurrentStatement.getInjectedStatements());
      newInjected.add(
          new SeqBitVectorAssignment(
              bitVector,
              BitVectorUtil.createBitVector(pGlobalVariableIds, globalVariables.build())));
      newInjected.add(
          new SeqBitVectorGotoStatement(
              new SeqLogicalNotExpression(pBitVectorEvaluation), pAssumeLabel));
      return pCurrentStatement.cloneWithInjectedStatements(newInjected.build());
    }
    return pCurrentStatement;
  }

  private static ImmutableMap<CVariableDeclaration, Integer> assignGlobalVariableIds(
      ImmutableSet<CVariableDeclaration> pGlobalVariables) {
    ImmutableMap.Builder<CVariableDeclaration, Integer> rIds = ImmutableMap.builder();
    int id = 0;
    for (CVariableDeclaration variable : pGlobalVariables) {
      assert variable.isGlobal();
      rIds.put(variable, id++);
    }
    return rIds.buildOrThrow();
  }
}
