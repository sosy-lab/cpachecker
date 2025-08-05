// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
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
      SeqThreadStatementBlock pBlock) {

    return !findDirectGlobalVariablesByAccessType(
            pLabelBlockMap, pBlock, BitVectorAccessType.ACCESS)
        .isEmpty();
  }

  /**
   * Maps pointers {@code ptr} to the memory locations e.g. {@code &var} assigned to them based on
   * {@code pSubstituteEdges}, including both global and local memory locations.
   */
  public static ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration>
      mapPointerAssignments(ImmutableCollection<SubstituteEdge> pSubstituteEdges) {

    // step 1: map pointers to memory locations assigned to them, including other pointers
    ImmutableSetMultimap.Builder<CVariableDeclaration, CVariableDeclaration> initialBuilder =
        ImmutableSetMultimap.builder();
    for (SubstituteEdge substituteEdge : pSubstituteEdges) {
      if (!substituteEdge.pointerAssignment.isEmpty()) {
        assert substituteEdge.pointerAssignment.size() <= 1
            : "a single edge can have at most 1 pointer assignments";
        Map.Entry<CVariableDeclaration, CVariableDeclaration> singleEntry =
            substituteEdge.pointerAssignment.entrySet().iterator().next();
        initialBuilder.put(singleEntry.getKey(), singleEntry.getValue());
      }
    }
    ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> initialMap =
        initialBuilder.build();
    // step 2: update the map so that only non-pointer variables are in the values
    ImmutableSetMultimap.Builder<CVariableDeclaration, CVariableDeclaration> rFinal =
        ImmutableSetMultimap.builder();
    for (var entry : initialMap.entries()) {
      rFinal.putAll(
          entry.getKey(), findAllAssignedVariables(entry.getKey(), initialMap, new HashSet<>()));
    }
    return rFinal.build();
  }

  private static ImmutableSet<CVariableDeclaration> findAllAssignedVariables(
      CVariableDeclaration pPointer,
      ImmutableSetMultimap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      Set<CVariableDeclaration> pVisitedPointers) {

    ImmutableSet.Builder<CVariableDeclaration> rLocations = ImmutableSet.builder();
    if (pPointerAssignments.containsKey(pPointer)) {
      for (CVariableDeclaration variableDeclaration : pPointerAssignments.get(pPointer)) {
        if (variableDeclaration.getType() instanceof CPointerType) {
          if (pVisitedPointers.add(pPointer)) {
            // for pointers, recursively find all variables assigned to the pointer
            rLocations.addAll(
                findAllAssignedVariables(
                    variableDeclaration, pPointerAssignments, pVisitedPointers));
          }
        } else {
          // for non-pointer variables, just add the variable itself
          rLocations.add(variableDeclaration);
        }
      }
    }
    return rLocations.build();
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<CVariableDeclaration> findDirectGlobalVariablesByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      recursivelyFindTargetGotoStatements(found, statement, pLabelBlockMap);
      ImmutableSet<CVariableDeclaration> foundGlobalVariables =
          extractGlobalVariablesFromStatements(ImmutableSet.copyOf(found), pAccessType);
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
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      recursivelyFindTargetStatements(found, statement, pLabelClauseMap, pLabelBlockMap);
      ImmutableSet<CVariableDeclaration> foundGlobalVariables =
          extractGlobalVariablesFromStatements(ImmutableSet.copyOf(found), pAccessType);
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
      return targetClause.block.getStatements();
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
