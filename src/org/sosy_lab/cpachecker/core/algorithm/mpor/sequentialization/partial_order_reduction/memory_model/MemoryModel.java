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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.SeqBitVectorUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * A class to keep track of all memory locations in the concurrent input program, including pointers
 * associated with a memory location.
 */
public class MemoryModel {

  private final ImmutableList<SeqMemoryLocation> allMemoryLocations;

  private final int relevantMemoryLocationAmount;

  /**
   * The set of relevant {@link SeqMemoryLocation}s, i.e. all that are needed to decide whether a
   * statement commutes. This includes explicit and implicit (through pointers) memory locations.
   */
  private final ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocations;

  public final ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments;

  /**
   * Keep track of {@code start_routine arg} assignments in {@code pthread_create} separately, since
   * even a local memory address passed here is implicitly global.
   */
  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments;

  /**
   * The map of call context-sensitive {@link SeqMemoryLocation} mapped to their assigned {@link
   * SeqMemoryLocation}. Each parameter is only assigned once due to function cloning. Note that
   * this is not restricted to pointers, since non-pointer parameters can be made implicitly global
   * through global pointers.
   */
  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> parameterAssignments;

  /** The subset of parameters that are pointers. */
  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerParameterAssignments;

  public final ImmutableSet<SeqMemoryLocation> pointerDereferences;

  MemoryModel(
      MPOROptions pOptions,
      ImmutableList<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableMap<SeqMemoryLocation, Integer> pRelevantMemoryLocationIds,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences)
      throws UnsupportedCodeException {

    checkArguments(
        pOptions,
        pRelevantMemoryLocationIds,
        pPointerAssignments,
        pParameterAssignments,
        pPointerParameterAssignments);
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
      MPOROptions pOptions,
      ImmutableMap<SeqMemoryLocation, Integer> pRelevantMemoryLocationIds,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments)
      throws UnsupportedCodeException {

    if (pOptions.bitVectorEncoding().isDense) {
      if (pRelevantMemoryLocationIds.size() > SeqBitVectorUtil.MAX_BINARY_LENGTH) {
        throw new UnsupportedCodeException(
            String.format(
                "The input program contains too many relevant memory locations (> %s). Try setting"
                    + " bitVectorEncoding=SPARSE.",
                SeqBitVectorUtil.MAX_BINARY_LENGTH),
            null);
      }
    }

    // check that all left hand sides in pointer assignments are CPointerType
    for (SeqMemoryLocation memoryLocation : pPointerAssignments.keySet()) {
      if (memoryLocation.fieldMember().isPresent()) {
        // for field owner / members: only the member must be CPointerType
        CCompositeTypeMemberDeclaration memberDeclaration =
            memoryLocation.fieldMember().orElseThrow();
        checkArgument(
            memberDeclaration.getType() instanceof CPointerType,
            "memberDeclaration must be CPointerType, got %s",
            memberDeclaration.getType());
      } else {
        // for all else: the variable itself must be CPointerType
        checkArgument(
            memoryLocation.declaration().getType() instanceof CPointerType,
            "variableDeclaration must be CPointerType, got %s",
            memoryLocation.declaration().getType());
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
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

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

  public boolean isMemoryLocationReachableByThread(
      SeqMemoryLocation pMemoryLocation,
      MPORThread pThread,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      MemoryAccessType pAccessType) {

    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
      ImmutableSet<SeqMemoryLocation> memoryLocations =
          SeqMemoryLocationFinder.findMemoryLocationsBySubstituteEdge(
              substituteEdge, this, pAccessType);
      if (memoryLocations.contains(pMemoryLocation)) {
        return true;
      }
    }
    return false;
  }

  // getters =======================================================================================

  static ImmutableSet<SeqMemoryLocation> getPointerAssignmentRightHandSides(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    if (pMemoryLocation.isFieldOwnerPointerType()) {
      return getPointerAssignmentRightHandSides(
          pMemoryLocation.getFieldOwnerMemoryLocation(),
          pPointerAssignments,
          pStartRoutineArgAssignments,
          pPointerParameterAssignments);
    }
    ImmutableSet.Builder<SeqMemoryLocation> rRightHandSides = ImmutableSet.builder();
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

  public ImmutableList<SeqMemoryLocation> getAllMemoryLocations() {
    return allMemoryLocations;
  }

  /**
   * Returns the set of relevant memory locations, i.e. all that are important to decide whether two
   * statements commute. These are all explicit and implicit global memory locations.
   */
  public ImmutableMap<SeqMemoryLocation, Integer> getRelevantMemoryLocations() {
    return relevantMemoryLocations;
  }
}
