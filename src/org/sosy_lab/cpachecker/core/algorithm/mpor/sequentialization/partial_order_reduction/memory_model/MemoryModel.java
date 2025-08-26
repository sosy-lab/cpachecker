// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/**
 * A class to keep track of all memory locations in the concurrent input program, including pointers
 * associated with a memory location.
 */
public class MemoryModel {

  private final int memoryLocationAmount;

  private final ImmutableMap<MemoryLocation, Integer> memoryLocationIds;

  // TODO we could map MemoryLocation, MemoryLocation here directly
  private final ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments;

  private final ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
      pointerParameterAssignments;

  private final ImmutableSet<MemoryLocation> pointerDereferences;

  // public constructor for unit tests
  public MemoryModel(
      ImmutableMap<MemoryLocation, Integer> pMemoryLocationIds,
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
          pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    checkArguments(pPointerAssignments, pPointerParameterAssignments);
    memoryLocationAmount = pMemoryLocationIds.size();
    memoryLocationIds = pMemoryLocationIds;
    pointerAssignments = pPointerAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
    pointerDereferences = pPointerDereferences;
  }

  private static void checkArguments(
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
          pPointerParameterAssignments) {

    for (CVariableDeclaration variableDeclaration : pPointerAssignments.keySet()) {
      checkArgument(
          variableDeclaration.getType() instanceof CPointerType,
          "variableDeclaration must be CPointerType, got %s",
          variableDeclaration.getType());
    }
    for (CParameterDeclaration parameterDeclaration : pPointerParameterAssignments.columnKeySet()) {
      checkArgument(
          parameterDeclaration.getType() instanceof CPointerType,
          "parameterDeclaration must be CPointerType, got %s",
          parameterDeclaration.getType());
    }
  }

  // boolean helpers ===============================================================================

  /**
   * Returns true if the pointer in {@code pVariableDeclaration} is assigned a pointer {@code ptr},
   * or the address of a non-pointer {@code &non_ptr}.
   */
  public boolean isAssignedPointer(CVariableDeclaration pVariableDeclaration) {
    checkArgument(
        pVariableDeclaration.getType() instanceof CPointerType,
        "pVariableDeclaration must be CPointerType, got %s",
        pVariableDeclaration.getType());
    return pointerAssignments.containsKey(pVariableDeclaration);
  }

  /**
   * Returns true if the pointer in {@code pParameterDeclaration} is assigned a pointer {@code ptr},
   * or the address of a non-pointer {@code &non_ptr} in any function call.
   */
  public boolean isAssignedPointerParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    checkArgument(
        pParameterDeclaration.getType() instanceof CPointerType,
        "pParameterDeclaration must be CPointerType, got %s",
        pParameterDeclaration.getType());
    return pointerParameterAssignments.contains(pCallContext, pParameterDeclaration);
  }

  /**
   * Returns {@code true} if {@code pMemoryLocation} is implicitly global e.g. through {@code
   * global_ptr = &local_var;}. Returns {@code false} even if the memory location itself is global.
   */
  public boolean isImplicitGlobal(MemoryLocation pMemoryLocation) {
    if (pMemoryLocation.isGlobal()) {
      return false;
    }
    // TODO also handle parameterPointerAssignments here
    for (CVariableDeclaration pointerDeclaration : pointerAssignments.keySet()) {
      if (pointerAssignments.get(pointerDeclaration).contains(pMemoryLocation)) {
        ImmutableSet<CVariableDeclaration> transitivePointerDeclarations =
            findPointerDeclarationsByPointerAssignments(pointerDeclaration);
        for (CVariableDeclaration transitivePointerDeclaration : transitivePointerDeclarations) {
          if (transitivePointerDeclaration.isGlobal()) {
            return true;
          }
        }
      }
    }
    for (MemoryLocation pointerDereference : pointerDereferences) {
      if (pointerDereference.equals(pMemoryLocation)) {
        ImmutableSet<MemoryLocation> memoryLocations =
            MemoryLocationFinder.findMemoryLocationsByPointerDereference(
                pointerDereference, pMemoryLocation.callContext, this);
        for (MemoryLocation memoryLocation : memoryLocations) {
          if (memoryLocation.isGlobal()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private ImmutableSet<CVariableDeclaration> findPointerDeclarationsByPointerAssignments(
      CVariableDeclaration pPointerDeclaration) {

    Set<CVariableDeclaration> rFound = new HashSet<>();
    rFound.add(pPointerDeclaration);
    recursivelyFindPointerDeclarationsByPointerAssignments(
        pPointerDeclaration, rFound, new HashSet<>());
    return ImmutableSet.copyOf(rFound);
  }

  private void recursivelyFindPointerDeclarationsByPointerAssignments(
      CVariableDeclaration pCurrentPointerDeclaration,
      Set<CVariableDeclaration> pFound,
      Set<CVariableDeclaration> pVisited) {

    // should always hold, so we use assert instead of checkArgument
    assert pCurrentPointerDeclaration.getType() instanceof CPointerType
        : "type of pCurrentPointerDeclaration must be CPointerType";

    if (pVisited.add(pCurrentPointerDeclaration)) {
      if (pointerAssignments.containsKey(pCurrentPointerDeclaration)) {
        for (CVariableDeclaration pointerDeclaration : pointerAssignments.keySet()) {
          if (pVisited.add(pointerDeclaration)) {
            for (MemoryLocation assignedMemoryLocation :
                pointerAssignments.get(pointerDeclaration)) {
              CSimpleDeclaration simpleDeclaration = assignedMemoryLocation.getSimpleDeclaration();
              if (simpleDeclaration instanceof CVariableDeclaration variableDeclaration) {
                if (pointerAssignments.containsKey(variableDeclaration)) {
                  pFound.add(pointerDeclaration);
                  recursivelyFindPointerDeclarationsByPointerAssignments(
                      pointerDeclaration, pFound, pVisited);
                }
              }
            }
          }
        }
      }
    }
  }

  // getters =======================================================================================

  public ImmutableSet<MemoryLocation> getRightHandSidesByPointer(
      CVariableDeclaration pVariableDeclaration) {

    return pointerAssignments.get(pVariableDeclaration);
  }

  public MemoryLocation getRightHandSideByPointerParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    return pointerParameterAssignments.get(pCallContext, pParameterDeclaration);
  }

  public int getMemoryLocationAmount() {
    return memoryLocationAmount;
  }

  public ImmutableMap<MemoryLocation, Integer> getMemoryLocationIds() {
    return memoryLocationIds;
  }

  /**
   * Returns the set of relevant memory locations, i.e. all that are important to decide whether two
   * statements commute. These are all explicit and implicit global memory locations.
   */
  public ImmutableSet<MemoryLocation> getRelevantMemoryLocations() {
    ImmutableSet.Builder<MemoryLocation> rRelevant = ImmutableSet.builder();
    for (MemoryLocation memoryLocation : memoryLocationIds.keySet()) {
      // TODO non-pointer parameters should be included. e.g. 'global_ptr = &non-ptr_param'
      // exclude parameters, they are not memory locations themselves
      if (!memoryLocation.isParameter()) {
        // exclude const CPAchecker_TMP, they do not have any effect in the input program
        if (!MemoryLocationUtil.isConstCpaCheckerTmp(memoryLocation)) {
          if (memoryLocation.isGlobal() || isImplicitGlobal(memoryLocation)) {
            rRelevant.add(memoryLocation);
          }
        }
      }
    }
    return rRelevant.build();
  }
}
