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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
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

  private final ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments;

  /**
   * The table of call context i.e. {@link ThreadEdge} sensitive {@link CParameterDeclaration}
   * mapped to their assigned {@link MemoryLocation}. Note that this is not restricted to pointers,
   * since non-pointer parameters can be made implicitly global through global pointers.
   */
  private final ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
      parameterAssignments;

  /** The subset of parameters that are pointers. */
  private final ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
      pointerParameterAssignments;

  private final ImmutableSet<MemoryLocation> pointerDereferences;

  // public constructor for unit tests
  public MemoryModel(
      ImmutableMap<MemoryLocation, Integer> pMemoryLocationIds,
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation> pParameterAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
          pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    checkArguments(pPointerAssignments);
    memoryLocationAmount = pMemoryLocationIds.size();
    memoryLocationIds = pMemoryLocationIds;
    pointerAssignments = pPointerAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
    parameterAssignments = pParameterAssignments;
    pointerDereferences = pPointerDereferences;
  }

  private static void checkArguments(
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments) {

    for (CVariableDeclaration variableDeclaration : pPointerAssignments.keySet()) {
      checkArgument(
          variableDeclaration.getType() instanceof CPointerType,
          "variableDeclaration must be CPointerType, got %s",
          variableDeclaration.getType());
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

    if (pParameterDeclaration.getType() instanceof CPointerType) {
      return parameterAssignments.contains(pCallContext, pParameterDeclaration);
    }
    return false;
  }

  /**
   * Returns {@code true} if any pointer (parameter or variable, both local and global) points to
   * {@code pMemoryLocation}.
   */
  private boolean isPointedTo(MemoryLocation pMemoryLocation) {
    if (pointerAssignments.values().contains(pMemoryLocation)) {
      return true;
    }
    if (pointerParameterAssignments.containsValue(pMemoryLocation)) {
      return true;
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pMemoryLocation} is implicitly global e.g. through {@code
   * global_ptr = &local_var;}. Returns {@code false} even if the memory location itself is global.
   */
  public boolean isImplicitGlobal(MemoryLocation pMemoryLocation) {
    if (pMemoryLocation.isGlobal()) {
      return false;
    }
    if (isPointedTo(pMemoryLocation)) {
      for (CVariableDeclaration pointerDeclaration : pointerAssignments.keySet()) {
        if (pointerDeclaration.isGlobal()) {
          return true;
        }
        ImmutableSet<CVariableDeclaration> transitivePointerDeclarations =
            MemoryLocationFinder.findPointerDeclarationsByPointerAssignments(
                pointerDeclaration, pointerAssignments, parameterAssignments);
        for (CVariableDeclaration transitivePointerDeclaration : transitivePointerDeclarations) {
          if (transitivePointerDeclaration.isGlobal()) {
            return true;
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
    }
    return false;
  }

  // getters =======================================================================================

  public ImmutableSet<MemoryLocation> getRightHandSidesByPointer(
      CVariableDeclaration pVariableDeclaration) {

    return pointerAssignments.get(pVariableDeclaration);
  }

  public MemoryLocation getRightHandSideByParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    return parameterAssignments.get(pCallContext, pParameterDeclaration);
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
      // exclude const CPAchecker_TMP, they do not have any effect in the input program
      if (!MemoryLocationUtil.isConstCpaCheckerTmp(memoryLocation)) {
        if (memoryLocation.isGlobal() || isImplicitGlobal(memoryLocation)) {
          rRelevant.add(memoryLocation);
        }
      }
    }
    return rRelevant.build();
  }
}
