// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;

/**
 * A class to keep track of all memory locations in the concurrent input program, including pointers
 * associated with a memory location.
 */
public class MemoryModel {

  private final ImmutableList<MemoryLocation> allMemoryLocations;

  private final int relevantMemoryLocationAmount;

  /**
   * The set of relevant {@link MemoryLocation}s, i.e. all that are needed to decide whether a
   * statement commutes. This includes explicit and implicit (through pointers) memory locations.
   */
  private final ImmutableMap<MemoryLocation, Integer> relevantMemoryLocations;

  public final ImmutableSetMultimap<MemoryLocation, MemoryLocation> pointerAssignments;

  /**
   * Keep track of {@code start_routine arg} assignments in {@code pthread_create} separately, since
   * even a local memory address passed here is implicitly global.
   */
  public final ImmutableMap<MemoryLocation, MemoryLocation> startRoutineArgAssignments;

  /**
   * The map of call context-sensitive {@link MemoryLocation} mapped to their assigned {@link
   * MemoryLocation}. Each parameter is only assigned once due to function cloning. Note that this
   * is not restricted to pointers, since non-pointer parameters can be made implicitly global
   * through global pointers.
   */
  public final ImmutableMap<MemoryLocation, MemoryLocation> parameterAssignments;

  /** The subset of parameters that are pointers. */
  public final ImmutableMap<MemoryLocation, MemoryLocation> pointerParameterAssignments;

  public final ImmutableSet<MemoryLocation> pointerDereferences;

  MemoryModel(
      ImmutableList<MemoryLocation> pAllMemoryLocations,
      ImmutableMap<MemoryLocation, Integer> pRelevantMemoryLocationIds,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pParameterAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments,
      ImmutableSet<MemoryLocation> pPointerDereferences) {

    checkArguments(pPointerAssignments, pParameterAssignments, pPointerParameterAssignments);
    allMemoryLocations = pAllMemoryLocations;
    relevantMemoryLocationAmount = pRelevantMemoryLocationIds.size();
    relevantMemoryLocations = pRelevantMemoryLocationIds;
    startRoutineArgAssignments = pStartRoutineArgAssignments;
    pointerAssignments = pPointerAssignments;
    parameterAssignments = pParameterAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
    pointerDereferences = pPointerDereferences;
  }

  private static void checkArguments(
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pParameterAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    // check that all left hand sides in pointer assignments are CPointerType
    for (MemoryLocation memoryLocation : pPointerAssignments.keySet()) {
      if (memoryLocation.fieldMember.isPresent()) {
        // for field owner / members: only the member must be CPointerType
        CCompositeTypeMemberDeclaration memberDeclaration =
            memoryLocation.fieldMember.orElseThrow();
        checkArgument(
            memberDeclaration.getType() instanceof CPointerType,
            "memberDeclaration must be CPointerType, got %s",
            memberDeclaration.getType());
      } else {
        // for all else: the variable itself must be CPointerType
        checkArgument(
            memoryLocation.declaration.getType() instanceof CPointerType,
            "variableDeclaration must be CPointerType, got %s",
            memoryLocation.declaration.getType());
      }
    }
    // check that pointer assignments contains all pointer parameter assignments (i.e. subset)
    for (var entry : pPointerParameterAssignments.entrySet()) {
      checkArgument(
          pParameterAssignments.containsKey(entry.getKey()),
          "pParameterAssignments must contain all pPointerParameterAssignments");
      checkArgument(
          Objects.equals(pParameterAssignments.get(entry.getKey()), entry.getValue()),
          "pParameterAssignments must contain all pPointerParameterAssignments");
    }
  }

  // boolean helpers ===============================================================================

  /**
   * Returns true if the pointer in {@code pMemoryLocation} is assigned a pointer {@code ptr}, or
   * the address of a non-pointer {@code &non_ptr}.
   */
  static boolean isLeftHandSideInPointerAssignment(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    if (pMemoryLocation.isFieldOwnerPointerType()) {
      return isLeftHandSideInPointerAssignment(
          pMemoryLocation.getFieldOwnerMemoryLocation(),
          pPointerAssignments,
          pStartRoutineArgAssignments,
          pPointerParameterAssignments);
    }
    return pPointerAssignments.containsKey(pMemoryLocation)
        || pStartRoutineArgAssignments.containsKey(pMemoryLocation)
        || pPointerParameterAssignments.containsKey(pMemoryLocation);
  }

  // getters =======================================================================================

  // TODO this can be optimized by using an ImmutableSetMultimap and saving it on creation
  static ImmutableSet<MemoryLocation> getPointerAssignmentRightHandSides(
      MemoryLocation pMemoryLocation,
      ImmutableSetMultimap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerParameterAssignments) {

    if (pMemoryLocation.isFieldOwnerPointerType()) {
      return getPointerAssignmentRightHandSides(
          pMemoryLocation.getFieldOwnerMemoryLocation(),
          pPointerAssignments,
          pStartRoutineArgAssignments,
          pPointerParameterAssignments);
    }
    ImmutableSet.Builder<MemoryLocation> rRightHandSides = ImmutableSet.builder();
    rRightHandSides.addAll(pPointerAssignments.get(pMemoryLocation));
    if (pStartRoutineArgAssignments.containsKey(pMemoryLocation)) {
      rRightHandSides.add(Objects.requireNonNull(pStartRoutineArgAssignments.get(pMemoryLocation)));
    }
    if (pPointerParameterAssignments.containsKey(pMemoryLocation)) {
      rRightHandSides.add(
          Objects.requireNonNull(pPointerParameterAssignments.get(pMemoryLocation)));
    }
    return rRightHandSides.build();
  }

  public int getRelevantMemoryLocationAmount() {
    return relevantMemoryLocationAmount;
  }

  public ImmutableList<MemoryLocation> getAllMemoryLocations() {
    return allMemoryLocations;
  }

  /**
   * Returns the set of relevant memory locations, i.e. all that are important to decide whether two
   * statements commute. These are all explicit and implicit global memory locations.
   */
  public ImmutableMap<MemoryLocation, Integer> getRelevantMemoryLocations() {
    return relevantMemoryLocations;
  }
}
