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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

public class GlobalVariableFinder {

  /**
   * Returns {@code true} if any global variable is accessed when executing {@code pBlock} and its
   * directly linked blocks.
   */
  static boolean hasGlobalAccess(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      SeqThreadStatementBlock pBlock) {

    return !findDirectGlobalVariablesByAccessType(
            pLabelBlockMap,
            pPointerAssignments,
            pPointerParameterAssignments,
            pBlock,
            BitVectorAccessType.ACCESS)
        .isEmpty();
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<CVariableDeclaration> findDirectGlobalVariablesByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      recursivelyFindTargetGotoStatements(found, statement, pLabelBlockMap);
      ImmutableSet<CVariableDeclaration> foundGlobalVariables =
          extractGlobalVariablesFromStatements(
              ImmutableSet.copyOf(found),
              pPointerAssignments,
              pPointerParameterAssignments,
              pAccessType);
      rGlobalVariables.addAll(foundGlobalVariables);
    }
    return rGlobalVariables.build();
  }

  // TODO also use ReachType here and remove redundant pLabelClauseMap
  /**
   * Returns all global variables accessed when executing {@code pBlock}, its directly linked blocks
   * and all possible successor blocks, that may or may not actually be executed.
   */
  static ImmutableSet<CVariableDeclaration> findReachableGlobalVariablesByAccessType(
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      recursivelyFindTargetStatements(found, statement, pLabelClauseMap, pLabelBlockMap);
      ImmutableSet<CVariableDeclaration> foundGlobalVariables =
          extractGlobalVariablesFromStatements(
              ImmutableSet.copyOf(found),
              pPointerAssignments,
              pPointerParameterAssignments,
              pAccessType);
      rGlobalVariables.addAll(foundGlobalVariables);
    }
    return rGlobalVariables.build();
  }

  // Private Methods ===============================================================================

  /**
   * Searches {@code pStatement}, all directly linked statements via {@code goto} and all target
   * {@code pc} statements and stores them in {@code pFound}.
   */
  private static void recursivelyFindTargetStatements(
      Set<SeqThreadStatement> pFound,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<SeqThreadStatement> targetStatements =
        ImmutableList.<SeqThreadStatement>builder()
            .addAll(getTargetPcStatements(pStatement, pLabelClauseMap))
            .addAll(getTargetGotoStatements(pStatement, pLabelBlockMap))
            .build();
    for (SeqThreadStatement targetStatement : targetStatements) {
      // prevent infinite loops when statements contain loops
      if (pFound.add(targetStatement)) {
        recursivelyFindTargetStatements(pFound, targetStatement, pLabelClauseMap, pLabelBlockMap);
      }
    }
  }

  /**
   * Searches {@code pStatement} and all directly linked via {@code goto} statements and stores them
   * in {@code pFound}.
   */
  private static void recursivelyFindTargetGotoStatements(
      Set<SeqThreadStatement> pFound,
      SeqThreadStatement pStatement,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    // recursively search the target goto statements
    ImmutableList<SeqThreadStatement> targetGotoStatements =
        getTargetGotoStatements(pStatement, pLabelBlockMap);
    for (SeqThreadStatement targetStatement : targetGotoStatements) {
      // prevent infinite loops when statements contain loops
      if (pFound.add(targetStatement)) {
        recursivelyFindTargetGotoStatements(pFound, targetStatement, pLabelBlockMap);
      }
    }
  }

  private static ImmutableList<SeqThreadStatement> getTargetPcStatements(
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap) {

    if (SeqThreadStatementClauseUtil.isValidTargetPc(pStatement.getTargetPc())) {
      int targetNumber = pStatement.getTargetPc().orElseThrow();
      SeqThreadStatementClause targetClause =
          Objects.requireNonNull(pLabelClauseMap.get(targetNumber));
      return targetClause.getFirstBlock().getStatements();
    }
    return ImmutableList.of();
  }

  /**
   * Searches all statements targeted by {@code pStatement} via {@code goto}. This excludes target
   * {@code pc} because they represent a cut i.e. context switch in the sequentialization.
   */
  private static ImmutableList<SeqThreadStatement> getTargetGotoStatements(
      SeqThreadStatement pStatement,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap) {

    if (pStatement.getTargetGoto().isPresent()) {
      int targetNumber = pStatement.getTargetGoto().orElseThrow().labelNumber;
      SeqThreadStatementBlock targetBlock =
          Objects.requireNonNull(pLabelBlockMap.get(targetNumber));
      return targetBlock.getStatements();
    }
    return ImmutableList.of();
  }

  // TODO actually use pointer parameter assignments
  @SuppressWarnings("unused")
  private static ImmutableSet<CVariableDeclaration> extractGlobalVariablesFromStatements(
      ImmutableSet<SeqThreadStatement> pStatements,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableMap<CParameterDeclaration, CSimpleDeclaration> pPointerParameterAssignments,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        // first check direct accesses on the variables themselves
        for (CVariableDeclaration variable :
            substituteEdge.getGlobalVariablesByAccessType(pAccessType)) {
          assert variable.isGlobal() : "CVariableDeclaration in SubstituteEdge must be global";
          rGlobalVariables.add(variable);
        }
        // then check indirect accesses via pointers that point to the variables
        for (CSimpleDeclaration pointerDereference :
            substituteEdge.getPointerDereferencesByAccessType(pAccessType)) {
          if (pointerDereference instanceof CVariableDeclaration lhsVariableDeclaration) {
            if (pPointerAssignments.containsKey(lhsVariableDeclaration)) {
              for (CSimpleDeclaration assignedVariable :
                  pPointerAssignments.get(lhsVariableDeclaration)) {
                // the variables assigned to a pointer may not be global, we only filter the globals
                if (assignedVariable instanceof CVariableDeclaration rhsVariableDeclaration) {
                  if (rhsVariableDeclaration.isGlobal()) {
                    rGlobalVariables.add(rhsVariableDeclaration);
                  }
                }
              }
            }
          }
        }
      }
    }
    return rGlobalVariables.build();
  }
}
