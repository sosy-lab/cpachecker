// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class MemoryLocationFinder {

  /**
   * Returns {@code true} if any global memory location is (possibly) accessed when executing {@code
   * pBlock} and its directly linked blocks.
   */
  static boolean hasGlobalAccess(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      PointerAssignments pPointerAssignments,
      SeqThreadStatementBlock pBlock) {

    return !findDirectMemoryLocationsByAccessType(
            pLabelBlockMap,
            pAllMemoryLocations,
            pPointerAssignments,
            pBlock,
            BitVectorAccessType.ACCESS)
        .isEmpty();
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<MemoryLocation> findDirectMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      PointerAssignments pPointerAssignments,
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetGotoStatements(found, statement, pLabelBlockMap);
      ImmutableSet<MemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(
              ImmutableSet.copyOf(found), pAllMemoryLocations, pPointerAssignments, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  // TODO also use ReachType here and remove redundant pLabelClauseMap
  /**
   * Returns all global variables accessed when executing {@code pBlock}, its directly linked blocks
   * and all possible successor blocks, that may or may not actually be executed.
   */
  static ImmutableSet<MemoryLocation> findReachableMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      PointerAssignments pPointerAssignments,
      SeqThreadStatementBlock pBlock,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetStatements(
          found, statement, pLabelClauseMap, pLabelBlockMap);
      ImmutableSet<MemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(
              ImmutableSet.copyOf(found), pAllMemoryLocations, pPointerAssignments, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  // Global Variable Extraction ====================================================================

  private static ImmutableSet<MemoryLocation> findMemoryLocationsByStatements(
      ImmutableSet<SeqThreadStatement> pStatements,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      PointerAssignments pPointerAssignments,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.getSubstituteEdges()) {
        rMemLocations.addAll(
            findMemoryLocationsBySubstituteEdge(
                substituteEdge, pAllMemoryLocations, pPointerAssignments, pAccessType));
      }
    }
    return rMemLocations.build();
  }

  private static ImmutableSet<MemoryLocation> findMemoryLocationsBySubstituteEdge(
      SubstituteEdge pSubstituteEdge,
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      PointerAssignments pPointerAssignments,
      BitVectorAccessType pAccessType) {

    ImmutableSet.Builder<MemoryLocation> rMemLocations = ImmutableSet.builder();
    // first check direct accesses on the variables themselves
    ImmutableSet<CVariableDeclaration> globalVariables =
        pSubstituteEdge.getGlobalVariablesByAccessType(pAccessType);
    for (CVariableDeclaration globalVariable : globalVariables) {
      rMemLocations.add(
          getMemoryLocationByVariableDeclaration(globalVariable, pAllMemoryLocations));
    }
    ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration> fieldMembers =
        pSubstituteEdge.getFieldMembersByAccessType(pAccessType);
    for (CVariableDeclaration fieldOwner : fieldMembers.keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember : fieldMembers.get(fieldOwner)) {
        rMemLocations.add(
            getMemoryLocationByFieldOwnerAndMember(fieldOwner, fieldMember, pAllMemoryLocations));
      }
    }
    // then check indirect accesses via pointers that point to the variables
    ImmutableSet<CSimpleDeclaration> pointerDereferences =
        pSubstituteEdge.getPointerDereferencesByAccessType(pAccessType);
    for (CSimpleDeclaration pointerDereference : pointerDereferences) {
      Set<MemoryLocation> found = new HashSet<>();
      recursivelyFindMemoryLocationsByPointerDereference(
          pointerDereference,
          pAllMemoryLocations,
          pSubstituteEdge.getCallContext(),
          pPointerAssignments,
          found,
          new HashSet<>());
      rMemLocations.addAll(found);
    }
    return rMemLocations.build();
  }

  // Extraction by Pointer Dereference =============================================================

  /**
   * Finds the set of {@link CVariableDeclaration}s that are associated by the given pointer
   * dereference, i.e. the set of global variables whose addresses are at some point in the program
   * assigned to the pointer variable / parameter.
   */
  private static void recursivelyFindMemoryLocationsByPointerDereference(
      CSimpleDeclaration pCurrentDeclaration,
      final ImmutableSet<MemoryLocation> pAllMemoryLocations,
      final Optional<ThreadEdge> pCallContext,
      final PointerAssignments pPointerAssignments,
      Set<MemoryLocation> pFound,
      Set<CSimpleDeclaration> pVisited) {

    // prevent infinite loop, e.g. if a pointer is assigned itself: 'ptr = ptr;'
    if (pVisited.add(pCurrentDeclaration)) {
      if (pCurrentDeclaration instanceof CVariableDeclaration variableDeclaration) {
        if (variableDeclaration.getType() instanceof CPointerType) {
          // it is possible that a pointer is not in the map, if it is e.g. initialized with malloc
          // and then dereferenced -> the pointer is not associated with the address of a var
          if (pPointerAssignments.isAssignedPointer(variableDeclaration)) {
            ImmutableSet<MemoryLocation> rightHandSides =
                pPointerAssignments.getRightHandSidesByPointer(variableDeclaration);
            for (MemoryLocation rightHandSide : rightHandSides) {
              recursivelyFindMemoryLocationsByPointerDereference(
                  rightHandSide.getSimpleDeclaration(),
                  pAllMemoryLocations,
                  pCallContext,
                  pPointerAssignments,
                  pFound,
                  pVisited);
            }
          }
        } else {
          pFound.add(
              getMemoryLocationByVariableDeclaration(variableDeclaration, pAllMemoryLocations));
        }

      } else if (pCurrentDeclaration instanceof CParameterDeclaration parameterDeclaration) {
        recursivelyFindMemoryLocationsByPointerDereference(
            parameterDeclaration,
            pAllMemoryLocations,
            pCallContext,
            pPointerAssignments,
            pFound,
            pVisited);
      }
    }
  }

  private static void recursivelyFindMemoryLocationsByPointerDereference(
      CParameterDeclaration pCurrentDeclaration,
      final ImmutableSet<MemoryLocation> pAllMemoryLocations,
      final Optional<ThreadEdge> pCallContext,
      final PointerAssignments pPointerAssignments,
      Set<MemoryLocation> pFound,
      Set<CSimpleDeclaration> pVisited) {

    assert pCallContext.isPresent() : "call context must be present for CParameterDeclaration";
    ThreadEdge callContext = pCallContext.orElseThrow();
    // in pthread_create that does not pass an arg to start_routine, the pair is not present
    if (pPointerAssignments.isAssignedPointerParameter(callContext, pCurrentDeclaration)) {
      MemoryLocation rightHandSide =
          pPointerAssignments.getRightHandSideByPointerParameter(callContext, pCurrentDeclaration);
      recursivelyFindMemoryLocationsByPointerDereference(
          rightHandSide.getSimpleDeclaration(),
          pAllMemoryLocations,
          pCallContext,
          pPointerAssignments,
          pFound,
          pVisited);
    }
  }

  // Helpers =======================================================================================

  private static MemoryLocation getMemoryLocationByVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, ImmutableSet<MemoryLocation> pAllMemoryLocations) {

    for (MemoryLocation memoryLocation : pAllMemoryLocations) {
      if (memoryLocation.variable.isPresent()) {
        if (memoryLocation.variable.orElseThrow().equals(pVariableDeclaration)) {
          return memoryLocation;
        }
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "could not find pVariableDeclaration %s in pAllMemoryLocations",
            pVariableDeclaration.toASTString()));
  }

  private static MemoryLocation getMemoryLocationByFieldOwnerAndMember(
      CVariableDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember,
      ImmutableSet<MemoryLocation> pAllMemoryLocations) {

    for (MemoryLocation memoryLocation : pAllMemoryLocations) {
      if (memoryLocation.fieldMember.isPresent()) {
        SimpleImmutableEntry<CSimpleDeclaration, CCompositeTypeMemberDeclaration> entry =
            memoryLocation.fieldMember.orElseThrow();
        if (entry.getKey().equals(pFieldOwner)) {
          if (entry.getValue().equals(pFieldMember)) {
            return memoryLocation;
          }
        }
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "could not find pFieldOwner %s and pFieldMember %s in pAllMemoryLocations",
            pFieldOwner.toASTString(), pFieldMember.toASTString()));
  }
}
