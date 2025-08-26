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
import com.google.common.collect.ImmutableTable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocationFinder {

  /**
   * Returns {@code true} if any global memory location is (possibly) accessed when executing {@code
   * pBlock} and its directly linked blocks.
   */
  public static boolean hasGlobalAccess(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      MemoryModel pMemoryModel,
      SeqThreadStatementBlock pBlock) {

    ImmutableSet<MemoryLocation> memoryLocations =
        findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pMemoryModel, pBlock, MemoryAccessType.ACCESS);
    return !memoryLocations.stream()
        .filter(MemoryLocation::isGlobal)
        .collect(ImmutableSet.toImmutableSet())
        .isEmpty();
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<MemoryLocation> findDirectMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      MemoryModel pMemoryModel,
      SeqThreadStatementBlock pBlock,
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
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      MemoryModel pMemoryModel,
      SeqThreadStatementBlock pBlock,
      MemoryAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetStatements(found, statement, pLabelBlockMap);
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
              pointerDereference, pSubstituteEdge.getCallContext(), pMemoryModel));
    }
    return rMemLocations.build();
  }

  // Extraction by Pointer Assignments (including Parameters) ======================================

  public static ImmutableSet<CVariableDeclaration> findPointerDeclarationsByPointerAssignments(
      CVariableDeclaration pPointerDeclaration,
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation> pParameterAssignments) {

    Set<CVariableDeclaration> rFound = new HashSet<>();
    recursivelyFindPointerDeclarationsByPointerAssignments(
        pPointerDeclaration, pPointerAssignments, pParameterAssignments, rFound, new HashSet<>());
    return ImmutableSet.copyOf(rFound);
  }

  private static void recursivelyFindPointerDeclarationsByPointerAssignments(
      CSimpleDeclaration pCurrentPointerDeclaration,
      final ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      final ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation> pParameterAssignments,
      Set<CVariableDeclaration> pFound,
      Set<CSimpleDeclaration> pVisited) {

    // should always hold, so we use assert instead of checkArgument
    assert pCurrentPointerDeclaration.getType() instanceof CPointerType
        : "type of pCurrentPointerDeclaration must be CPointerType";

    if (pCurrentPointerDeclaration instanceof CVariableDeclaration) {
      if (pPointerAssignments.containsKey(pCurrentPointerDeclaration)) {
        for (CVariableDeclaration pointerDeclaration : pPointerAssignments.keySet()) {
          if (pVisited.add(pointerDeclaration)) {
            for (MemoryLocation memoryLocation : pPointerAssignments.get(pointerDeclaration)) {
              CSimpleDeclaration simpleDeclaration = memoryLocation.getSimpleDeclaration();
              if (simpleDeclaration instanceof CVariableDeclaration) {
                if (pPointerAssignments.containsKey(simpleDeclaration)) {
                  pFound.add(pointerDeclaration);
                  recursivelyFindPointerDeclarationsByPointerAssignments(
                      pointerDeclaration,
                      pPointerAssignments,
                      pParameterAssignments,
                      pFound,
                      pVisited);
                }
              }
              if (simpleDeclaration instanceof CParameterDeclaration) {
                pFound.add(pointerDeclaration);
                // consider only pointer parameters, all others are not relevant
                if (simpleDeclaration.getType() instanceof CPointerType) {
                  // call context is always present for parameters
                  ThreadEdge callContext = memoryLocation.callContext.orElseThrow();
                  if (pParameterAssignments.contains(callContext, simpleDeclaration)) {
                    recursivelyFindPointerDeclarationsByPointerAssignments(
                        pointerDeclaration,
                        pPointerAssignments,
                        pParameterAssignments,
                        pFound,
                        pVisited);
                  }
                }
              }
            }
          }
        }
      }
    }
    if (pCurrentPointerDeclaration instanceof CParameterDeclaration parameterDeclaration) {
      if (parameterDeclaration.getType() instanceof CPointerType) {
        if (pParameterAssignments.containsColumn(parameterDeclaration)) {
          ImmutableMap<ThreadEdge, MemoryLocation> column =
              pParameterAssignments.column(parameterDeclaration);
          for (MemoryLocation memoryLocation : column.values()) {
            recursivelyFindPointerDeclarationsByPointerAssignments(
                memoryLocation.getSimpleDeclaration(),
                pPointerAssignments,
                pParameterAssignments,
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
      Optional<ThreadEdge> pCallContext,
      MemoryModel pMemoryModel) {

    Set<MemoryLocation> found = new HashSet<>();
    recursivelyFindMemoryLocationsByPointerDereference(
        pPointerDereference, pCallContext, pMemoryModel, found, new HashSet<>());
    return ImmutableSet.copyOf(found);
  }

  /**
   * Finds the set of {@link CVariableDeclaration}s that are associated by the given pointer
   * dereference, i.e. the set of global variables whose addresses are at some point in the program
   * assigned to the pointer variable / parameter.
   */
  private static void recursivelyFindMemoryLocationsByPointerDereference(
      MemoryLocation pCurrentMemoryLocation,
      final Optional<ThreadEdge> pCallContext,
      final MemoryModel pMemoryModel,
      Set<MemoryLocation> pFound,
      Set<MemoryLocation> pVisited) {

    // prevent infinite loop, e.g. if a pointer is assigned itself: 'ptr = ptr;'
    if (pVisited.add(pCurrentMemoryLocation)) {
      CSimpleDeclaration simpleDeclaration = pCurrentMemoryLocation.getSimpleDeclaration();
      if (simpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
        if (variableDeclaration.getType() instanceof CPointerType) {
          // it is possible that a pointer is not in the map, if it is e.g. initialized with malloc
          // and then dereferenced -> the pointer is not associated with the address of a var
          if (pMemoryModel.isAssignedPointer(variableDeclaration)) {
            ImmutableSet<MemoryLocation> rightHandSides =
                pMemoryModel.getRightHandSidesByPointer(variableDeclaration);
            for (MemoryLocation rightHandSide : rightHandSides) {
              recursivelyFindMemoryLocationsByPointerDereference(
                  rightHandSide, pCallContext, pMemoryModel, pFound, pVisited);
            }
          }
        } else {
          pFound.add(pCurrentMemoryLocation);
        }

      } else if (simpleDeclaration instanceof CParameterDeclaration parameterDeclaration) {
        assert pCallContext.isPresent() : "call context must be present for CParameterDeclaration";
        ThreadEdge callContext = pCallContext.orElseThrow();
        // in pthread_create that does not pass an arg to start_routine, the pair is not present
        if (pMemoryModel.isAssignedPointerParameter(callContext, parameterDeclaration)) {
          MemoryLocation rightHandSide =
              pMemoryModel.getRightHandSideByParameter(callContext, parameterDeclaration);
          recursivelyFindMemoryLocationsByPointerDereference(
              rightHandSide, pCallContext, pMemoryModel, pFound, pVisited);
        }
      }
    }
  }
}
