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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

public class SeqMemoryLocationFinder {

  /**
   * Returns {@code true} if any global memory location is (possibly) accessed when executing {@code
   * pBlock} and its directly linked blocks.
   */
  public static boolean containsRelevantMemoryLocation(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel) {

    ImmutableSet<SeqMemoryLocation> foundMemoryLocations =
        findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pBlock, pMemoryModel, MemoryAccessType.ACCESS);
    return pMemoryModel.getRelevantMemoryLocations().keySet().stream()
        .anyMatch(relevantMemoryLocation -> foundMemoryLocations.contains(relevantMemoryLocation));
  }

  public static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByReachType(
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType,
      ReachType pReachType) {

    return switch (pReachType) {
      case DIRECT ->
          findDirectMemoryLocationsByAccessType(pLabelBlockMap, pBlock, pMemoryModel, pAccessType);
      case REACHABLE ->
          findReachableMemoryLocationsByAccessType(
              pLabelClauseMap, pLabelBlockMap, pBlock, pMemoryModel, pAccessType);
    };
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<SeqMemoryLocation> findDirectMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetGotoStatements(found, statement, pLabelBlockMap);
      ImmutableSet<SeqMemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(ImmutableSet.copyOf(found), pMemoryModel, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock}, its directly linked blocks
   * and all possible successor blocks, that may or may not actually be executed.
   */
  public static ImmutableSet<SeqMemoryLocation> findReachableMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetStatements(
          found, statement, pLabelClauseMap, pLabelBlockMap);
      ImmutableSet<SeqMemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(ImmutableSet.copyOf(found), pMemoryModel, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  // Memory Location Extraction ====================================================================

  private static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByStatements(
      ImmutableSet<SeqThreadStatement> pStatements,
      MemoryModel pMemoryModel,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.data().getSubstituteEdges()) {
        rMemLocations.addAll(
            findMemoryLocationsBySubstituteEdge(substituteEdge, pMemoryModel, pAccessType));
      }
    }
    return rMemLocations.build();
  }

  public static ImmutableSet<SeqMemoryLocation> findMemoryLocationsBySubstituteEdge(
      SubstituteEdge pSubstituteEdge, MemoryModel pMemoryModel, MemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    // first check direct accesses on the memory locations themselves
    rMemLocations.addAll(pSubstituteEdge.getMemoryLocationsByAccessType(pAccessType));
    // then check indirect accesses via pointers that point to the variables
    ImmutableSet<SeqMemoryLocation> pointerDereferences =
        pSubstituteEdge.getPointerDereferencesByAccessType(pAccessType);
    for (SeqMemoryLocation pointerDereference : pointerDereferences) {
      rMemLocations.addAll(
          findMemoryLocationsByPointerDereference(
              pointerDereference,
              pMemoryModel.pointerAssignments,
              pMemoryModel.startRoutineArgAssignments,
              pMemoryModel.pointerParameterAssignments));
    }
    return rMemLocations.build();
  }

  // Extraction by Pointer Dereference =============================================================

  /**
   * Finds the set of {@link SeqMemoryLocation}s that have {@link CVariableDeclaration}s that are
   * associated by the given pointer dereference, i.e. the set of global variables whose addresses
   * are at some point in the program assigned to the pointer.
   */
  static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByPointerDereference(
      SeqMemoryLocation pPointerDereference,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    // the set of memory locations associated with the pointer dereference
    Set<SeqMemoryLocation> found = new HashSet<>();
    // set of already visited memory locations
    Set<SeqMemoryLocation> visited = new HashSet<>();
    // stack to iteratively perform depth first search
    Deque<SeqMemoryLocation> stack = new ArrayDeque<>();

    // start the search by pushing the initial pointer dereference
    stack.push(pPointerDereference);
    visited.add(pPointerDereference); // Add initial location to visited immediately

    while (!stack.isEmpty()) {
      SeqMemoryLocation currentMemoryLocation = stack.pop();
      // check if the current location is a pointer (an LHS in an assignment)
      if (MemoryModel.isLeftHandSideInPointerAssignment(
          currentMemoryLocation,
          pPointerAssignments,
          pStartRoutineArgAssignments,
          pPointerParameterAssignments)) {

        // if it is a pointer, find what it points to (the RHS in the assignment)
        ImmutableSet<SeqMemoryLocation> rightHandSides =
            MemoryModel.getPointerAssignmentRightHandSides(
                currentMemoryLocation,
                pPointerAssignments,
                pStartRoutineArgAssignments,
                pPointerParameterAssignments);

        // add unvisited RHSs into the stack
        for (SeqMemoryLocation rightHandSide : rightHandSides) {
          if (visited.add(rightHandSide)) {
            stack.push(rightHandSide);
          }
        }
      } else {
        // if it is not a pointer (i.e. a target memory location), add it to found
        found.add(currentMemoryLocation);
      }
    }
    return ImmutableSet.copyOf(found);
  }
}
