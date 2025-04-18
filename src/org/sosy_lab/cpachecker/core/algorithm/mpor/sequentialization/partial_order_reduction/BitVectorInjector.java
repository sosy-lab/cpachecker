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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqBitVectorAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorInjector {

  protected static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> inject(
      MPOROptions pOptions, ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableSet<CVariableDeclaration> allGlobalVariables = getAllGlobalVariables(pCaseClauses);
    ImmutableMap<CVariableDeclaration, Integer> globalVariableIds =
        assignGlobalVariableIds(allGlobalVariables);
    ImmutableMap<MPORThread, CIdExpression> bitVectorVariables =
        buildBitVectorVariables(pOptions, pCaseClauses.keySet());

    ImmutableMap.Builder<MPORThread, ImmutableList<SeqCaseClause>> rWithBitVectors =
        ImmutableMap.builder();
    for (var entry : pCaseClauses.entrySet()) {
      rWithBitVectors.put(
          entry.getKey(),
          injectBitVectors(
              entry.getKey(), globalVariableIds, bitVectorVariables, entry.getValue()));
    }
    return rWithBitVectors.buildOrThrow();
  }

  private static ImmutableList<SeqCaseClause> injectBitVectors(
      MPORThread pThread,
      ImmutableMap<CVariableDeclaration, Integer> pGlobalVariableIds,
      ImmutableMap<MPORThread, CIdExpression> pBitVectorVariables,
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
      final ImmutableMap<MPORThread, CIdExpression> pBitVectorVariables,
      final ImmutableMap<Integer, SeqCaseClause> pLabelValueMap) {

    if (SeqCaseClauseUtil.isValidTargetPc(pCurrentStatement.getTargetPc())) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      SeqCaseClause newTarget = Objects.requireNonNull(pLabelValueMap.get(targetPc));
      ImmutableSet.Builder<CVariableDeclaration> globalVariables = ImmutableSet.builder();
      for (SeqCaseBlockStatement statement : newTarget.block.statements) {
        recursivelyGetGlobalVariables(globalVariables, statement);
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

  // TODO maybe use lists, so that the same input always results in the same output (ordering)
  private static ImmutableSet<CVariableDeclaration> getAllGlobalVariables(
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses) {

    ImmutableSet.Builder<CVariableDeclaration> rVariables = ImmutableSet.builder();
    for (var entry : pCaseClauses.entrySet()) {
      for (SeqCaseClause caseClause : entry.getValue()) {
        for (SeqCaseBlockStatement statement : caseClause.block.statements) {
          ImmutableSet.Builder<CVariableDeclaration> found = ImmutableSet.builder();
          recursivelyGetGlobalVariables(found, statement);
          rVariables.addAll(found.build());
        }
      }
    }
    return rVariables.build();
  }

  /** Searches {@code pStatement} and all concatenated statements for their global variables. */
  private static void recursivelyGetGlobalVariables(
      ImmutableSet.Builder<CVariableDeclaration> pFound, SeqCaseBlockStatement pStatement) {

    for (SubstituteEdge substituteEdge : pStatement.getSubstituteEdges()) {
      for (CVariableDeclaration variable : substituteEdge.globalVariables) {
        assert variable.isGlobal();
        pFound.add(variable);
      }
    }
    if (pStatement.isConcatenable()) {
      for (SeqCaseBlockStatement concatStatement : pStatement.getConcatenatedStatements()) {
        recursivelyGetGlobalVariables(pFound, concatStatement);
      }
    }
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

  private static ImmutableMap<MPORThread, CIdExpression> buildBitVectorVariables(
      MPOROptions pOptions, ImmutableSet<MPORThread> pThreads) {
    ImmutableMap.Builder<MPORThread, CIdExpression> rBitVectors = ImmutableMap.builder();
    for (MPORThread thread : pThreads) {
      String name = SeqNameUtil.buildBitVectorName(pOptions, thread.id);
      // TODO we don't actually use the declaration, only need it for the CIdExpression
      CVariableDeclaration declaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              true, SeqSimpleType.INT, name, SeqInitializer.INT_0);
      rBitVectors.put(thread, SeqExpressionBuilder.buildIdExpression(declaration));
    }
    return rBitVectors.buildOrThrow();
  }
}
