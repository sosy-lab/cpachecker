// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil.CExpressionCollector;
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
      SeqPointerAliasingMap pPointerAliasingMap) {

    ImmutableSet<SeqMemoryLocation> foundMemoryLocations =
        findDirectMemoryLocationsByAccessType(
            pLabelBlockMap, pBlock, pPointerAliasingMap, SeqMemoryAccessType.ACCESS);
    return pPointerAliasingMap.getRelevantMemoryLocations().keySet().stream()
        .anyMatch(relevantMemoryLocation -> foundMemoryLocations.contains(relevantMemoryLocation));
  }

  public static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByReachType(
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      SeqPointerAliasingMap pPointerAliasingMap,
      SeqMemoryAccessType pAccessType,
      SeqMemoryReachType pReachType) {

    return switch (pReachType) {
      case DIRECT ->
          findDirectMemoryLocationsByAccessType(
              pLabelBlockMap, pBlock, pPointerAliasingMap, pAccessType);
      case REACHABLE ->
          findReachableMemoryLocationsByAccessType(
              pLabelClauseMap, pLabelBlockMap, pBlock, pPointerAliasingMap, pAccessType);
    };
  }

  /**
   * Returns all global variables accessed when executing {@code pBlock} and its directly linked
   * blocks.
   */
  public static ImmutableSet<SeqMemoryLocation> findDirectMemoryLocationsByAccessType(
      ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      SeqThreadStatementBlock pBlock,
      SeqPointerAliasingMap pPointerAliasingMap,
      SeqMemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetGotoStatements(found, statement, pLabelBlockMap);
      ImmutableSet<SeqMemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(
              ImmutableSet.copyOf(found), pPointerAliasingMap, pAccessType);
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
      SeqPointerAliasingMap pPointerAliasingMap,
      SeqMemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pBlock.getStatements()) {
      Set<SeqThreadStatement> found = new HashSet<>();
      found.add(statement);
      SeqThreadStatementUtil.recursivelyFindTargetStatements(
          found, statement, pLabelClauseMap, pLabelBlockMap);
      ImmutableSet<SeqMemoryLocation> foundMemoryLocations =
          findMemoryLocationsByStatements(
              ImmutableSet.copyOf(found), pPointerAliasingMap, pAccessType);
      rMemLocations.addAll(foundMemoryLocations);
    }
    return rMemLocations.build();
  }

  // Memory Location Extraction ====================================================================

  private static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByStatements(
      ImmutableSet<SeqThreadStatement> pStatements,
      SeqPointerAliasingMap pPointerAliasingMap,
      SeqMemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemLocations = ImmutableSet.builder();
    for (SeqThreadStatement statement : pStatements) {
      for (SubstituteEdge substituteEdge : statement.data().getSubstituteEdges()) {
        rMemLocations.addAll(
            findMemoryLocationsBySubstituteEdge(substituteEdge, pPointerAliasingMap, pAccessType));
      }
    }
    return rMemLocations.build();
  }

  public static ImmutableSet<SeqMemoryLocation> findMemoryLocationsBySubstituteEdge(
      SubstituteEdge pSubstituteEdge,
      SeqPointerAliasingMap pPointerAliasingMap,
      SeqMemoryAccessType pAccessType) {

    ImmutableSet.Builder<SeqMemoryLocation> rMemoryLocations = ImmutableSet.builder();
    rMemoryLocations.addAll(pSubstituteEdge.getMemoryLocationsByAccessType(pAccessType));
    for (SeqMemoryLocation pointerDereference :
        pSubstituteEdge.getPointerDereferencesByAccessType(pAccessType)) {
      rMemoryLocations.addAll(
          findMemoryLocationsByPointerDereference(
              pointerDereference, pPointerAliasingMap.pointerAssignments));
    }
    return rMemoryLocations.build();
  }

  // Extraction by Pointer Dereference =============================================================

  /**
   * Finds the set of {@link SeqMemoryLocation}s that are associated by the given pointer
   * dereference, i.e. the set of {@link SeqMemoryLocation} that are at some point {@link
   * CRightHandSide} in an assignment where the pointer that is dereferenced is a {@link
   * CLeftHandSide}.
   */
  static ImmutableSet<SeqMemoryLocation> findMemoryLocationsByPointerDereference(
      final SeqMemoryLocation pPointerDereference,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    // the set of memory locations associated with the pointer dereference
    Set<SeqMemoryLocation> found = new HashSet<>();
    // set of already visited memory locations
    Set<SeqMemoryLocation> visited = new HashSet<>();
    // stack to iteratively perform depth first search
    Deque<SeqMemoryLocation> stack = new ArrayDeque<>();

    // start the search by pushing the initial pointer dereference
    stack.push(pPointerDereference);
    visited.add(pPointerDereference); // Add initial location to visited immediately

    final ImmutableSet<SeqMemoryLocation> pointerDereferenceRightHandSides =
        SeqPointerAliasingMap.getPointerAssignmentRightHandSides(
            pPointerDereference, pPointerAssignments);

    while (!stack.isEmpty()) {
      SeqMemoryLocation currentMemoryLocation = stack.pop();
      // check if the current location is a pointer (an LHS in an assignment)
      if (SeqPointerAliasingMap.isLeftHandSideInPointerAssignment(
          currentMemoryLocation, pPointerAssignments)) {

        // if it is a pointer, find what it points to (the RHS in the assignment)
        ImmutableSet<SeqMemoryLocation> rightHandSides =
            SeqPointerAliasingMap.getPointerAssignmentRightHandSides(
                currentMemoryLocation, pPointerAssignments);

        // add unvisited RHSs into the stack
        rightHandSides.stream().filter(visited::add).forEach(stack::push);
      } else {
        SeqMemoryLocation targetMemoryLocation =
            getTargetMemoryLocation(
                pPointerDereference, pointerDereferenceRightHandSides, currentMemoryLocation);
        // if field member is a pointer then it must be dereferenced too, otherwise add to found
        if (targetMemoryLocation.isFieldMemberPointerType()
            // it is possible that the target memory location equals the current memory location,
            // e.g. if the pointer was assigned '(void *)0' (something without a declaration)
            && !currentMemoryLocation.equals(targetMemoryLocation)) {
          stack.push(targetMemoryLocation);
        } else {
          found.add(targetMemoryLocation);
        }
      }
    }
    return ImmutableSet.copyOf(found);
  }

  /**
   * Returns the target memory location that is found when dereferencing {@code
   * pPointerDereference}. The target memory location is either:
   *
   * <ul>
   *   <li>{@code pCurrentMemoryLocation}
   *   <li>{@code pCurrentMemoryLocation} with an added {@link CCompositeTypeMemberDeclaration} in
   *       case the dereference does not target {@code pCurrentMemoryLocation} but one of its field
   *       members.
   * </ul>
   *
   * Note that the returned {@link SeqMemoryLocation} can also be of type {@link CPointerType}, e.g.
   * in the following example. In this case the dereference of {@code ptr} in the {@code main()}
   * function would backtrack the assignments and result in the set of target memory locations
   * {@code { a, new }} and not {@code { a, (void *)0 }} since {@code (void *)0} is not a {@link
   * SeqMemoryLocation} because it has no {@link CSimpleDeclaration} in it.
   *
   * <pre>{@code
   * void compare_and_swap(int **p, int* cmp, int* new) {
   *     if (*p == cmp) {
   *         *p = new;
   *     }
   * }
   * int main() {
   *   int a;
   *   int *ptr = & a;
   *   ti_cas(& ptr, & a, ((void *)0));
   * }
   * }</pre>
   */
  private static SeqMemoryLocation getTargetMemoryLocation(
      final SeqMemoryLocation pPointerDereference,
      final ImmutableSet<SeqMemoryLocation> pPointerDereferenceRightHandSides,
      SeqMemoryLocation pCurrentMemoryLocation) {

    SeqMemoryLocation targetMemoryLocation = pCurrentMemoryLocation;

    if (pCurrentMemoryLocation.declaration() != null) {
      CType currentType = pCurrentMemoryLocation.declaration().getType();
      ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();

      // only add a field reference memory location if there is at least one CCompositeType
      if (SeqPointerAliasingUtil.isAnyTypeTargetClass(
          currentType, CCompositeType.class, stopNames)) {
        if (pPointerDereference.declaration() instanceof CVariableDeclaration variableDeclaration) {
          CInitializer initializer = variableDeclaration.getInitializer();
          if (initializer instanceof CInitializerExpression initializerExpression) {
            CExpressionCollector<CFieldReference> fieldReferenceCollector =
                new CExpressionCollector<>(CFieldReference.class);
            initializerExpression.getExpression().accept(fieldReferenceCollector);
            if (!fieldReferenceCollector.getCollected().isEmpty()) {
              CFieldReference fieldReference =
                  Iterables.getOnlyElement(fieldReferenceCollector.getCollected());
              targetMemoryLocation =
                  getTargetMemoryLocationWithFieldMember(
                      fieldReference.getFieldOwner().getExpressionType(),
                      fieldReference.getFieldName(),
                      pCurrentMemoryLocation);
            }
          } else if (pPointerDereference.fieldMember().isPresent()) {
            String fieldMemberName =
                pCurrentMemoryLocation.fieldMember().isPresent()
                    ? pCurrentMemoryLocation.fieldMember().orElseThrow().getName()
                    : pPointerDereference.fieldMember().orElseThrow().getName();
            targetMemoryLocation =
                getTargetMemoryLocationWithFieldMember(
                    currentType, fieldMemberName, pCurrentMemoryLocation);
          } else {
            for (SeqMemoryLocation rightHandSide : pPointerDereferenceRightHandSides) {
              if (rightHandSide.fieldMember().isPresent()) {
                CType rhsType = rightHandSide.declaration().getType();
                if (rhsType.equals(currentType)
                    || (rhsType instanceof CPointerType pointerType
                        && pointerType.getType().equals(currentType))) {
                  targetMemoryLocation =
                      getTargetMemoryLocationWithFieldMember(
                          currentType,
                          rightHandSide.fieldMember().orElseThrow().getName(),
                          pCurrentMemoryLocation);
                }
              }
            }
          }
        }
      }
    }
    return targetMemoryLocation;
  }

  private static SeqMemoryLocation getTargetMemoryLocationWithFieldMember(
      CType pFieldOwnerType, String pFieldName, SeqMemoryLocation pCurrentMemoryLocation) {

    CCompositeTypeMemberDeclaration fieldMemberDeclaration =
        SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationByFieldName(
            pFieldOwnerType, pFieldName);
    return SeqMemoryLocation.of(
        pCurrentMemoryLocation.callContext(),
        Objects.requireNonNull(pCurrentMemoryLocation.declaration()),
        Optional.of(fieldMemberDeclaration));
  }
}
