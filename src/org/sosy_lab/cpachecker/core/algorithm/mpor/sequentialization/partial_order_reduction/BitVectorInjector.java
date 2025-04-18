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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorInjector {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> inject(
      BitVectorVariables pBitVectors,
      ImmutableSet<CVariableDeclaration> pAllGlobalVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableMap<CVariableDeclaration, Integer> globalVariableIds =
        assignGlobalVariableIds(pAllGlobalVariables);

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rWithBitVectors =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rWithBitVectors.put(
          entry.getKey(),
          injectBitVectors(entry.getKey(), globalVariableIds, pBitVectors, entry.getValue()));
    }
    return rWithBitVectors.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> injectBitVectors(
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      BitVectorVariables pBitVectorVariables,
      ImmutableList<SeqCaseClause> pCaseClauses) {

    ImmutableList.Builder<SeqCaseClause> rInjected = ImmutableList.builder();
    ImmutableMap<Integer, SeqCaseClause> labelValueMap =
        SeqCaseClauseUtil.mapCaseLabelValueToCaseClause(pCaseClauses);
    for (SeqCaseClause caseClause : pCaseClauses) {
      ImmutableList.Builder<SeqCaseBlockStatement> newStatements = ImmutableList.builder();
      for (SeqCaseBlockStatement statement : caseClause.block.statements) {
        newStatements.add(
            recursivelyInjectBitVectors(
                pThread, statement, pGlobalVariableIds, pBitVectorVariables, labelValueMap));
      }
      rInjected.add(caseClause.cloneWithBlock(new SeqCaseBlock(newStatements.build())));
    }
    return rInjected.build();
  }

  private static SeqCaseBlockStatement recursivelyInjectBitVectors(
      final MPORThread pThread,
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
      ImmutableList<SeqInjectedStatement> injected = pCurrentStatement.getInjectedStatements();
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      newInjected.addAll(injected);
      newInjected.add(
          new SeqBitVectorAssignment(
              pBitVectorVariables.get(pThread),
              BitVectorUtil.createBitVector(pGlobalVariableIds, globalVariables.build())));
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
