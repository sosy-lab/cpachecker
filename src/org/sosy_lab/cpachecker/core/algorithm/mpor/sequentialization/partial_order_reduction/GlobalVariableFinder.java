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
import com.google.common.collect.ImmutableTable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class GlobalVariableFinder {

  /**
   * Returns {@code true} if any global variable is accessed when executing {@code pBlock} and its
   * directly linked blocks.
   */
  static boolean hasGlobalAccess(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
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
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
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
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
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

  private static ImmutableSet<CVariableDeclaration> extractGlobalVariablesFromStatements(
      ImmutableSet<SeqThreadStatement> pStatements,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<CVariableDeclaration> rGlobalVariables = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        // first check direct accesses on the variables themselves
        ImmutableSet<CVariableDeclaration> globalAccesses =
            substituteEdge.getGlobalVariablesByAccessType(pAccessType);
        for (CVariableDeclaration variable : globalAccesses) {
          assert variable.isGlobal() : "CVariableDeclaration in SubstituteEdge must be global";
          rGlobalVariables.add(variable);
        }
        // then check indirect accesses via pointers that point to the variables
        ImmutableSet<CSimpleDeclaration> pointerDereferences =
            substituteEdge.getPointerDereferencesByAccessType(pAccessType);
        for (CSimpleDeclaration pointerDereference : pointerDereferences) {
          Set<CVariableDeclaration> globalVariables = new HashSet<>();
          recursivelyFindGlobalVariablesByPointerDereference(
              pointerDereference,
              globalVariables,
              substituteEdge.threadEdge.callContext,
              pPointerAssignments,
              pPointerParameterAssignments);
          rGlobalVariables.addAll(globalVariables);
        }
      }
    }
    return rGlobalVariables.build();
  }

  // TODO prevent infinite recursion when ptr = ptr;
  /**
   * Finds the set of {@link CVariableDeclaration}s that are associated by the given pointer
   * dereference, i.e. the set of global variables whose addresses are at some point in the program
   * assigned to the pointer variable / parameter.
   */
  private static void recursivelyFindGlobalVariablesByPointerDereference(
      CSimpleDeclaration pCurrentDeclaration,
      Set<CVariableDeclaration> pGlobalVariables,
      final Optional<ThreadEdge> pCallContext,
      final ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      final ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments) {

    if (pCurrentDeclaration instanceof CVariableDeclaration variableDeclaration) {
      if (variableDeclaration.getType() instanceof CPointerType) {
        assert pPointerAssignments.containsKey(variableDeclaration);
        for (CSimpleDeclaration rightHandSide : pPointerAssignments.get(variableDeclaration)) {
          recursivelyFindGlobalVariablesByPointerDereference(
              rightHandSide,
              pGlobalVariables,
              pCallContext,
              pPointerAssignments,
              pPointerParameterAssignments);
        }
      } else {
        pGlobalVariables.add(variableDeclaration);
      }

    } else if (pCurrentDeclaration instanceof CParameterDeclaration parameterDeclaration) {
      assert pCallContext.isPresent() : "call context must be present for CParameterDeclaration";
      ThreadEdge callContext = pCallContext.orElseThrow();
      // in pthread_create that does not pass an arg to start_routine, the pair is not present
      if (pPointerParameterAssignments.contains(callContext, parameterDeclaration)) {
        CSimpleDeclaration rightHandSide =
            pPointerParameterAssignments.get(callContext, parameterDeclaration);
        recursivelyFindGlobalVariablesByPointerDereference(
            rightHandSide,
            pGlobalVariables,
            pCallContext,
            pPointerAssignments,
            pPointerParameterAssignments);
      }
    }
  }
}
