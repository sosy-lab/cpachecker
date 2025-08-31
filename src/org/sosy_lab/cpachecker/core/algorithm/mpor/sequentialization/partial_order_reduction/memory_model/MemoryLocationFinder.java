// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

public class MemoryLocationFinder {

  /**
   * Returns {@code true} if any global memory location is (possibly) accessed when executing {@code
   * pBlock} and its directly linked blocks.
   */
  public static boolean containsRelevantMemoryLocation(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel) {

    ImmutableSet<MemoryLocation> foundMemoryLocations =
        findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pBlock, pMemoryModel, MemoryAccessType.ACCESS);
    return pMemoryModel.getRelevantMemoryLocations().keySet().stream()
        .anyMatch(relevantMemoryLocation -> foundMemoryLocations.contains(relevantMemoryLocation));
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<MemoryLocation> findDirectMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetGotoStatements(found, statement, pLabelBlockMap);
      ImmutableSet<MemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(ImmutableSet.copyOf(found), pMemoryModel, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock}, its directly linked blocks
   * and all possible successor blocks, that may or may not actually be executed.
   */
  public static ImmutableSet<MemoryLocation> findReachableMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetStatements(
          found, statement, pLabelClauseMap, pLabelBlockMap);
      ImmutableSet<MemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(ImmutableSet.copyOf(found), pMemoryModel, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  // Memory Location Extraction ====================================================================

  private static ImmutableSet<MemoryLocation> findMemoryLocationsByStatements(
      ImmutableSet<SeqThreadStatement> pStatements,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        rMemLocations.addAll(
            findMemoryLocationsBySubstituteEdge(substituteEdge, pMemoryModel, pAccessType));
      }
    }
    return rMemLocations.build();
  }

  private static ImmutableSet<MemoryLocation> findMemoryLocationsBySubstituteEdge(
      SubstituteEdge pSubstituteEdge, MemoryModel pMemoryModel, MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    // first check direct accesses on the memory locations themselves
    rMemLocations.addAll(pSubstituteEdge.getMemoryLocationsByAccessType(pAccessType));
    // then check indirect accesses via pointers that point to the variables
    ImmutableSet<MemoryLocation> pointerDereferences =
        pSubstituteEdge.getPointerDereferencesByAccessType(pAccessType);
    for (MemoryLocation pointerDereference : pointerDereferences) {
      rMemLocations.addAll(
          findMemoryLocationsByPointerDereference(
              pointerDereference,
              pMemoryModel.pointerAssignments,
              pMemoryModel.startRoutineArgAssignments,
              pMemoryModel.pointerParameterAssignments));
    }
    return rMemLocations.build();
  }

  // Extraction by Pointer Assignments (including Parameters) ======================================

  public static ImmutableSet<MemoryLocation> findPointerDeclarationsByPointerAssignments(
      MemoryLocation pPointerDeclaration,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    Set<MemoryLocation> rFound = new HashSet<>();
    recursivelyFindPointerDeclarationsByPointerAssignments(
        pPointerDeclaration,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments,
        rFound,
        new HashSet<>());
    return ImmutableSet.copyOf(rFound);
  }

  private static void recursivelyFindPointerDeclarationsByPointerAssignments(
      MemoryLocation pCurrentMemoryLocation,
      final ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      final ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      final ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      Set<MemoryLocation> pFound,
      Set<MemoryLocation> pVisited) {

    if (MemoryModel.isLeftHandSideInPointerAssignment(
        pCurrentMemoryLocation,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments)) {
      for (MemoryLocation pointerDeclaration : pPointerAssignments.keySet()) {
        if (pVisited.add(pointerDeclaration)) {
          for (MemoryLocation memoryLocation : pPointerAssignments.get(pointerDeclaration)) {
            pFound.add(pointerDeclaration);
            recursivelyFindPointerDeclarationsByPointerAssignments(
                memoryLocation,
                pPointerAssignments,
                pStartRoutineArgAssignments,
                pPointerParameterAssignments,
                pFound,
                pVisited);
          }
        }
      }
    }
  }

  // Extraction by Pointer Dereference =============================================================

  public static ImmutableSet<MemoryLocation> findMemoryLocationsByPointerDereference(
      MemoryLocation pPointerDereference,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    Set<MemoryLocation> found = new HashSet<>();
    recursivelyFindMemoryLocationsByPointerDereference(
        pPointerDereference,
        pPointerAssignments,
        pStartRoutineArgAssignments,
        pPointerParameterAssignments,
        found,
        new HashSet<>());
    return ImmutableSet.copyOf(found);
  }

  /**
   * Finds the set of {@link CVariableDeclaration}s that are associated by the given pointer
   * dereference, i.e. the set of global variables whose addresses are at some point in the program
   * assigned to the pointer variable / parameter.
   */
  private static void recursivelyFindMemoryLocationsByPointerDereference(
      MemoryLocation pCurrentMemoryLocation,
      final ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      final ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      final ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      Set<MemoryLocation> pFound,
      Set<MemoryLocation> pVisited) {

    // prevent infinite loop, e.g. if a pointer is assigned itself: 'ptr = ptr;'
    if (pVisited.add(pCurrentMemoryLocation)) {
      if (MemoryModel.isLeftHandSideInPointerAssignment(
          pCurrentMemoryLocation,
          pPointerAssignments,
          pStartRoutineArgAssignments,
          pPointerParameterAssignments)) {
        ImmutableSet<MemoryLocation> rightHandSides =
            MemoryModel.getRightHandSideMemoryLocations(
                pCurrentMemoryLocation,
                pPointerAssignments,
                pStartRoutineArgAssignments,
                pPointerParameterAssignments);
        for (MemoryLocation rightHandSide : rightHandSides) {
          recursivelyFindMemoryLocationsByPointerDereference(
              rightHandSide,
              pPointerAssignments,
              pStartRoutineArgAssignments,
              pPointerParameterAssignments,
              pFound,
              pVisited);
        }
      } else {
        pFound.add(pCurrentMemoryLocation);
      }
    }
  }
}
