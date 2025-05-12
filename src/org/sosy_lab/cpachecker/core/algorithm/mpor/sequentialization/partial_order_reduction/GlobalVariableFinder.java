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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

public class GlobalVariableFinder {

  public static ImmutableSet<CVariableDeclaration> findGlobalVariablesByReductionType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      BitVectorReduction pReductionType) {

    return switch (pReductionType) {
      case NONE -> ImmutableSet.of();
      case ACCESS_ONLY ->
          findGlobalVariablesByAccessType(pLabelBlockMap, pBlock, BitVectorAccessType.ACCESS);
      case READ_AND_WRITE ->
          ImmutableSet.<CVariableDeclaration>builder()
              .addAll(
                  findGlobalVariablesByAccessType(pLabelBlockMap, pBlock, BitVectorAccessType.READ))
              .addAll(
                  findGlobalVariablesByAccessType(
                      pLabelBlockMap, pBlock, BitVectorAccessType.WRITE))
              .build();
    };
  }

  public static ImmutableSet<CVariableDeclaration> findGlobalVariablesByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = Set.of(statement);
      rGlobalVariables.addAll(
          recursivelyFindGlobalVariablesByAccessType(
              found, pLabelBlockMap, statement, pAccessType));
    }
    return rGlobalVariables.build();
  }

  /**
   * Searches {@code pStatement} and all linked statements for their global variables based on
   * {@code pAccessType}.
   */
  private static ImmutableSet<CVariableDeclaration> recursivelyFindGlobalVariablesByAccessType(
      Set<SeqThreadStatement> pFound,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatement pStatement,
      final BitVectorAccessType pAccessType) {

    // recursively search the target pc and goto statements
    for (SeqThreadStatement targetStatement : getStatementsByTarget(pStatement, pLabelBlockMap)) {
      // prevent infinite loops when statements contain loops
      if (pFound.add(targetStatement)) {
        return recursivelyFindGlobalVariablesByAccessType(
            pFound, pLabelBlockMap, targetStatement, pAccessType);
      }
    }
    return extractGlobalVariablesFromStatements(ImmutableSet.copyOf(pFound), pAccessType);
  }

  private static ImmutableList<SeqThreadStatement> getStatementsByTarget(
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetNumber = pStatement.getTargetPc().orElseThrow();
      return Objects.requireNonNull(pLabelBlockMap.get(targetNumber)).getStatements();
    }
    if (pStatement.getTargetGoto().isPresent()) {
      int targetNumber = pStatement.getTargetGoto().orElseThrow().labelNumber;
      return Objects.requireNonNull(pLabelBlockMap.get(targetNumber)).getStatements();
    }
    return ImmutableList.of();
  }

  private static ImmutableSet<CVariableDeclaration> extractGlobalVariablesFromStatements(
      ImmutableSet<SeqThreadStatement> pStatements, BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        for (CVariableDeclaration variableDeclaration :
            substituteEdge.getGlobalVariablesByAccessType(pAccessType)) {
          assert variableDeclaration.isGlobal()
              : "CVariableDeclaration in SubstituteEdge must be global";
          rGlobalVariables.add(variableDeclaration);
        }
      }
    }
    return rGlobalVariables.build();
  }
}
