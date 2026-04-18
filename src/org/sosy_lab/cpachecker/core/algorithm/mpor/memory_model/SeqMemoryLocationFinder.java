// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryModelUtil.CFieldReferenceVisitor;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
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

    return ImmutableSet.<SeqMemoryLocation>builder()
        .addAll(pSubstituteEdge.getMemoryLocationsByAccessType(pAccessType))
        .addAll(
            findMemoryLocationsByPointerDereferences(
                pSubstituteEdge.getPointerDereferencesByAccessType(pAccessType), pMemoryModel))
        .build();
  }

  public static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByPointerDereferences(
      ImmutableSet<SeqMemoryLocation> pPointerDereferences, MemoryModel pMemoryModel) {

    return pPointerDereferences.stream()
        .flatMap(
            pointerDereference ->
                findMemoryLocationsByPointerDereference(
                    pointerDereference,
                    pMemoryModel.pointerAssignments,
                    pMemoryModel.startRoutineArgAssignments,
                    pMemoryModel.pointerParameterAssignments)
                    .stream())
        .collect(ImmutableSet.toImmutableSet());
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
        rightHandSides.stream().filter(visited::add).forEach(stack::push);
      } else {
        SeqMemoryLocation targetMemoryLocation =
            getTargetMemoryLocation(pPointerDereference, currentMemoryLocation);
        // if field member is a pointer then it must be dereferenced too, otherwise add to found
        if (targetMemoryLocation.isFieldMemberPointerType()) {
          stack.push(targetMemoryLocation);
        } else {
          found.add(targetMemoryLocation);
        }
      }
    }
    return ImmutableSet.copyOf(found);
  }

  private static SeqMemoryLocation getTargetMemoryLocation(
      SeqMemoryLocation pPointerDereference, SeqMemoryLocation pCurrentMemoryLocation) {

    checkArgument(
        !pCurrentMemoryLocation.isFieldOwnerPointerType(),
        "pCurrentMemoryLocation field owner cannot be CPointerType.");

    CType currentType = pCurrentMemoryLocation.declaration().getType();
    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();

    if (MemoryModelUtil.isAnyTypeTargetType(currentType, CCompositeType.class, stopNames)) {
      CInitializer initializer = pPointerDereference.declaration().getInitializer();
      if (initializer instanceof CInitializerExpression initializerExpression) {
        CFieldReferenceVisitor fieldReferenceVisitor = new CFieldReferenceVisitor();
        initializerExpression.getExpression().accept(fieldReferenceVisitor);
        if (!fieldReferenceVisitor.getFieldReferences().isEmpty()) {
          CFieldReference fieldReference =
              Iterables.getOnlyElement(fieldReferenceVisitor.getFieldReferences());
          CCompositeTypeMemberDeclaration fieldMemberDeclaration =
              MemoryModelUtil.getCompositeTypeMemberDeclarationByFieldName(
                  fieldReference.getFieldOwner().getExpressionType(),
                  fieldReference.getFieldName());
          // pass on the fieldMember, because currentMemoryLocation does not contain it
          CFieldReference newFieldReference =
              new CFieldReference(
                  FileLocation.DUMMY,
                  pCurrentMemoryLocation.declaration().getType(),
                  fieldMemberDeclaration.getName(),
                  Iterables.getOnlyElement(pCurrentMemoryLocation.expressions()),
                  // not a pointer dereference because currentMemoryLocation is not a pointer
                  false);
          return SeqMemoryLocation.of(
              pCurrentMemoryLocation.callContext(),
              pCurrentMemoryLocation.declaration(),
              fieldMemberDeclaration,
              ImmutableList.of(newFieldReference));
        }
      }
    }
    return pCurrentMemoryLocation;
  }
}
